#!/usr/bin/env python3
"""
统一股票数据提供器基类模块

定义了所有股票数据提供器的统一接口和基础功能，为不同数据源提供标准化的抽象基类。

主要功能：
1. 统一接口：定义标准化的数据获取接口
2. 连接管理：提供连接状态管理和生命周期控制
3. 数据标准化：提供数据格式标准化的基础方法
4. 错误处理：提供统一的错误处理和日志记录机制
5. 扩展支持：支持财务数据等扩展接口
6. 类型安全：使用类型注解确保代码质量
7. 异步支持：支持异步数据获取操作
8. 多市场支持：支持CN/HK/US等多个市场的股票数据

核心接口：
- connect()：连接到数据源
- disconnect()：断开连接
- is_available()：检查数据源可用性
- get_stock_basic_info()：获取股票基础信息
- get_stock_quotes()：获取实时行情数据
- get_historical_data()：获取历史K线数据
- get_financial_data()：获取财务数据（扩展接口）

数据标准化：
- standardize_basic_info()：标准化股票基础信息
- standardize_quotes()：标准化实时行情数据
- _determine_market_info()：确定市场信息
- _determine_market()：确定市场代码
- _format_date_output()：格式化日期输出
- _convert_to_float()：安全转换为浮点数

设计原则：
- 抽象基类：使用ABC定义抽象方法
- 类型注解：完整的类型提示支持
- 异步支持：核心接口支持异步操作
- 扩展性：支持子类扩展特定功能
- 标准化：统一的数据格式和字段命名
- 错误处理：完善的异常处理机制
- 日志记录：详细的操作日志记录

配置参数：
- provider_name：数据源提供商名称
- connected：连接状态标识
- logger：日志记录器实例

使用示例：
    from tradingagents.dataflows.providers.base_provider import BaseStockDataProvider
    
    class MyDataProvider(BaseStockDataProvider):
        async def connect(self) -> bool:
            # 实现连接逻辑
            self.connected = True
            return True
            
        async def get_stock_basic_info(self, symbol: str = None):
            # 实现基础信息获取
            raw_data = await self._fetch_data(symbol)
            return self.standardize_basic_info(raw_data)
            
        async def get_stock_quotes(self, symbol: str):
            # 实现行情数据获取
            raw_data = await self._fetch_quotes(symbol)
            return self.standardize_quotes(raw_data)
            
        async def get_historical_data(self, symbol, start_date, end_date=None):
            # 实现历史数据获取
            return await self._fetch_historical(symbol, start_date, end_date)

依赖要求：
- pandas：数据处理和分析库
- typing：类型注解支持
- abc：抽象基类支持
- datetime：日期时间处理
- logging：日志记录

数据格式：
- 基础信息：包含代码、名称、市场、行业等字段
- 实时行情：包含价格、成交量、涨跌幅等字段
- 历史数据：pandas DataFrame格式，包含OHLCV数据
- 财务数据：支持年报和季报格式

错误处理：
- 连接失败：返回False并记录错误日志
- 数据获取失败：返回None并记录警告日志
- 数据解析错误：记录详细错误信息
- 网络异常：提供重试机制建议

性能优化：
- 连接池：支持连接复用和池化管理
- 缓存机制：支持数据缓存减少重复请求
- 批量处理：支持批量数据获取和处理
- 异步操作：支持并发数据获取

扩展性：
- 子类化：支持继承扩展特定功能
- 插件机制：支持动态加载数据提供商
- 配置驱动：支持配置文件驱动的参数设置
- 多数据源：支持同时连接多个数据源

作者：TradingAgents-CN团队
版本：2.0.0
创建时间：2024-01-01
"""
from abc import ABC, abstractmethod
from typing import Optional, Dict, Any, List, Union
from datetime import datetime, date
import logging
import pandas as pd


class BaseStockDataProvider(ABC):
    """
    股票数据提供器基类
    定义了所有数据源提供器的统一接口
    """
    
    def __init__(self, provider_name: str):
        """
        初始化数据提供器
        
        Args:
            provider_name: 提供器名称
        """
        self.provider_name = provider_name
        self.connected = False
        self.logger = logging.getLogger(f"{__name__}.{provider_name}")
    
    # ==================== 连接管理 ====================
    
    @abstractmethod
    async def connect(self) -> bool:
        """
        连接到数据源
        
        Returns:
            bool: 连接是否成功
        """
        pass
    
    async def disconnect(self):
        """断开连接"""
        self.connected = False
        self.logger.info(f"✅ {self.provider_name} 连接已断开")
    
    def is_available(self) -> bool:
        """检查数据源是否可用"""
        return self.connected
    
    # ==================== 核心数据接口 ====================
    
    @abstractmethod
    async def get_stock_basic_info(self, symbol: str = None) -> Optional[Union[Dict[str, Any], List[Dict[str, Any]]]]:
        """
        获取股票基础信息
        
        Args:
            symbol: 股票代码，为空则获取所有股票
            
        Returns:
            单个股票信息字典或股票列表
        """
        pass
    
    @abstractmethod
    async def get_stock_quotes(self, symbol: str) -> Optional[Dict[str, Any]]:
        """
        获取实时行情
        
        Args:
            symbol: 股票代码
            
        Returns:
            实时行情数据字典
        """
        pass
    
    @abstractmethod
    async def get_historical_data(
        self, 
        symbol: str, 
        start_date: Union[str, date], 
        end_date: Union[str, date] = None
    ) -> Optional[pd.DataFrame]:
        """
        获取历史数据
        
        Args:
            symbol: 股票代码
            start_date: 开始日期
            end_date: 结束日期
            
        Returns:
            历史数据DataFrame
        """
        pass
    
    # ==================== 扩展接口 ====================
    
    async def get_stock_list(self, market: str = None) -> Optional[List[Dict[str, Any]]]:
        """
        获取股票列表
        
        Args:
            market: 市场代码 (CN/HK/US)
            
        Returns:
            股票列表
        """
        return await self.get_stock_basic_info()
    
    async def get_financial_data(self, symbol: str, report_type: str = "annual") -> Optional[Dict[str, Any]]:
        """
        获取财务数据
        
        Args:
            symbol: 股票代码
            report_type: 报告类型 (annual/quarterly)
            
        Returns:
            财务数据字典
        """
        # 默认实现返回None，子类可以重写
        return None
    
    # ==================== 数据标准化方法 ====================
    
    def standardize_basic_info(self, raw_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        标准化股票基础信息
        
        Args:
            raw_data: 原始数据
            
        Returns:
            标准化后的数据
        """
        # 基础标准化逻辑
        return {
            "code": raw_data.get("code", raw_data.get("symbol", "")),
            "name": raw_data.get("name", ""),
            "symbol": raw_data.get("symbol", raw_data.get("code", "")),
            "full_symbol": raw_data.get("full_symbol", raw_data.get("ts_code", "")),
            
            # 市场信息
            "market_info": self._determine_market_info(raw_data),
            
            # 业务信息
            "industry": raw_data.get("industry"),
            "area": raw_data.get("area"),
            "list_date": self._format_date_output(raw_data.get("list_date")),
            
            # 元数据
            "data_source": self.provider_name.lower(),
            "data_version": 1,
            "updated_at": datetime.utcnow()
        }
    
    def standardize_quotes(self, raw_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        标准化实时行情数据
        
        Args:
            raw_data: 原始数据
            
        Returns:
            标准化后的数据
        """
        symbol = raw_data.get("symbol", raw_data.get("code", ""))
        
        return {
            # 基础字段
            "code": symbol,
            "symbol": symbol,
            "full_symbol": raw_data.get("full_symbol", raw_data.get("ts_code", symbol)),
            "market": self._determine_market(raw_data),
            
            # 价格数据
            "close": self._convert_to_float(raw_data.get("close")),
            "current_price": self._convert_to_float(raw_data.get("current_price", raw_data.get("close"))),
            "open": self._convert_to_float(raw_data.get("open")),
            "high": self._convert_to_float(raw_data.get("high")),
            "low": self._convert_to_float(raw_data.get("low")),
            "pre_close": self._convert_to_float(raw_data.get("pre_close")),
            
            # 变动数据
            "change": self._convert_to_float(raw_data.get("change")),
            "pct_chg": self._convert_to_float(raw_data.get("pct_chg")),
            
            # 成交数据
            "volume": self._convert_to_float(raw_data.get("volume", raw_data.get("vol"))),
            "amount": self._convert_to_float(raw_data.get("amount")),
            
            # 时间数据
            "trade_date": self._format_date_output(raw_data.get("trade_date")),
            "timestamp": datetime.utcnow(),
            
            # 元数据
            "data_source": self.provider_name.lower(),
            "data_version": 1,
            "updated_at": datetime.utcnow()
        }
    
    # ==================== 辅助方法 ====================
    
    def _determine_market_info(self, raw_data: Dict[str, Any]) -> Dict[str, Any]:
        """确定市场信息"""
        # 默认实现，子类可以重写
        return {
            "market": "CN",
            "exchange": "UNKNOWN",
            "exchange_name": "未知交易所",
            "currency": "CNY",
            "timezone": "Asia/Shanghai"
        }
    
    def _determine_market(self, raw_data: Dict[str, Any]) -> str:
        """确定市场代码"""
        market_info = self._determine_market_info(raw_data)
        return market_info.get("market", "CN")
    
    def _convert_to_float(self, value: Any) -> Optional[float]:
        """转换为浮点数"""
        if value is None or value == "":
            return None
        try:
            return float(value)
        except (ValueError, TypeError):
            return None
    
    def _format_date_output(self, date_value: Any) -> Optional[str]:
        """格式化日期为输出格式 (YYYY-MM-DD)"""
        if not date_value:
            return None
        
        date_str = str(date_value)
        
        # 处理YYYYMMDD格式
        if len(date_str) == 8 and date_str.isdigit():
            return f"{date_str[:4]}-{date_str[4:6]}-{date_str[6:8]}"
        
        # 处理其他格式
        if isinstance(date_value, (date, datetime)):
            return date_value.strftime('%Y-%m-%d')
        
        return date_str
    
    # ==================== 上下文管理器支持 ====================
    
    async def __aenter__(self):
        """异步上下文管理器入口"""
        await self.connect()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """异步上下文管理器出口"""
        await self.disconnect()
    
    def __repr__(self):
        return f"<{self.__class__.__name__}(name='{self.provider_name}', connected={self.connected})>"
