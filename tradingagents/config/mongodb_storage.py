#!/usr/bin/env python3
"""
MongoDB存储适配器模块

用于将token使用记录存储到MongoDB数据库，提供高性能的数据持久化和查询功能。

主要功能：
1. 使用记录存储和管理
2. 数据库连接和连接池管理
3. 索引创建和查询优化
4. 使用统计和数据分析
5. 错误处理和降级机制
6. 时区感知的时间戳处理

配置参数：
- MONGODB_CONNECTION_STRING：MongoDB连接字符串（必需）
- MONGODB_DATABASE：数据库名称（可选，默认tradingagents）
- MONGO_CONNECT_TIMEOUT_MS：连接超时（可选，默认30000ms）
- MONGO_SOCKET_TIMEOUT_MS：套接字超时（可选，默认60000ms）
- MONGO_SERVER_SELECTION_TIMEOUT_MS：服务器选择超时（可选，默认5000ms）

特性：
- 自动索引创建：优化查询性能
- 时区支持：使用系统时区设置
- 批量操作：支持批量插入和查询
- 错误处理：详细的错误日志和降级处理
- 连接池：高效的连接复用
- 数据验证：记录格式验证和转换

索引设计：
- 复合索引：(timestamp, provider, model_name) - 优化时间序列查询
- 会话索引：session_id - 支持会话级别的查询
- 分析类型索引：analysis_type - 支持按分析类型过滤

使用示例：
    # 创建存储实例
    storage = MongoDBStorage("mongodb://localhost:27017/", "tradingagents")
    
    # 保存使用记录
    if storage.is_connected():
        success = storage.save_usage_record(record)
        if success:
            print("记录保存成功")
    
    # 加载使用记录
    records = storage.load_usage_records(limit=1000, days=30)
    
    # 获取使用统计
    stats = storage.get_usage_statistics(days=30)
    print(f"总成本: {stats.get('total_cost', 0)}")

错误处理：
- 连接失败：记录错误日志，返回False
- 插入失败：记录错误详情，返回False
- 查询失败：返回空列表或默认统计
- 索引创建失败：记录警告，继续运行

性能优化：
- 连接复用：避免频繁创建连接
- 索引优化：针对常见查询场景
- 批量操作：减少网络往返
- 超时配置：防止长时间阻塞

作者：TradingAgents-CN团队
版本：1.0.0
创建时间：2024-01-01
"""

import os
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from typing import Dict, List, Optional, Any
from dataclasses import asdict
from .config_manager import UsageRecord

# 导入日志模块
from tradingagents.utils.logging_manager import get_logger
from tradingagents.config.runtime_settings import get_timezone_name
logger = get_logger('agents')

try:
    from pymongo import MongoClient
    from pymongo.errors import ConnectionFailure, ServerSelectionTimeoutError
    MONGODB_AVAILABLE = True
except ImportError:
    MONGODB_AVAILABLE = False
    MongoClient = None


class MongoDBStorage:
    """MongoDB存储适配器"""
    
    def __init__(self, connection_string: str = None, database_name: str = "tradingagents"):
        if not MONGODB_AVAILABLE:
            raise ImportError("pymongo is not installed. Please install it with: pip install pymongo")
        
        # 修复硬编码问题 - 如果没有提供连接字符串且环境变量也未设置，则抛出错误
        self.connection_string = connection_string or os.getenv("MONGODB_CONNECTION_STRING")
        if not self.connection_string:
            raise ValueError(
                "MongoDB连接字符串未配置。请通过以下方式之一进行配置：\n"
                "1. 设置环境变量 MONGODB_CONNECTION_STRING\n"
                "2. 在初始化时传入 connection_string 参数\n"
                "例如: MONGODB_CONNECTION_STRING=mongodb://localhost:27017/"
            )
        
        self.database_name = database_name
        self.collection_name = "token_usage"
        
        self.client = None
        self.db = None
        self.collection = None
        self._connected = False
        
        # 尝试连接
        self._connect()
    
    def _connect(self):
        """连接到MongoDB"""
        try:
            # 从环境变量读取超时配置，使用合理的默认值
            import os
            connect_timeout = int(os.getenv("MONGO_CONNECT_TIMEOUT_MS", "30000"))
            socket_timeout = int(os.getenv("MONGO_SOCKET_TIMEOUT_MS", "60000"))
            server_selection_timeout = int(os.getenv("MONGO_SERVER_SELECTION_TIMEOUT_MS", "5000"))

            self.client = MongoClient(
                self.connection_string,
                serverSelectionTimeoutMS=server_selection_timeout,
                connectTimeoutMS=connect_timeout,
                socketTimeoutMS=socket_timeout
            )
            # 测试连接
            self.client.admin.command('ping')
            
            self.db = self.client[self.database_name]
            self.collection = self.db[self.collection_name]
            
            # 创建索引以提高查询性能
            self._create_indexes()
            
            self._connected = True
            logger.info(f"✅ MongoDB连接成功: {self.database_name}.{self.collection_name}")
            
        except (ConnectionFailure, ServerSelectionTimeoutError) as e:
            logger.error(f"❌ MongoDB连接失败: {e}")
            logger.info(f"将使用本地JSON文件存储")
            self._connected = False
        except Exception as e:
            logger.error(f"❌ MongoDB初始化失败: {e}")
            self._connected = False
    
    def _create_indexes(self):
        """创建数据库索引"""
        try:
            # 创建复合索引
            self.collection.create_index([
                ("timestamp", -1),  # 按时间倒序
                ("provider", 1),
                ("model_name", 1)
            ])
            
            # 创建会话ID索引
            self.collection.create_index("session_id")
            
            # 创建分析类型索引
            self.collection.create_index("analysis_type")
            
        except Exception as e:
            logger.error(f"创建MongoDB索引失败: {e}")
    
    def is_connected(self) -> bool:
        """检查是否连接到MongoDB"""
        return self._connected
    
    def save_usage_record(self, record: UsageRecord) -> bool:
        """保存单个使用记录到MongoDB"""
        if not self._connected:
            return False
        
        try:
            # 转换为字典格式
            record_dict = asdict(record)
            
            # 添加MongoDB特有的字段
            record_dict['_created_at'] = datetime.now(ZoneInfo(get_timezone_name()))
            
            # 插入记录
            result = self.collection.insert_one(record_dict)
            
            if result.inserted_id:
                return True
            else:
                logger.error(f"MongoDB插入失败：未返回插入ID")
                return False
                
        except Exception as e:
            logger.error(f"保存记录到MongoDB失败: {e}")
            return False
    
    def load_usage_records(self, limit: int = 10000, days: int = None) -> List[UsageRecord]:
        """从MongoDB加载使用记录"""
        if not self._connected:
            return []
        
        try:
            # 构建查询条件
            query = {}
            if days:
                from datetime import timedelta
                cutoff_date = datetime.now(ZoneInfo(get_timezone_name())) - timedelta(days=days)
                query['timestamp'] = {'$gte': cutoff_date.isoformat()}
            
            # 查询记录，按时间倒序
            cursor = self.collection.find(query).sort('timestamp', -1).limit(limit)
            
            records = []
            for doc in cursor:
                # 移除MongoDB特有的字段
                doc.pop('_id', None)
                doc.pop('_created_at', None)
                
                # 转换为UsageRecord对象
                try:
                    record = UsageRecord(**doc)
                    records.append(record)
                except Exception as e:
                    logger.error(f"解析记录失败: {e}, 记录: {doc}")
                    continue
            
            return records
            
        except Exception as e:
            logger.error(f"从MongoDB加载记录失败: {e}")
            return []
    
    def get_usage_statistics(self, days: int = 30) -> Dict[str, Any]:
        """从MongoDB获取使用统计"""
        if not self._connected:
            return {}
        
        try:
            from datetime import timedelta
            cutoff_date = datetime.now() - timedelta(days=days)
            
            # 聚合查询
            pipeline = [
                {
                    '$match': {
                        'timestamp': {'$gte': cutoff_date.isoformat()}
                    }
                },
                {
                    '$group': {
                        '_id': None,
                        'total_cost': {'$sum': '$cost'},
                        'total_input_tokens': {'$sum': '$input_tokens'},
                        'total_output_tokens': {'$sum': '$output_tokens'},
                        'total_requests': {'$sum': 1}
                    }
                }
            ]
            
            result = list(self.collection.aggregate(pipeline))
            
            if result:
                stats = result[0]
                return {
                    'period_days': days,
                    'total_cost': round(stats.get('total_cost', 0), 4),
                    'total_input_tokens': stats.get('total_input_tokens', 0),
                    'total_output_tokens': stats.get('total_output_tokens', 0),
                    'total_requests': stats.get('total_requests', 0)
                }
            else:
                return {
                    'period_days': days,
                    'total_cost': 0,
                    'total_input_tokens': 0,
                    'total_output_tokens': 0,
                    'total_requests': 0
                }
                
        except Exception as e:
            logger.error(f"获取MongoDB统计失败: {e}")
            return {}
    
    def get_provider_statistics(self, days: int = 30) -> Dict[str, Dict[str, Any]]:
        """按供应商获取统计信息"""
        if not self._connected:
            return {}
        
        try:
            from datetime import timedelta
            cutoff_date = datetime.now() - timedelta(days=days)
            
            # 按供应商聚合
            pipeline = [
                {
                    '$match': {
                        'timestamp': {'$gte': cutoff_date.isoformat()}
                    }
                },
                {
                    '$group': {
                        '_id': '$provider',
                        'cost': {'$sum': '$cost'},
                        'input_tokens': {'$sum': '$input_tokens'},
                        'output_tokens': {'$sum': '$output_tokens'},
                        'requests': {'$sum': 1}
                    }
                }
            ]
            
            results = list(self.collection.aggregate(pipeline))
            
            provider_stats = {}
            for result in results:
                provider = result['_id']
                provider_stats[provider] = {
                    'cost': round(result.get('cost', 0), 4),
                    'input_tokens': result.get('input_tokens', 0),
                    'output_tokens': result.get('output_tokens', 0),
                    'requests': result.get('requests', 0)
                }
            
            return provider_stats
            
        except Exception as e:
            logger.error(f"获取供应商统计失败: {e}")
            return {}
    
    def cleanup_old_records(self, days: int = 90) -> int:
        """清理旧记录"""
        if not self._connected:
            return 0
        
        try:
            from datetime import timedelta

            cutoff_date = datetime.now() - timedelta(days=days)
            
            result = self.collection.delete_many({
                'timestamp': {'$lt': cutoff_date.isoformat()}
            })
            
            deleted_count = result.deleted_count
            if deleted_count > 0:
                logger.info(f"清理了 {deleted_count} 条超过 {days} 天的记录")
            
            return deleted_count
            
        except Exception as e:
            logger.error(f"清理旧记录失败: {e}")
            return 0
    
    def close(self):
        """关闭MongoDB连接"""
        if self.client:
            self.client.close()
            self._connected = False
            logger.info(f"MongoDB连接已关闭")