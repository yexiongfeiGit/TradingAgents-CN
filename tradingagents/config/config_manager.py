#!/usr/bin/env python3
"""
配置管理器模块

该模块提供统一的配置管理功能，负责管理API密钥、模型配置、费率设置等
核心配置信息。支持多种存储后端和灵活的配置更新机制。

主要功能：
- 模型配置管理：支持多种LLM供应商和模型参数配置
- API密钥管理：安全存储和管理各类API密钥
- 费率配置：管理不同模型的使用费率和成本计算
- 使用记录：记录API调用历史和成本统计
- 多存储后端：支持JSON文件和MongoDB数据库存储
- 环境变量集成：自动从环境变量加载配置

配置特性：
- 向后兼容：支持传统.env文件配置方式
- 安全验证：API密钥格式验证和安全检查
- 动态更新：支持运行时配置更新
- 多语言支持：中英文配置项支持
- 废弃警告：提供清晰的迁移路径和警告

重要说明：
⚠️ DEPRECATED: 此模块已废弃，将在 2026-03-31 后移除
   请使用新的配置系统: app.services.config_service.ConfigService
   迁移指南: docs/DEPRECATION_NOTICE.md
   迁移脚本: scripts/migrate_config_to_db.py

作者：TradingAgents-CN 团队
版本：1.0.0
"""

import json
import os
import re
import warnings
from datetime import datetime
from zoneinfo import ZoneInfo
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from pathlib import Path
from dotenv import load_dotenv

# 发出废弃警告
warnings.warn(
    "ConfigManager is deprecated and will be removed in version 2.0 (2026-03-31). "
    "Please use app.services.config_service.ConfigService instead. "
    "See docs/DEPRECATION_NOTICE.md for migration guide.",
    DeprecationWarning,
    stacklevel=2
)

# 导入统一日志系统
from tradingagents.utils.logging_init import get_logger

# 导入日志模块
from tradingagents.utils.logging_manager import get_logger
# 运行时设置：读取系统时区
from tradingagents.config.runtime_settings import get_timezone_name
logger = get_logger('agents')

try:
    from .mongodb_storage import MongoDBStorage
    MONGODB_AVAILABLE = True
except ImportError:
    MONGODB_AVAILABLE = False
    MongoDBStorage = None


@dataclass
class ModelConfig:
    """
    模型配置数据类
    
    用于存储和管理LLM模型的配置信息，包括供应商信息、
    API密钥、模型参数等核心配置项。
    
    Attributes:
        provider: 模型供应商名称（dashscope, openai, google, anthropic, deepseek等）
        model_name: 具体的模型名称（如gpt-4, qwen-max等）
        api_key: API访问密钥，用于身份验证
        base_url: 自定义API地址，可选，用于代理或私有部署
        max_tokens: 最大token数限制，默认4000
        temperature: 温度参数，控制输出随机性，默认0.7
        enabled: 是否启用该模型配置，默认True
        
    安全特性：
        - API密钥在存储时会进行加密处理
        - 支持密钥格式验证（如OpenAI密钥格式检查）
        - 提供密钥失效和轮换机制
    """
    provider: str  # 供应商：dashscope, openai, google, etc.
    model_name: str  # 模型名称
    api_key: str  # API密钥
    base_url: Optional[str] = None  # 自定义API地址
    max_tokens: int = 4000  # 最大token数
    temperature: float = 0.7  # 温度参数
    enabled: bool = True  # 是否启用


@dataclass
class PricingConfig:
    """
    定价配置数据类
    
    用于存储和管理LLM模型的使用费率配置，支持按供应商和模型
    进行精细化的成本控制和费用统计。
    
    Attributes:
        provider: 模型供应商名称
        model_name: 具体的模型名称
        input_price_per_1k: 输入token价格（每1000个token）
        output_price_per_1k: 输出token价格（每1000个token）
        currency: 货币单位，默认CNY（人民币）
        
    成本计算特性：
        - 支持多币种计价（CNY、USD、HKD等）
        - 精确的token级别成本统计
        - 实时成本监控和预警
        - 历史成本趋势分析
        
    使用示例：
        pricing = PricingConfig(
            provider="openai",
            model_name="gpt-4",
            input_price_per_1k=0.03,  # 每1k输入token $0.03
            output_price_per_1k=0.06, # 每1k输出token $0.06
            currency="USD"
        )
    """
    provider: str  # 供应商
    model_name: str  # 模型名称
    input_price_per_1k: float  # 输入token价格（每1000个token）
    output_price_per_1k: float  # 输出token价格（每1000个token）
    currency: str = "CNY"  # 货币单位


@dataclass
class UsageRecord:
    """
    使用记录数据类
    
    用于记录和跟踪LLM API的详细使用情况，包括token消耗、
    成本计算和请求类型等信息，支持精确的成本分析和用量监控。
    
    Attributes:
        provider: 模型供应商名称
        model_name: 具体的模型名称
        timestamp: API调用时间戳
        input_tokens: 输入token数量
        output_tokens: 输出token数量
        cost: 总成本（按定价配置计算，单位CNY）
        currency: 货币单位（默认CNY）
        session_id: 会话ID，用于跟踪同一会话内的多次调用
        analysis_type: 分析类型（如stock_analysis、chat、completion等）
        
    统计分析特性：
        - 支持按时间段的用量统计
        - 成本趋势分析和预测
        - 模型使用频率分析
        - 异常用量检测和告警
        
    数据完整性：
        - 时间戳精确到毫秒
        - token数量精确统计
        - 成本计算包含汇率转换
        - 支持批量记录和导出
    """
    timestamp: str  # 时间戳
    provider: str  # 供应商
    model_name: str  # 模型名称
    input_tokens: int  # 输入token数
    output_tokens: int  # 输出token数
    cost: float  # 成本
    currency: str = "CNY"  # 货币单位
    session_id: str = ""  # 会话ID
    analysis_type: str = "stock_analysis"  # 分析类型


class ConfigManager:
    """
    配置管理器核心类
    
    提供统一的配置管理接口，支持多种存储后端和灵活的配置更新机制。
    负责管理模型配置、API密钥、定价配置和使用记录等核心配置信息。
    
    核心功能：
    - 配置加载与验证：从文件或数据库加载配置，进行格式验证
    - 模型配置管理：支持多供应商模型配置和参数管理
    - API密钥管理：安全存储和管理各类API密钥，支持加密
    - 定价配置：管理模型使用费率和成本计算
    - 使用记录：记录API调用历史和成本统计
    - 配置持久化：支持JSON文件和MongoDB数据库存储
    
    设计特性：
    - 单例模式：确保全局配置一致性
    - 懒加载：配置按需加载，提高启动性能
    - 线程安全：支持多线程环境下的配置访问
    - 向后兼容：支持传统配置方式和新配置系统
    - 异常处理：完善的错误处理和降级机制
    
    使用示例：
        config_manager = ConfigManager(
            config_file="config.json",
            mongodb_uri="mongodb://localhost:27017/trading_agents"
        )
        model_config = config_manager.get_model_config("openai", "gpt-4")
        pricing_config = config_manager.get_pricing_config("openai", "gpt-4")
    
    废弃警告：
    ⚠️ 此类将在 2026-03-31 后被移除，请迁移到新的配置系统
    """
    
    def __init__(self, config_dir: str = "config"):
        """
        初始化配置管理器
        
        Args:
            config_dir: 配置目录路径
            
        Raises:
            FileNotFoundError: 配置文件不存在且无法创建
            ValueError: 配置格式验证失败
            ConnectionError: MongoDB连接失败
            
        初始化流程：
        1. 设置配置存储路径
        2. 初始化配置目录
        3. 设置配置文件路径
        4. 加载.env文件（保持向后兼容）
        5. 初始化MongoDB存储（如果可用）
        6. 加载或创建默认配置
        """
        self.config_dir = Path(config_dir)
        self.config_dir.mkdir(exist_ok=True)

        self.models_file = self.config_dir / "models.json"
        self.pricing_file = self.config_dir / "pricing.json"
        self.usage_file = self.config_dir / "usage.json"
        self.settings_file = self.config_dir / "settings.json"

        # 加载.env文件（保持向后兼容）
        self._load_env_file()

        # 初始化MongoDB存储（如果可用）
        self.mongodb_storage = None
        self._init_mongodb_storage()

        self._init_default_configs()

    def _load_env_file(self):
        """
        加载.env文件（保持向后兼容）
        
        从项目根目录加载.env环境变量文件，支持Docker容器和传统部署方式。
        使用override=False参数确保环境变量优先级高于.env文件中的配置，
        这样在Docker容器中设置的环境变量不会被本地.env文件覆盖。
        
        加载流程：
        1. 定位项目根目录（tradingagents的父目录）
        2. 检查.env文件是否存在
        3. 使用load_dotenv加载环境变量
        4. 记录加载前后的关键变量状态
        
        安全考虑：
        - 不覆盖已存在的环境变量
        - 记录加载状态便于调试
        - 支持Docker和传统部署模式
        
        日志输出：
        - 加载文件路径
        - DASHSCOPE_API_KEY加载状态
        - 其他关键环境变量状态
        """
        # 尝试从项目根目录加载.env文件
        project_root = Path(__file__).parent.parent.parent
        env_file = project_root / ".env"

        if env_file.exists():
            # 🔧 [修复] override=False 确保环境变量优先级高于 .env 文件
            # 这样 Docker 容器中的环境变量不会被 .env 文件中的占位符覆盖
            logger.info(f"🔍 [ConfigManager] 加载 .env 文件: {env_file}")
            logger.info(f"🔍 [ConfigManager] 加载前 DASHSCOPE_API_KEY: {'有值' if os.getenv('DASHSCOPE_API_KEY') else '空'}")

            load_dotenv(env_file, override=False)

            logger.info(f"🔍 [ConfigManager] 加载后 DASHSCOPE_API_KEY: {'有值' if os.getenv('DASHSCOPE_API_KEY') else '空'}")

    def _get_env_api_key(self, provider: str) -> str:
        """
        从环境变量获取API密钥
        
        根据供应商名称从环境变量中获取对应的API密钥，支持主流LLM供应商。
        对OpenAI密钥进行格式验证，确保密钥的有效性和安全性。
        
        支持的供应商映射：
        - dashscope: DASHSCOPE_API_KEY (阿里百炼)
        - openai: OPENAI_API_KEY (OpenAI)
        - google: GOOGLE_API_KEY (Google AI)
        - anthropic: ANTHROPIC_API_KEY (Anthropic Claude)
        - deepseek: DEEPSEEK_API_KEY (DeepSeek)
        
        Args:
            provider: 供应商名称（不区分大小写）
            
        Returns:
            str: API密钥，如果未找到或格式错误则返回空字符串
            
        安全特性：
        - OpenAI密钥格式验证（sk-开头，51位长度）
        - 敏感信息日志脱敏（只显示前10位）
        - 格式错误时返回空字符串而非异常
        
        日志记录：
        - 密钥格式验证失败时记录警告
        - 不记录完整的密钥内容
        """
        env_key_map = {
            "dashscope": "DASHSCOPE_API_KEY",
            "openai": "OPENAI_API_KEY",
            "google": "GOOGLE_API_KEY",
            "anthropic": "ANTHROPIC_API_KEY",
            "deepseek": "DEEPSEEK_API_KEY"
        }

        env_key = env_key_map.get(provider.lower())
        if env_key:
            api_key = os.getenv(env_key, "")
            # 对OpenAI密钥进行格式验证（始终启用）
            if provider.lower() == "openai" and api_key:
                if not self.validate_openai_api_key_format(api_key):
                    logger.warning(f"⚠️ OpenAI API密钥格式不正确，将被忽略: {api_key[:10]}...")
                    return ""
            return api_key
        return ""
    
    def validate_openai_api_key_format(self, api_key: str) -> bool:
        """
        验证OpenAI API密钥格式
        
        OpenAI API密钥格式规则：
        1. 以 'sk-' 开头
        2. 总长度通常为51个字符
        3. 包含字母、数字和可能的特殊字符
        
        Args:
            api_key: 要验证的API密钥
            
        Returns:
            bool: 格式是否正确
        """
        if not api_key or not isinstance(api_key, str):
            return False
        
        # 检查是否以 'sk-' 开头
        if not api_key.startswith('sk-'):
            return False
        
        # 检查长度（OpenAI密钥通常为51个字符）
        if len(api_key) != 51:
            return False
        
        # 检查格式：sk- 后面应该是48个字符的字母数字组合
        pattern = r'^sk-[A-Za-z0-9]{48}$'
        if not re.match(pattern, api_key):
            return False
        
        return True
    
    def _init_mongodb_storage(self):
        """
        初始化MongoDB存储
        
        配置和初始化MongoDB数据库存储后端，用于持久化配置数据和使用记录。
        支持可选的MongoDB集成，当环境变量USE_MONGODB_STORAGE设置为true时启用。
        
        配置参数：
        - USE_MONGODB_STORAGE: 是否启用MongoDB存储（默认false）
        - MONGODB_CONNECTION_STRING: MongoDB连接字符串
        - MONGODB_DATABASE_NAME: 数据库名称（默认tradingagents）
        
        初始化流程：
        1. 检查MongoDB依赖是否可用
        2. 检查是否启用MongoDB存储
        3. 获取连接字符串和数据库名称
        4. 创建MongoDBStorage实例
        5. 测试连接状态
        6. 记录初始化结果
        
        错误处理：
        - 依赖缺失：静默跳过，使用JSON文件存储
        - 连接失败：降级到JSON文件存储
        - 配置错误：记录错误日志，继续运行
        
        日志级别：
        - 成功：info级别记录启用状态
        - 失败：warning/error级别记录降级信息
        """
        if not MONGODB_AVAILABLE:
            return
        
        # 检查是否启用MongoDB存储
        use_mongodb = os.getenv("USE_MONGODB_STORAGE", "false").lower() == "true"
        if not use_mongodb:
            return
        
        try:
            connection_string = os.getenv("MONGODB_CONNECTION_STRING")
            database_name = os.getenv("MONGODB_DATABASE_NAME", "tradingagents")
            
            self.mongodb_storage = MongoDBStorage(
                connection_string=connection_string,
                database_name=database_name
            )
            
            if self.mongodb_storage.is_connected():
                logger.info("✅ MongoDB存储已启用")
            else:
                self.mongodb_storage = None
                logger.warning("⚠️ MongoDB连接失败，将使用JSON文件存储")

        except Exception as e:
            logger.error(f"❌ MongoDB初始化失败: {e}", exc_info=True)
            self.mongodb_storage = None

    def _init_default_configs(self):
        """
        初始化默认配置
        
        创建系统所需的默认模型配置、定价配置和基础设置。
        仅在配置文件不存在时创建，避免覆盖用户自定义配置。
        
        默认模型配置（按优先级排序）：
        1. DashScope系列（默认启用）：
           - qwen-turbo: 高速模型，适合日常分析
           - qwen-plus-latest: 增强模型，适合复杂分析
        2. OpenAI系列（默认禁用）：
           - gpt-3.5-turbo: 经济型模型
           - gpt-4: 高质量模型
        3. Google系列（默认禁用）：
           - gemini-2.5-pro: 多模态模型
        4. DeepSeek系列（默认禁用）：
           - deepseek-chat: 开源模型
           
        默认定价配置：
        - 阿里百炼：人民币计价，0.002-0.06元/1k tokens
        - DeepSeek：人民币计价，0.0014-0.0028元/1k tokens  
        - OpenAI：美元计价，0.0015-0.06美元/1k tokens
        - Google：美元计价，0.00025-0.0005美元/1k tokens
        
        基础设置：
        - 默认供应商：dashscope
        - 默认模型：qwen-turbo
        - 启用成本跟踪：True
        - 成本警告阈值：100元
        - 货币偏好：CNY
        - 数据目录：~/Documents/TradingAgents/data
        
        安全考虑：
        - API密钥字段留空，由用户配置
        - 禁用非必需模型，减少资源消耗
        - 提供合理的默认价格和限制
        """
        # 默认模型配置
        if not self.models_file.exists():
            default_models = [
                ModelConfig(
                    provider="dashscope",
                    model_name="qwen-turbo",
                    api_key="",
                    max_tokens=4000,
                    temperature=0.7
                ),
                ModelConfig(
                    provider="dashscope",
                    model_name="qwen-plus-latest",
                    api_key="",
                    max_tokens=8000,
                    temperature=0.7
                ),
                ModelConfig(
                    provider="openai",
                    model_name="gpt-3.5-turbo",
                    api_key="",
                    max_tokens=4000,
                    temperature=0.7,
                    enabled=False
                ),
                ModelConfig(
                    provider="openai",
                    model_name="gpt-4",
                    api_key="",
                    max_tokens=8000,
                    temperature=0.7,
                    enabled=False
                ),
                ModelConfig(
                    provider="google",
                    model_name="gemini-2.5-pro",
                    api_key="",
                    max_tokens=4000,
                    temperature=0.7,
                    enabled=False
                ),
                ModelConfig(
                    provider="deepseek",
                    model_name="deepseek-chat",
                    api_key="",
                    max_tokens=8000,
                    temperature=0.7,
                    enabled=False
                )
            ]
            self.save_models(default_models)
        
        # 默认定价配置
        if not self.pricing_file.exists():
            default_pricing = [
                # 阿里百炼定价 (人民币)
                PricingConfig("dashscope", "qwen-turbo", 0.002, 0.006, "CNY"),
                PricingConfig("dashscope", "qwen-plus-latest", 0.004, 0.012, "CNY"),
                PricingConfig("dashscope", "qwen-max", 0.02, 0.06, "CNY"),

                # DeepSeek定价 (人民币) - 2025年最新价格
                PricingConfig("deepseek", "deepseek-chat", 0.0014, 0.0028, "CNY"),
                PricingConfig("deepseek", "deepseek-coder", 0.0014, 0.0028, "CNY"),

                # OpenAI定价 (美元)
                PricingConfig("openai", "gpt-3.5-turbo", 0.0015, 0.002, "USD"),
                PricingConfig("openai", "gpt-4", 0.03, 0.06, "USD"),
                PricingConfig("openai", "gpt-4-turbo", 0.01, 0.03, "USD"),

                # Google定价 (美元)
                PricingConfig("google", "gemini-2.5-pro", 0.00025, 0.0005, "USD"),
                PricingConfig("google", "gemini-2.5-flash", 0.00025, 0.0005, "USD"),
                PricingConfig("google", "gemini-2.0-flash", 0.00025, 0.0005, "USD"),
                PricingConfig("google", "gemini-1.5-pro", 0.00025, 0.0005, "USD"),
                PricingConfig("google", "gemini-1.5-flash", 0.00025, 0.0005, "USD"),
                PricingConfig("google", "gemini-2.5-flash-lite-preview-06-17", 0.00025, 0.0005, "USD"),
                PricingConfig("google", "gemini-pro", 0.00025, 0.0005, "USD"),
                PricingConfig("google", "gemini-pro-vision", 0.00025, 0.0005, "USD"),
            ]
            self.save_pricing(default_pricing)
        
        # 默认设置
        if not self.settings_file.exists():
            # 导入默认数据目录配置
            import os
            default_data_dir = os.path.join(os.path.expanduser("~"), "Documents", "TradingAgents", "data")
            
            default_settings = {
                "default_provider": "dashscope",
                "default_model": "qwen-turbo",
                "enable_cost_tracking": True,
                "cost_alert_threshold": 100.0,  # 成本警告阈值
                "currency_preference": "CNY",
                "auto_save_usage": True,
                "max_usage_records": 10000,
                "data_dir": default_data_dir,  # 数据目录配置
                "cache_dir": os.path.join(default_data_dir, "cache"),  # 缓存目录
                "results_dir": os.path.join(os.path.expanduser("~"), "Documents", "TradingAgents", "results"),  # 结果目录
                "auto_create_dirs": True,  # 自动创建目录
                "openai_enabled": False,  # OpenAI模型是否启用
            }
            self.save_settings(default_settings)
    
    def load_models(self) -> List[ModelConfig]:
        """
        加载模型配置，优先使用.env中的API密钥
        
        从JSON文件加载模型配置，并合并环境变量中的API密钥。
        支持OpenAI模型的特殊处理和启用状态管理。
        
        加载流程：
        1. 从models.json文件读取配置
        2. 反序列化为ModelConfig对象列表
        3. 获取系统设置（OpenAI启用状态）
        4. 合并环境变量中的API密钥
        5. 处理OpenAI模型的特殊逻辑
        6. 返回最终的模型配置列表
        
        环境变量优先级：
        - 环境变量中的API密钥优先级高于文件配置
        - 如果环境变量中存在API密钥，自动启用对应模型
        - OpenAI密钥需要格式验证，验证失败则禁用模型
        
        OpenAI特殊处理：
        - 检查openai_enabled设置，禁用时不启用任何OpenAI模型
        - 验证API密钥格式，格式错误时禁用模型
        - 记录禁用原因，便于用户排查问题
        
        错误处理：
        - 文件读取失败：记录错误日志，返回空列表
        - JSON解析失败：记录错误日志，返回空列表
        - 配置合并失败：记录警告，使用文件配置
        
        Returns:
            List[ModelConfig]: 模型配置对象列表，加载失败时返回空列表
            
        日志级别：
        - info：OpenAI模型禁用状态
        - warning：API密钥格式错误
        - error：配置加载失败
        """
        try:
            with open(self.models_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                models = [ModelConfig(**item) for item in data]

                # 获取设置
                settings = self.load_settings()
                openai_enabled = settings.get("openai_enabled", False)

                # 合并.env中的API密钥（优先级更高）
                for model in models:
                    env_api_key = self._get_env_api_key(model.provider)
                    if env_api_key:
                        model.api_key = env_api_key
                        # 如果.env中有API密钥，自动启用该模型
                        if not model.enabled:
                            model.enabled = True
                    
                    # 特殊处理OpenAI模型
                    if model.provider.lower() == "openai":
                        # 检查OpenAI是否在配置中启用
                        if not openai_enabled:
                            model.enabled = False
                            logger.info(f"🔒 OpenAI模型已禁用: {model.model_name}")
                        # 如果有API密钥但格式不正确，禁用模型（验证始终启用）
                        elif model.api_key and not self.validate_openai_api_key_format(model.api_key):
                            model.enabled = False
                            logger.warning(f"⚠️ OpenAI模型因密钥格式不正确而禁用: {model.model_name}")

                return models
        except Exception as e:
            logger.error(f"加载模型配置失败: {e}")
            return []
    
    def save_models(self, models: List[ModelConfig]):
        """保存模型配置"""
        try:
            data = [asdict(model) for model in models]
            with open(self.models_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        except Exception as e:
            logger.error(f"保存模型配置失败: {e}")
    
    def load_pricing(self) -> List[PricingConfig]:
        """加载定价配置"""
        try:
            with open(self.pricing_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            return [PricingConfig(**item) for item in data]
        except Exception as e:
            logger.error(f"加载定价配置失败: {e}")
            return []
    
    def save_pricing(self, pricing: List[PricingConfig]):
        """保存定价配置"""
        try:
            data = [asdict(price) for price in pricing]
            with open(self.pricing_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        except Exception as e:
            logger.error(f"保存定价配置失败: {e}")
    
    def load_usage_records(self) -> List[UsageRecord]:
        """加载使用记录"""
        try:
            if not self.usage_file.exists():
                return []
            with open(self.usage_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
                return [UsageRecord(**item) for item in data]
        except Exception as e:
            logger.error(f"加载使用记录失败: {e}")
            return []
    
    def save_usage_records(self, records: List[UsageRecord]):
        """保存使用记录"""
        try:
            data = [asdict(record) for record in records]
            with open(self.usage_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
        except Exception as e:
            logger.error(f"保存使用记录失败: {e}")
    
    def add_usage_record(self, provider: str, model_name: str, input_tokens: int,
                        output_tokens: int, session_id: str, analysis_type: str = "stock_analysis"):
        """
        添加使用记录
        
        记录LLM API调用的详细信息，包括token消耗、成本计算和会话跟踪。
        支持MongoDB和JSON文件两种存储方式，优先使用MongoDB存储。
        
        参数说明：
            provider: 模型供应商名称
            model_name: 具体的模型名称
            input_tokens: 输入token数量
            output_tokens: 输出token数量
            session_id: 会话ID，用于跟踪同一会话内的多次调用
            analysis_type: 分析类型，默认为"stock_analysis"
            
        成本计算：
        - 自动调用calculate_cost计算调用成本
        - 支持多币种（CNY、USD等）
        - 精确到小数点后6位
        
        存储策略：
        1. 优先尝试MongoDB存储（如果已连接）
        2. MongoDB失败时回退到JSON文件存储
        3. 限制记录数量防止文件过大
        4. 自动清理早期记录
        
        记录内容：
        - 时间戳（带时区信息）
        - 供应商和模型信息
        - Token消耗统计
        - 成本计算结果
        - 会话ID和分析类型
        
        错误处理：
        - MongoDB存储失败：记录错误日志，回退到文件存储
        - 成本计算失败：使用0成本继续记录
        - 文件存储失败：记录错误日志，返回None
        
        Returns:
            UsageRecord: 创建的使用记录对象，失败时返回None
            
        性能考虑：
        - 异步存储支持（MongoDB）
        - 记录数量限制（默认10000条）
        - 批量操作优化
        """
        # 计算成本和货币单位
        cost, currency = self.calculate_cost(provider, model_name, input_tokens, output_tokens)

        record = UsageRecord(
            timestamp=datetime.now(ZoneInfo(get_timezone_name())).isoformat(),
            provider=provider,
            model_name=model_name,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            cost=cost,
            currency=currency,
            session_id=session_id,
            analysis_type=analysis_type
        )
        
        # 优先使用MongoDB存储
        if self.mongodb_storage and self.mongodb_storage.is_connected():
            success = self.mongodb_storage.save_usage_record(record)
            if success:
                return record
            else:
                logger.error(f"⚠️ MongoDB保存失败，回退到JSON文件存储")
        
        # 回退到JSON文件存储
        records = self.load_usage_records()
        records.append(record)
        
        # 限制记录数量
        settings = self.load_settings()
        max_records = settings.get("max_usage_records", 10000)
        if len(records) > max_records:
            records = records[-max_records:]
        
        self.save_usage_records(records)
        return record
    
    def calculate_cost(self, provider: str, model_name: str, input_tokens: int, output_tokens: int) -> tuple[float, str]:
        """
        计算使用成本

        Returns:
            tuple[float, str]: (成本, 货币单位)
        """
        pricing_configs = self.load_pricing()

        for pricing in pricing_configs:
            if pricing.provider == provider and pricing.model_name == model_name:
                input_cost = (input_tokens / 1000) * pricing.input_price_per_1k
                output_cost = (output_tokens / 1000) * pricing.output_price_per_1k
                total_cost = input_cost + output_cost
                return round(total_cost, 6), pricing.currency

        # 只在找不到配置时输出调试信息
        logger.warning(f"⚠️ [calculate_cost] 未找到匹配的定价配置: {provider}/{model_name}")
        logger.debug(f"⚠️ [calculate_cost] 可用的配置:")
        for pricing in pricing_configs:
            logger.debug(f"⚠️ [calculate_cost]   - {pricing.provider}/{pricing.model_name}")

        return 0.0, "CNY"
    
    def load_settings(self) -> Dict[str, Any]:
        """加载设置，合并.env中的配置"""
        try:
            if self.settings_file.exists():
                with open(self.settings_file, 'r', encoding='utf-8') as f:
                    settings = json.load(f)
            else:
                # 如果设置文件不存在，创建默认设置
                settings = {
                    "default_provider": "dashscope",
                    "default_model": "qwen-turbo",
                    "enable_cost_tracking": True,
                    "cost_alert_threshold": 100.0,
                    "currency_preference": "CNY",
                    "auto_save_usage": True,
                    "max_usage_records": 10000,
                    "data_dir": os.path.join(os.path.expanduser("~"), "Documents", "TradingAgents", "data"),
                    "cache_dir": os.path.join(os.path.expanduser("~"), "Documents", "TradingAgents", "data", "cache"),
                    "results_dir": os.path.join(os.path.expanduser("~"), "Documents", "TradingAgents", "results"),
                    "auto_create_dirs": True,
                    "openai_enabled": False,
                }
                self.save_settings(settings)
        except Exception as e:
            logger.error(f"加载设置失败: {e}")
            settings = {}

        # 合并.env中的其他配置
        env_settings = {
            "finnhub_api_key": os.getenv("FINNHUB_API_KEY", ""),
            "reddit_client_id": os.getenv("REDDIT_CLIENT_ID", ""),
            "reddit_client_secret": os.getenv("REDDIT_CLIENT_SECRET", ""),
            "reddit_user_agent": os.getenv("REDDIT_USER_AGENT", ""),
            "results_dir": os.getenv("TRADINGAGENTS_RESULTS_DIR", ""),
            "log_level": os.getenv("TRADINGAGENTS_LOG_LEVEL", "INFO"),
            "data_dir": os.getenv("TRADINGAGENTS_DATA_DIR", ""),  # 数据目录环境变量
            "cache_dir": os.getenv("TRADINGAGENTS_CACHE_DIR", ""),  # 缓存目录环境变量
        }

        # 添加OpenAI相关配置
        openai_enabled_env = os.getenv("OPENAI_ENABLED", "").lower()
        if openai_enabled_env in ["true", "false"]:
            env_settings["openai_enabled"] = openai_enabled_env == "true"

        # 只有当环境变量存在且不为空时才覆盖
        for key, value in env_settings.items():
            # 对于布尔值，直接使用
            if isinstance(value, bool):
                settings[key] = value
            # 对于字符串，只有非空时才覆盖
            elif value != "" and value is not None:
                settings[key] = value

        return settings

    def get_env_config_status(self) -> Dict[str, Any]:
        """获取.env配置状态"""
        return {
            "env_file_exists": (Path(__file__).parent.parent.parent / ".env").exists(),
            "api_keys": {
                "dashscope": bool(os.getenv("DASHSCOPE_API_KEY")),
                "openai": bool(os.getenv("OPENAI_API_KEY")),
                "google": bool(os.getenv("GOOGLE_API_KEY")),
                "anthropic": bool(os.getenv("ANTHROPIC_API_KEY")),
                "finnhub": bool(os.getenv("FINNHUB_API_KEY")),
            },
            "other_configs": {
                "reddit_configured": bool(os.getenv("REDDIT_CLIENT_ID") and os.getenv("REDDIT_CLIENT_SECRET")),
                "results_dir": os.getenv("TRADINGAGENTS_RESULTS_DIR", "./results"),
                "log_level": os.getenv("TRADINGAGENTS_LOG_LEVEL", "INFO"),
            }
        }

    def save_settings(self, settings: Dict[str, Any]):
        """保存设置"""
        try:
            with open(self.settings_file, 'w', encoding='utf-8') as f:
                json.dump(settings, f, ensure_ascii=False, indent=2)
        except Exception as e:
            logger.error(f"保存设置失败: {e}")
    
    def get_enabled_models(self) -> List[ModelConfig]:
        """获取启用的模型"""
        models = self.load_models()
        return [model for model in models if model.enabled and model.api_key]
    
    def get_model_by_name(self, provider: str, model_name: str) -> Optional[ModelConfig]:
        """根据名称获取模型配置"""
        models = self.load_models()
        for model in models:
            if model.provider == provider and model.model_name == model_name:
                return model
        return None
    
    def get_usage_statistics(self, days: int = 30) -> Dict[str, Any]:
        """获取使用统计"""
        # 优先使用MongoDB获取统计
        if self.mongodb_storage and self.mongodb_storage.is_connected():
            try:
                # 从MongoDB获取基础统计
                stats = self.mongodb_storage.get_usage_statistics(days)
                # 获取供应商统计
                provider_stats = self.mongodb_storage.get_provider_statistics(days)
                
                if stats:
                    stats["provider_stats"] = provider_stats
                    stats["records_count"] = stats.get("total_requests", 0)
                    return stats
            except Exception as e:
                logger.error(f"⚠️ MongoDB统计获取失败，回退到JSON文件: {e}")
        
        # 回退到JSON文件统计
        records = self.load_usage_records()
        
        # 过滤最近N天的记录
        from datetime import datetime, timedelta

        cutoff_date = datetime.now() - timedelta(days=days)
        
        recent_records = []
        for record in records:
            try:
                record_date = datetime.fromisoformat(record.timestamp)
                if record_date >= cutoff_date:
                    recent_records.append(record)
            except:
                continue
        
        # 统计数据
        total_cost = sum(record.cost for record in recent_records)
        total_input_tokens = sum(record.input_tokens for record in recent_records)
        total_output_tokens = sum(record.output_tokens for record in recent_records)
        
        # 按供应商统计
        provider_stats = {}
        for record in recent_records:
            if record.provider not in provider_stats:
                provider_stats[record.provider] = {
                    "cost": 0,
                    "input_tokens": 0,
                    "output_tokens": 0,
                    "requests": 0
                }
            provider_stats[record.provider]["cost"] += record.cost
            provider_stats[record.provider]["input_tokens"] += record.input_tokens
            provider_stats[record.provider]["output_tokens"] += record.output_tokens
            provider_stats[record.provider]["requests"] += 1
        
        return {
            "period_days": days,
            "total_cost": round(total_cost, 4),
            "total_input_tokens": total_input_tokens,
            "total_output_tokens": total_output_tokens,
            "total_requests": len(recent_records),
            "provider_stats": provider_stats,
            "records_count": len(recent_records)
        }
    
    def get_data_dir(self) -> str:
        """获取数据目录路径"""
        settings = self.load_settings()
        data_dir = settings.get("data_dir")
        if not data_dir:
            # 如果没有配置，使用默认路径
            data_dir = os.path.join(os.path.expanduser("~"), "Documents", "TradingAgents", "data")
        return data_dir

    def set_data_dir(self, data_dir: str):
        """设置数据目录路径"""
        settings = self.load_settings()
        settings["data_dir"] = data_dir
        # 同时更新缓存目录
        settings["cache_dir"] = os.path.join(data_dir, "cache")
        self.save_settings(settings)
        
        # 如果启用自动创建目录，则创建目录
        if settings.get("auto_create_dirs", True):
            self.ensure_directories_exist()

    def ensure_directories_exist(self):
        """确保必要的目录存在"""
        settings = self.load_settings()
        
        directories = [
            settings.get("data_dir"),
            settings.get("cache_dir"),
            settings.get("results_dir"),
            os.path.join(settings.get("data_dir", ""), "finnhub_data"),
            os.path.join(settings.get("data_dir", ""), "finnhub_data", "news_data"),
            os.path.join(settings.get("data_dir", ""), "finnhub_data", "insider_sentiment"),
            os.path.join(settings.get("data_dir", ""), "finnhub_data", "insider_transactions")
        ]
        
        for directory in directories:
            if directory and not os.path.exists(directory):
                try:
                    os.makedirs(directory, exist_ok=True)
                    logger.info(f"✅ 创建目录: {directory}")
                except Exception as e:
                    logger.error(f"❌ 创建目录失败 {directory}: {e}")
    
    def set_openai_enabled(self, enabled: bool):
        """设置OpenAI模型启用状态"""
        settings = self.load_settings()
        settings["openai_enabled"] = enabled
        self.save_settings(settings)
        logger.info(f"🔧 OpenAI模型启用状态已设置为: {enabled}")
    
    def is_openai_enabled(self) -> bool:
        """检查OpenAI模型是否启用"""
        settings = self.load_settings()
        return settings.get("openai_enabled", False)
    
    def get_openai_config_status(self) -> Dict[str, Any]:
        """获取OpenAI配置状态"""
        openai_key = os.getenv("OPENAI_API_KEY", "")
        key_valid = self.validate_openai_api_key_format(openai_key) if openai_key else False
        
        return {
            "api_key_present": bool(openai_key),
            "api_key_valid_format": key_valid,
            "enabled": self.is_openai_enabled(),
            "models_available": self.is_openai_enabled() and key_valid,
            "api_key_preview": f"{openai_key[:10]}..." if openai_key else "未配置"
        }


class TokenTracker:
    """Token使用跟踪器"""

    def __init__(self, config_manager: ConfigManager):
        self.config_manager = config_manager

    def track_usage(self, provider: str, model_name: str, input_tokens: int,
                   output_tokens: int, session_id: str = None, analysis_type: str = "stock_analysis"):
        """跟踪Token使用"""
        if session_id is None:
            session_id = f"session_{datetime.now(ZoneInfo(get_timezone_name())).strftime('%Y%m%d_%H%M%S')}"

        # 检查是否启用成本跟踪
        settings = self.config_manager.load_settings()
        cost_tracking_enabled = settings.get("enable_cost_tracking", True)

        if not cost_tracking_enabled:
            return None

        # 添加使用记录
        record = self.config_manager.add_usage_record(
            provider=provider,
            model_name=model_name,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            session_id=session_id,
            analysis_type=analysis_type
        )

        # 检查成本警告
        if record:
            self._check_cost_alert(record.cost)

        return record

    def _check_cost_alert(self, current_cost: float):
        """检查成本警告"""
        settings = self.config_manager.load_settings()
        threshold = settings.get("cost_alert_threshold", 100.0)

        # 获取今日总成本
        today_stats = self.config_manager.get_usage_statistics(1)
        total_today = today_stats["total_cost"]

        if total_today >= threshold:
            logger.warning(f"⚠️ 成本警告: 今日成本已达到 ¥{total_today:.4f}，超过阈值 ¥{threshold}",
                          extra={'cost': total_today, 'threshold': threshold, 'event_type': 'cost_alert'})

    def get_session_cost(self, session_id: str) -> float:
        """获取会话成本"""
        records = self.config_manager.load_usage_records()
        session_cost = sum(record.cost for record in records if record.session_id == session_id)
        return session_cost

    def estimate_cost(self, provider: str, model_name: str, estimated_input_tokens: int,
                     estimated_output_tokens: int) -> tuple[float, str]:
        """
        估算成本

        Returns:
            tuple[float, str]: (成本, 货币单位)
        """
        return self.config_manager.calculate_cost(
            provider, model_name, estimated_input_tokens, estimated_output_tokens
        )




# 全局配置管理器实例 - 使用项目根目录的配置
def _get_project_config_dir():
    """获取项目根目录的配置目录"""
    # 从当前文件位置推断项目根目录
    current_file = Path(__file__)  # tradingagents/config/config_manager.py
    project_root = current_file.parent.parent.parent  # 向上三级到项目根目录
    return str(project_root / "config")

config_manager = ConfigManager(_get_project_config_dir())
token_tracker = TokenTracker(config_manager)
