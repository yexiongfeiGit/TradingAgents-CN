#!/usr/bin/env python3
"""
TradingAgents 配置模块初始化文件

提供统一的配置管理接口，包括模型配置、数据库配置、数据源配置等。

主要导出：
- ConfigManager：核心配置管理器，管理API密钥、模型配置等
- ModelConfig：模型配置数据类
- PricingConfig：定价配置数据类  
- UsageRecord：使用记录数据类
- DatabaseManager：智能数据库管理器
- DataSourceConfig：数据源配置管理器
- TushareConfig：Tushare专用配置管理器
- MongoDBStorage：MongoDB存储适配器
- 环境工具函数：parse_bool_env、parse_int_env等

模块结构：
- config_manager.py：核心配置管理
- database_config.py：数据库配置管理
- database_manager.py：数据库连接管理
- env_utils.py：环境变量解析工具
- mongodb_storage.py：MongoDB存储适配器
- providers_config.py：数据源配置管理
- runtime_settings.py：运行时配置适配器
- tushare_config.py：Tushare专用配置

使用示例：
    from tradingagents.config import ConfigManager, DatabaseManager
    
    # 获取配置管理器实例
    config_manager = ConfigManager()
    
    # 获取数据库管理器
    db_manager = DatabaseManager()
    
    # 获取模型配置
    model_config = config_manager.get_model_config("deepseek")

依赖要求：
- python-dotenv：环境变量文件加载（可选）
- pymongo：MongoDB支持（可选）
- redis：Redis支持（可选）

版本兼容性：
- Python 3.8+
- 兼容Python 3.13+的语法特性

作者：TradingAgents-CN团队
版本：2.0.0
创建时间：2024-01-01
"""

from .config_manager import config_manager, token_tracker, ModelConfig, PricingConfig, UsageRecord

__all__ = [
    'config_manager',
    'token_tracker', 
    'ModelConfig',
    'PricingConfig',
    'UsageRecord'
]
