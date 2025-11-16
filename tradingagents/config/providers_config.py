"""
数据源提供器配置管理模块

从 tradingagents/dataflows/providers_config.py 迁移而来，统一管理所有数据源提供器的配置。

主要功能：
1. 多数据源支持（Tushare、AKShare、BaoStock、Yahoo Finance、Finnhub）
2. 环境变量驱动的配置管理
3. 数据源启用/禁用控制
4. 缓存配置和限流设置
5. 全局配置实例管理

支持的数据源：
- Tushare：国内股票数据，需要API Token
- AKShare：开源财经数据接口
- BaoStock：免费股票数据
- Yahoo Finance：国际股票数据（默认禁用）
- Finnhub：专业金融数据，需要API Key（默认禁用）

配置参数：
- enabled：数据源启用状态
- token/api_key：API密钥（如需要）
- timeout：请求超时时间（秒）
- rate_limit：限流间隔（秒）
- max_retries：最大重试次数
- cache_enabled：缓存启用状态
- cache_ttl：缓存过期时间（秒）

特性：
- 懒加载：配置在首次访问时加载
- 单例模式：全局共享配置实例
- 错误处理：默认值和异常处理
- 日志记录：详细的配置加载日志
- 向后兼容：支持旧版本配置格式

使用示例：
    # 获取全局配置实例
    config = get_data_source_config()
    
    # 获取特定数据源配置
    tushare_config = config.get_provider_config("tushare")
    
    # 检查数据源是否启用
    if config.is_provider_enabled("tushare"):
        # 使用Tushare数据源
        pass
    
    # 获取所有启用的数据源
    enabled_providers = config.get_all_enabled_providers()

环境变量配置：
- TUSHARE_ENABLED、TUSHARE_TOKEN、TUSHARE_TIMEOUT等
- AKSHARE_ENABLED、AKSHARE_TIMEOUT、AKSHARE_RATE_LIMIT等
- BAOSTOCK_ENABLED、BAOSTOCK_TIMEOUT、BAOSTOCK_MAX_RETRIES等

作者：TradingAgents-CN团队
版本：1.0.0
创建时间：2024-01-01
"""
import os
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)


class DataSourceConfig:
    """数据源配置管理器"""
    
    def __init__(self):
        self._configs = {}
        self._load_configs()
    
    def _load_configs(self):
        """加载所有数据源配置"""
        # Tushare配置
        self._configs["tushare"] = {
            "enabled": self._get_bool_env("TUSHARE_ENABLED", True),
            "token": os.getenv("TUSHARE_TOKEN", ""),
            "timeout": self._get_int_env("TUSHARE_TIMEOUT", 30),
            "rate_limit": self._get_float_env("TUSHARE_RATE_LIMIT", 0.1),
            "max_retries": self._get_int_env("TUSHARE_MAX_RETRIES", 3),
            "cache_enabled": self._get_bool_env("TUSHARE_CACHE_ENABLED", True),
            "cache_ttl": self._get_int_env("TUSHARE_CACHE_TTL", 3600),
        }
        
        # AKShare配置
        self._configs["akshare"] = {
            "enabled": self._get_bool_env("AKSHARE_ENABLED", True),
            "timeout": self._get_int_env("AKSHARE_TIMEOUT", 30),
            "rate_limit": self._get_float_env("AKSHARE_RATE_LIMIT", 0.2),
            "max_retries": self._get_int_env("AKSHARE_MAX_RETRIES", 3),
            "cache_enabled": self._get_bool_env("AKSHARE_CACHE_ENABLED", True),
            "cache_ttl": self._get_int_env("AKSHARE_CACHE_TTL", 1800),
        }
        
        # BaoStock配置
        self._configs["baostock"] = {
            "enabled": self._get_bool_env("BAOSTOCK_ENABLED", True),
            "timeout": self._get_int_env("BAOSTOCK_TIMEOUT", 30),
            "rate_limit": self._get_float_env("BAOSTOCK_RATE_LIMIT", 0.1),
            "max_retries": self._get_int_env("BAOSTOCK_MAX_RETRIES", 3),
            "cache_enabled": self._get_bool_env("BAOSTOCK_CACHE_ENABLED", True),
            "cache_ttl": self._get_int_env("BAOSTOCK_CACHE_TTL", 1800),
        }
        
        # Yahoo Finance配置
        self._configs["yahoo"] = {
            "enabled": self._get_bool_env("YAHOO_ENABLED", False),
            "timeout": self._get_int_env("YAHOO_TIMEOUT", 30),
            "rate_limit": self._get_float_env("YAHOO_RATE_LIMIT", 0.5),
            "max_retries": self._get_int_env("YAHOO_MAX_RETRIES", 3),
            "cache_enabled": self._get_bool_env("YAHOO_CACHE_ENABLED", True),
            "cache_ttl": self._get_int_env("YAHOO_CACHE_TTL", 300),
        }
        
        # Finnhub配置
        self._configs["finnhub"] = {
            "enabled": self._get_bool_env("FINNHUB_ENABLED", False),
            "api_key": os.getenv("FINNHUB_API_KEY", ""),
            "timeout": self._get_int_env("FINNHUB_TIMEOUT", 30),
            "rate_limit": self._get_float_env("FINNHUB_RATE_LIMIT", 1.0),
            "max_retries": self._get_int_env("FINNHUB_MAX_RETRIES", 3),
            "cache_enabled": self._get_bool_env("FINNHUB_CACHE_ENABLED", True),
            "cache_ttl": self._get_int_env("FINNHUB_CACHE_TTL", 300),
        }
        
        # 通达信配置 - 已移除
        # TDX 数据源已不再支持
        # self._configs["tdx"] = {
        #     "enabled": False,
        # }

        logger.debug("✅ 数据源配置加载完成")
    
    def get_provider_config(self, provider_name: str) -> Dict[str, Any]:
        """
        获取指定提供器的配置
        
        Args:
            provider_name: 提供器名称
            
        Returns:
            配置字典
        """
        config = self._configs.get(provider_name.lower(), {})
        if not config:
            logger.warning(f"⚠️ 未找到 {provider_name} 的配置")
        return config
    
    def is_provider_enabled(self, provider_name: str) -> bool:
        """检查提供器是否启用"""
        config = self.get_provider_config(provider_name)
        return config.get("enabled", False)
    
    def get_all_enabled_providers(self) -> list:
        """获取所有启用的提供器名称"""
        enabled = []
        for name, config in self._configs.items():
            if config.get("enabled", False):
                enabled.append(name)
        return enabled
    
    def _get_bool_env(self, key: str, default: bool) -> bool:
        """获取布尔型环境变量"""
        value = os.getenv(key, str(default)).lower()
        return value in ("true", "1", "yes", "on")
    
    def _get_int_env(self, key: str, default: int) -> int:
        """获取整型环境变量"""
        try:
            return int(os.getenv(key, str(default)))
        except ValueError:
            return default
    
    def _get_float_env(self, key: str, default: float) -> float:
        """获取浮点型环境变量"""
        try:
            return float(os.getenv(key, str(default)))
        except ValueError:
            return default


# 全局配置实例
_config_instance = None

def get_data_source_config() -> DataSourceConfig:
    """获取全局数据源配置实例"""
    global _config_instance
    if _config_instance is None:
        _config_instance = DataSourceConfig()
    return _config_instance

def get_provider_config(provider_name: str) -> Dict[str, Any]:
    """获取指定提供器配置的便捷函数"""
    config = get_data_source_config()
    return config.get_provider_config(provider_name)

