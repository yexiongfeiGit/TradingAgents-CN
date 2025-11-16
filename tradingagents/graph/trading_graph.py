#!/usr/bin/env python3
"""
TradingAgentsGraph - 多智能体股票分析系统核心图引擎

这个模块实现了TradingAgentsGraph类，它使用LangGraph框架来协调多个AI智能体的协作，
为股票分析提供全面的投资决策支持。系统采用多智能体架构，包括分析师、研究员、交易员和风险管理团队。

主要功能：
- 多LLM供应商支持（Google、OpenAI、Anthropic、DeepSeek等）
- 智能体协作分析流程编排
- 实时进度跟踪和性能监控
- 内存管理和学习优化
- 多市场支持（A股、港股、美股）

作者: TradingAgents-CN Team
版本: 2.0.0
"""

import os
from pathlib import Path
import json
from datetime import date
from typing import Dict, Any, Tuple, List, Optional
import time

from langchain_openai import ChatOpenAI
from langchain_anthropic import ChatAnthropic
from langchain_google_genai import ChatGoogleGenerativeAI
from tradingagents.llm_adapters import ChatDashScopeOpenAI, ChatGoogleOpenAI

from langgraph.prebuilt import ToolNode

from tradingagents.agents import *
from tradingagents.default_config import DEFAULT_CONFIG
from tradingagents.agents.utils.memory import FinancialSituationMemory

# 导入统一日志系统
from tradingagents.utils.logging_init import get_logger

# 导入日志模块
from tradingagents.utils.logging_manager import get_logger
logger = get_logger('agents')
from tradingagents.agents.utils.agent_states import (
    AgentState,
    InvestDebateState,
    RiskDebateState,
)
from tradingagents.dataflows.interface import set_config

from .conditional_logic import ConditionalLogic
from .setup import GraphSetup
from .propagation import Propagator
from .reflection import Reflector
from .signal_processing import SignalProcessor


def create_llm_by_provider(provider: str, model: str, backend_url: str, temperature: float, max_tokens: int, timeout: int, api_key: str = None):
    """
    根据 provider 创建对应的 LLM 实例 - 工厂方法
    
    支持多LLM供应商的统一创建接口，处理不同供应商的API密钥管理、基础URL配置和模型参数设置。
    支持从数据库配置或环境变量读取API密钥，实现灵活的多供应商LLM集成。

    Args:
        provider: 供应商名称 (google, dashscope, deepseek, openai, anthropic, siliconflow, openrouter, ollama, qianfan, custom_openai)
        model: 模型名称 (如: gemini-1.5-pro, qwen-max, deepseek-chat, gpt-4, claude-3-opus等)
        backend_url: API 基础地址，支持自定义API端点
        temperature: 温度参数 (0.0-1.0)，控制生成文本的随机性
        max_tokens: 最大token数，限制生成文本长度
        timeout: 请求超时时间（秒）
        api_key: API密钥（可选），如果未提供则从环境变量读取

    Returns:
        LLM实例：根据供应商返回对应的LangChain LLM对象
        
    Raises:
        ValueError: 当必需的环境变量未设置时抛出
        
    Examples:
        >>> llm = create_llm_by_provider("google", "gemini-1.5-pro", None, 0.7, 4000, 30)
        >>> llm = create_llm_by_provider("dashscope", "qwen-max", None, 0.8, 2000, 60, "your-api-key")
    """
    from tradingagents.llm_adapters.deepseek_adapter import ChatDeepSeek
    from tradingagents.llm_adapters.openai_compatible_base import create_openai_compatible_llm

    logger.info(f"🔧 [创建LLM] provider={provider}, model={model}, url={backend_url}")
    logger.info(f"🔑 [API Key] 来源: {'数据库配置' if api_key else '环境变量'}")

    if provider.lower() == "google":
        # 优先使用传入的 API Key，否则从环境变量读取
        google_api_key = api_key or os.getenv('GOOGLE_API_KEY')
        if not google_api_key:
            raise ValueError("使用Google需要设置GOOGLE_API_KEY环境变量或在数据库中配置API Key")

        # 传递 base_url 参数，使厂家配置的 default_base_url 生效
        return ChatGoogleOpenAI(
            model=model,
            google_api_key=google_api_key,
            base_url=backend_url if backend_url else None,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=timeout
        )

    elif provider.lower() == "dashscope":
        # 优先使用传入的 API Key，否则从环境变量读取
        dashscope_api_key = api_key or os.getenv('DASHSCOPE_API_KEY')

        # 传递 base_url 参数，使厂家配置的 default_base_url 生效
        return ChatDashScopeOpenAI(
            model=model,
            api_key=dashscope_api_key,  # 🔥 传递 API Key
            base_url=backend_url if backend_url else None,  # 如果有自定义 URL 则使用
            temperature=temperature,
            max_tokens=max_tokens,
            request_timeout=timeout
        )

    elif provider.lower() == "deepseek":
        # 优先使用传入的 API Key，否则从环境变量读取
        deepseek_api_key = api_key or os.getenv('DEEPSEEK_API_KEY')
        if not deepseek_api_key:
            raise ValueError("使用DeepSeek需要设置DEEPSEEK_API_KEY环境变量或在数据库中配置API Key")

        return ChatDeepSeek(
            model=model,
            api_key=deepseek_api_key,
            base_url=backend_url,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=timeout
        )

    elif provider.lower() in ["openai", "siliconflow", "openrouter", "ollama"]:
        # 优先使用传入的 API Key，否则从环境变量读取
        if not api_key:
            if provider.lower() == "siliconflow":
                api_key = os.getenv('SILICONFLOW_API_KEY')
            elif provider.lower() == "openrouter":
                api_key = os.getenv('OPENROUTER_API_KEY') or os.getenv('OPENAI_API_KEY')
            elif provider.lower() == "openai":
                api_key = os.getenv('OPENAI_API_KEY')

        return ChatOpenAI(
            model=model,
            base_url=backend_url,
            api_key=api_key,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=timeout
        )

    elif provider.lower() == "anthropic":
        return ChatAnthropic(
            model=model,
            base_url=backend_url,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=timeout
        )

    elif provider.lower() in ["qianfan", "custom_openai"]:
        return create_openai_compatible_llm(
            provider=provider,
            model=model,
            base_url=backend_url,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=timeout
        )

    else:
        # 🔧 自定义厂家：使用 OpenAI 兼容模式
        logger.info(f"🔧 使用 OpenAI 兼容模式处理自定义厂家: {provider}")

        # 尝试从环境变量获取 API Key（支持多种命名格式）
        api_key_candidates = [
            f"{provider.upper()}_API_KEY",  # 例如: KYX_API_KEY
            f"{provider}_API_KEY",          # 例如: kyx_API_KEY
            "CUSTOM_OPENAI_API_KEY"         # 通用环境变量
        ]

        custom_api_key = None
        for env_var in api_key_candidates:
            custom_api_key = os.getenv(env_var)
            if custom_api_key:
                logger.info(f"✅ 从环境变量 {env_var} 获取到 API Key")
                break

        if not custom_api_key:
            logger.warning(f"⚠️ 未找到自定义厂家 {provider} 的 API Key，尝试使用默认配置")

        return ChatOpenAI(
            model=model,
            base_url=backend_url,
            api_key=custom_api_key,
            temperature=temperature,
            max_tokens=max_tokens,
            timeout=timeout
        )


class TradingAgentsGraph:
    """
    多智能体股票分析系统核心图引擎 - 主类
    
    TradingAgentsGraph是系统的核心协调器，使用LangGraph框架来编排多个AI智能体的协作流程。
    它管理整个股票分析生命周期，从数据收集到最终投资决策的生成。
    
    核心职责：
    1. 多LLM供应商管理 - 支持Google、OpenAI、Anthropic、DeepSeek等
    2. 智能体生命周期管理 - 创建、配置和管理各类分析师、研究员、交易员
    3. 分析流程编排 - 定义智能体间的协作顺序和数据流转
    4. 性能监控 - 跟踪各节点执行时间和资源消耗
    5. 内存管理 - 管理分析历史和决策反思
    6. 进度跟踪 - 实时报告分析进度给前端
    
    支持的分析师类型：
    - market: 市场分析师（技术分析）
    - social: 社交媒体分析师（情绪分析）  
    - news: 新闻分析师（新闻事件影响）
    - fundamentals: 基本面分析师（财务分析）
    
    配置参数：
    - 支持混合模式：快速模型和深度模型可来自不同供应商
    - 支持单一供应商模式：所有模型来自同一供应商
    - 支持自定义API端点和密钥管理
    """

    def __init__(
        self,
        selected_analysts=["market", "social", "news", "fundamentals"],
        debug=False,
        config: Dict[str, Any] = None,
    ):
        """
        初始化TradingAgents图引擎和组件
        
        负责创建和配置所有必需的智能体、工具和数据处理器。根据配置选择性地启用
        不同类型的分析师，并设置LLM供应商、内存管理和调试模式。
        
        Args:
            selected_analysts: 启用的分析师类型列表，默认为全部4种分析师
                - "market": 市场技术分析
                - "social": 社交媒体情绪分析  
                - "news": 新闻事件分析
                - "fundamentals": 基本面财务分析
            debug: 是否启用调试模式，会输出更详细的日志信息
            config: 配置字典，如果为None则使用DEFAULT_CONFIG默认配置
                
        配置包含：
        - LLM供应商设置（快速模型和深度模型）
        - API密钥和端点配置
        - 内存管理设置
        - 分析流程参数
        
        初始化流程：
        1. 设置配置和调试模式
        2. 创建LLM实例（快速模型和深度模型）
        3. 初始化内存管理器（如果启用）
        4. 创建各类智能体实例
        5. 构建分析图结构
        """
        self.debug = debug
        self.config = config or DEFAULT_CONFIG

        # Update the interface's config
        set_config(self.config)

        # Create necessary directories
        os.makedirs(
            os.path.join(self.config["project_dir"], "dataflows/data_cache"),
            exist_ok=True,
        )

        # Initialize LLMs
        # 🔧 从配置中读取模型参数（优先使用用户配置，否则使用默认值）
        quick_config = self.config.get("quick_model_config", {})
        deep_config = self.config.get("deep_model_config", {})

        # 读取快速模型参数
        quick_max_tokens = quick_config.get("max_tokens", 4000)
        quick_temperature = quick_config.get("temperature", 0.7)
        quick_timeout = quick_config.get("timeout", 180)

        # 读取深度模型参数
        deep_max_tokens = deep_config.get("max_tokens", 4000)
        deep_temperature = deep_config.get("temperature", 0.7)
        deep_timeout = deep_config.get("timeout", 180)

        # 🔧 检查是否为混合模式（快速模型和深度模型来自不同厂家）
        quick_provider = self.config.get("quick_provider")
        deep_provider = self.config.get("deep_provider")
        quick_backend_url = self.config.get("quick_backend_url")
        deep_backend_url = self.config.get("deep_backend_url")

        if quick_provider and deep_provider and quick_provider != deep_provider:
            # 混合模式：快速模型和深度模型来自不同厂家
            logger.info(f"🔀 [混合模式] 检测到不同厂家的模型组合")
            logger.info(f"   快速模型: {self.config['quick_think_llm']} ({quick_provider})")
            logger.info(f"   深度模型: {self.config['deep_think_llm']} ({deep_provider})")

            # 使用统一的函数创建 LLM 实例
            self.quick_thinking_llm = create_llm_by_provider(
                provider=quick_provider,
                model=self.config["quick_think_llm"],
                backend_url=quick_backend_url or self.config.get("backend_url", ""),
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout,
                api_key=self.config.get("quick_api_key")  # 🔥 传递 API Key
            )

            self.deep_thinking_llm = create_llm_by_provider(
                provider=deep_provider,
                model=self.config["deep_think_llm"],
                backend_url=deep_backend_url or self.config.get("backend_url", ""),
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout,
                api_key=self.config.get("deep_api_key")  # 🔥 传递 API Key
            )

            logger.info(f"✅ [混合模式] LLM 实例创建成功")
            
        elif self.config["llm_provider"].lower() == "baidu" or "百度" in self.config["llm_provider"]:
            # 百度千帆大模型支持
            logger.info(f"🔧 使用百度千帆大模型 (OpenAI 兼容模式)")
            
            # 🔥 优先使用数据库配置的 API Key，否则从环境变量读取
            qianfan_api_key = self.config.get("quick_api_key") or self.config.get("deep_api_key") or os.getenv('QIANFAN_API_KEY')
            if not qianfan_api_key:
                raise ValueError("使用百度千帆需要在数据库中配置API Key或设置QIANFAN_API_KEY环境变量")
            
            logger.info(f"🔑 [百度千帆] API Key 来源: {'数据库配置' if self.config.get('quick_api_key') or self.config.get('deep_api_key') else '环境变量'}")
            
            # 🔧 从配置中读取模型参数（优先使用用户配置，否则使用默认值）
            quick_config = self.config.get("quick_model_config", {})
            deep_config = self.config.get("deep_model_config", {})
            
            quick_max_tokens = quick_config.get("max_tokens", 4000)
            quick_temperature = quick_config.get("temperature", 0.7)
            quick_timeout = quick_config.get("timeout", 180)
            
            deep_max_tokens = deep_config.get("max_tokens", 4000)
            deep_temperature = deep_config.get("temperature", 0.7)
            deep_timeout = deep_config.get("timeout", 180)
            
            logger.info(f"🔧 [百度千帆-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [百度千帆-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")
            
            # 获取 backend_url（如果配置中有的话）
            backend_url = self.config.get("backend_url")
            if backend_url:
                logger.info(f"🔧 [百度千帆] 使用配置的 backend_url: {backend_url}")
            else:
                logger.info(f"🔧 [百度千帆] 未配置 backend_url，使用默认端点")
            
            from tradingagents.llm_adapters.openai_compatible_base import create_openai_compatible_llm
            
            self.deep_thinking_llm = create_openai_compatible_llm(
                provider="qianfan",
                model=self.config["deep_think_llm"],
                base_url=backend_url,
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = create_openai_compatible_llm(
                provider="qianfan", 
                model=self.config["quick_think_llm"],
                base_url=backend_url,
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )
            
            logger.info(f"✅ [百度千帆] 已启用千帆大模型支持并应用用户配置的模型参数")

        elif self.config["llm_provider"].lower() == "openai":
            logger.info(f"🔧 [OpenAI-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [OpenAI-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            self.deep_thinking_llm = ChatOpenAI(
                model=self.config["deep_think_llm"],
                base_url=self.config["backend_url"],
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = ChatOpenAI(
                model=self.config["quick_think_llm"],
                base_url=self.config["backend_url"],
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )
        elif self.config["llm_provider"] == "siliconflow":
            # SiliconFlow支持：使用OpenAI兼容API
            siliconflow_api_key = os.getenv('SILICONFLOW_API_KEY')
            if not siliconflow_api_key:
                raise ValueError("使用SiliconFlow需要设置SILICONFLOW_API_KEY环境变量")

            logger.info(f"🌐 [SiliconFlow] 使用API密钥: {siliconflow_api_key[:20]}...")
            logger.info(f"🔧 [SiliconFlow-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [SiliconFlow-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            self.deep_thinking_llm = ChatOpenAI(
                model=self.config["deep_think_llm"],
                base_url=self.config["backend_url"],
                api_key=siliconflow_api_key,
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = ChatOpenAI(
                model=self.config["quick_think_llm"],
                base_url=self.config["backend_url"],
                api_key=siliconflow_api_key,
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )
        elif self.config["llm_provider"] == "openrouter":
            # OpenRouter支持：优先使用OPENROUTER_API_KEY，否则使用OPENAI_API_KEY
            openrouter_api_key = os.getenv('OPENROUTER_API_KEY') or os.getenv('OPENAI_API_KEY')
            if not openrouter_api_key:
                raise ValueError("使用OpenRouter需要设置OPENROUTER_API_KEY或OPENAI_API_KEY环境变量")

            logger.info(f"🌐 [OpenRouter] 使用API密钥: {openrouter_api_key[:20]}...")
            logger.info(f"🔧 [OpenRouter-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [OpenRouter-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            self.deep_thinking_llm = ChatOpenAI(
                model=self.config["deep_think_llm"],
                base_url=self.config["backend_url"],
                api_key=openrouter_api_key,
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = ChatOpenAI(
                model=self.config["quick_think_llm"],
                base_url=self.config["backend_url"],
                api_key=openrouter_api_key,
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )
        elif self.config["llm_provider"] == "ollama":
            logger.info(f"🔧 [Ollama-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [Ollama-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            self.deep_thinking_llm = ChatOpenAI(
                model=self.config["deep_think_llm"],
                base_url=self.config["backend_url"],
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = ChatOpenAI(
                model=self.config["quick_think_llm"],
                base_url=self.config["backend_url"],
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )
        elif self.config["llm_provider"].lower() == "anthropic":
            logger.info(f"🔧 [Anthropic-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [Anthropic-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            self.deep_thinking_llm = ChatAnthropic(
                model=self.config["deep_think_llm"],
                base_url=self.config["backend_url"],
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = ChatAnthropic(
                model=self.config["quick_think_llm"],
                base_url=self.config["backend_url"],
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )
        elif self.config["llm_provider"].lower() == "google":
            # 使用 Google OpenAI 兼容适配器，解决工具调用格式不匹配问题
            logger.info(f"🔧 使用Google AI OpenAI 兼容适配器 (解决工具调用问题)")

            # 🔥 优先使用数据库配置的 API Key，否则从环境变量读取
            google_api_key = self.config.get("quick_api_key") or self.config.get("deep_api_key") or os.getenv('GOOGLE_API_KEY')
            if not google_api_key:
                raise ValueError("使用Google AI需要在数据库中配置API Key或设置GOOGLE_API_KEY环境变量")

            logger.info(f"🔑 [Google AI] API Key 来源: {'数据库配置' if self.config.get('quick_api_key') or self.config.get('deep_api_key') else '环境变量'}")

            # 🔧 从配置中读取模型参数（优先使用用户配置，否则使用默认值）
            quick_config = self.config.get("quick_model_config", {})
            deep_config = self.config.get("deep_model_config", {})

            quick_max_tokens = quick_config.get("max_tokens", 4000)
            quick_temperature = quick_config.get("temperature", 0.7)
            quick_timeout = quick_config.get("timeout", 180)

            deep_max_tokens = deep_config.get("max_tokens", 4000)
            deep_temperature = deep_config.get("temperature", 0.7)
            deep_timeout = deep_config.get("timeout", 180)

            logger.info(f"🔧 [Google-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [Google-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            # 获取 backend_url（如果配置中有的话）
            backend_url = self.config.get("backend_url")
            if backend_url:
                logger.info(f"🔧 [Google AI] 使用配置的 backend_url: {backend_url}")
            else:
                logger.info(f"🔧 [Google AI] 未配置 backend_url，使用默认端点")

            self.deep_thinking_llm = ChatGoogleOpenAI(
                model=self.config["deep_think_llm"],
                google_api_key=google_api_key,
                base_url=backend_url if backend_url else None,
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = ChatGoogleOpenAI(
                model=self.config["quick_think_llm"],
                google_api_key=google_api_key,
                base_url=backend_url if backend_url else None,
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout,
                transport="rest"
            )

            logger.info(f"✅ [Google AI] 已启用优化的工具调用和内容格式处理并应用用户配置的模型参数")
        elif (self.config["llm_provider"].lower() == "dashscope" or
              self.config["llm_provider"].lower() == "alibaba" or
              "dashscope" in self.config["llm_provider"].lower() or
              "阿里百炼" in self.config["llm_provider"]):
            # 使用 OpenAI 兼容适配器，支持原生 Function Calling
            logger.info(f"🔧 使用阿里百炼 OpenAI 兼容适配器 (支持原生工具调用)")

            # 🔥 优先使用数据库配置的 API Key，否则从环境变量读取
            dashscope_api_key = self.config.get("quick_api_key") or self.config.get("deep_api_key") or os.getenv('DASHSCOPE_API_KEY')
            logger.info(f"🔑 [阿里百炼] API Key 来源: {'数据库配置' if self.config.get('quick_api_key') or self.config.get('deep_api_key') else '环境变量'}")

            # 🔧 从配置中读取模型参数（优先使用用户配置，否则使用默认值）
            quick_config = self.config.get("quick_model_config", {})
            deep_config = self.config.get("deep_model_config", {})

            # 读取快速模型参数
            quick_max_tokens = quick_config.get("max_tokens", 4000)
            quick_temperature = quick_config.get("temperature", 0.7)
            quick_timeout = quick_config.get("timeout", 180)

            # 读取深度模型参数
            deep_max_tokens = deep_config.get("max_tokens", 4000)
            deep_temperature = deep_config.get("temperature", 0.7)
            deep_timeout = deep_config.get("timeout", 180)

            logger.info(f"🔧 [阿里百炼-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [阿里百炼-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            # 获取 backend_url（如果配置中有的话）
            backend_url = self.config.get("backend_url")
            if backend_url:
                logger.info(f"🔧 [阿里百炼] 使用自定义 API 地址: {backend_url}")

            # 🔥 详细日志：打印所有 LLM 初始化参数
            logger.info("=" * 80)
            logger.info("🤖 [LLM初始化] 阿里百炼深度模型参数:")
            logger.info(f"   model: {self.config['deep_think_llm']}")
            logger.info(f"   api_key: {'有值' if dashscope_api_key else '空'} (长度: {len(dashscope_api_key) if dashscope_api_key else 0})")
            logger.info(f"   base_url: {backend_url if backend_url else '默认'}")
            logger.info(f"   temperature: {deep_temperature}")
            logger.info(f"   max_tokens: {deep_max_tokens}")
            logger.info(f"   request_timeout: {deep_timeout}")
            logger.info("=" * 80)

            self.deep_thinking_llm = ChatDashScopeOpenAI(
                model=self.config["deep_think_llm"],
                api_key=dashscope_api_key,  # 🔥 传递 API Key
                base_url=backend_url if backend_url else None,  # 传递 base_url
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                request_timeout=deep_timeout
            )

            logger.info("=" * 80)
            logger.info("🤖 [LLM初始化] 阿里百炼快速模型参数:")
            logger.info(f"   model: {self.config['quick_think_llm']}")
            logger.info(f"   api_key: {'有值' if dashscope_api_key else '空'} (长度: {len(dashscope_api_key) if dashscope_api_key else 0})")
            logger.info(f"   base_url: {backend_url if backend_url else '默认'}")
            logger.info(f"   temperature: {quick_temperature}")
            logger.info(f"   max_tokens: {quick_max_tokens}")
            logger.info(f"   request_timeout: {quick_timeout}")
            logger.info("=" * 80)

            self.quick_thinking_llm = ChatDashScopeOpenAI(
                model=self.config["quick_think_llm"],
                api_key=dashscope_api_key,  # 🔥 传递 API Key
                base_url=backend_url if backend_url else None,  # 传递 base_url
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                request_timeout=quick_timeout
            )
            logger.info(f"✅ [阿里百炼] 已应用用户配置的模型参数")
        elif (self.config["llm_provider"].lower() == "deepseek" or
              "deepseek" in self.config["llm_provider"].lower()):
            # DeepSeek V3配置 - 使用支持token统计的适配器
            from tradingagents.llm_adapters.deepseek_adapter import ChatDeepSeek

            deepseek_api_key = os.getenv('DEEPSEEK_API_KEY')
            if not deepseek_api_key:
                raise ValueError("使用DeepSeek需要设置DEEPSEEK_API_KEY环境变量")

            deepseek_base_url = os.getenv('DEEPSEEK_BASE_URL', 'https://api.deepseek.com')

            # 🔧 从配置中读取模型参数（优先使用用户配置，否则使用默认值）
            quick_config = self.config.get("quick_model_config", {})
            deep_config = self.config.get("deep_model_config", {})

            # 读取快速模型参数
            quick_max_tokens = quick_config.get("max_tokens", 4000)
            quick_temperature = quick_config.get("temperature", 0.7)
            quick_timeout = quick_config.get("timeout", 180)

            # 读取深度模型参数
            deep_max_tokens = deep_config.get("max_tokens", 4000)
            deep_temperature = deep_config.get("temperature", 0.7)
            deep_timeout = deep_config.get("timeout", 180)

            logger.info(f"🔧 [DeepSeek-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [DeepSeek-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            # 使用支持token统计的DeepSeek适配器
            self.deep_thinking_llm = ChatDeepSeek(
                model=self.config["deep_think_llm"],
                api_key=deepseek_api_key,
                base_url=deepseek_base_url,
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = ChatDeepSeek(
                model=self.config["quick_think_llm"],
                api_key=deepseek_api_key,
                base_url=deepseek_base_url,
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )

            logger.info(f"✅ [DeepSeek] 已启用token统计功能并应用用户配置的模型参数")
        elif self.config["llm_provider"].lower() == "custom_openai":
            # 自定义OpenAI端点配置
            from tradingagents.llm_adapters.openai_compatible_base import create_openai_compatible_llm

            custom_api_key = os.getenv('CUSTOM_OPENAI_API_KEY')
            if not custom_api_key:
                raise ValueError("使用自定义OpenAI端点需要设置CUSTOM_OPENAI_API_KEY环境变量")

            custom_base_url = self.config.get("custom_openai_base_url", "https://api.openai.com/v1")

            # 🔧 从配置中读取模型参数（优先使用用户配置，否则使用默认值）
            quick_config = self.config.get("quick_model_config", {})
            deep_config = self.config.get("deep_model_config", {})

            quick_max_tokens = quick_config.get("max_tokens", 4000)
            quick_temperature = quick_config.get("temperature", 0.7)
            quick_timeout = quick_config.get("timeout", 180)

            deep_max_tokens = deep_config.get("max_tokens", 4000)
            deep_temperature = deep_config.get("temperature", 0.7)
            deep_timeout = deep_config.get("timeout", 180)

            logger.info(f"🔧 [自定义OpenAI] 使用端点: {custom_base_url}")
            logger.info(f"🔧 [自定义OpenAI-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [自定义OpenAI-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            # 使用OpenAI兼容适配器创建LLM实例
            self.deep_thinking_llm = create_openai_compatible_llm(
                provider="custom_openai",
                model=self.config["deep_think_llm"],
                base_url=custom_base_url,
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = create_openai_compatible_llm(
                provider="custom_openai",
                model=self.config["quick_think_llm"],
                base_url=custom_base_url,
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )

            logger.info(f"✅ [自定义OpenAI] 已配置自定义端点并应用用户配置的模型参数")
        elif self.config["llm_provider"].lower() == "qianfan":
            # 百度千帆（文心一言）配置 - 统一由适配器内部读取与校验 QIANFAN_API_KEY
            from tradingagents.llm_adapters.openai_compatible_base import create_openai_compatible_llm

            # 🔧 从配置中读取模型参数（优先使用用户配置，否则使用默认值）
            quick_config = self.config.get("quick_model_config", {})
            deep_config = self.config.get("deep_model_config", {})

            quick_max_tokens = quick_config.get("max_tokens", 4000)
            quick_temperature = quick_config.get("temperature", 0.7)
            quick_timeout = quick_config.get("timeout", 180)

            deep_max_tokens = deep_config.get("max_tokens", 4000)
            deep_temperature = deep_config.get("temperature", 0.7)
            deep_timeout = deep_config.get("timeout", 180)

            logger.info(f"🔧 [千帆-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [千帆-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            # 使用OpenAI兼容适配器创建LLM实例（基类会使用千帆默认base_url并负责密钥校验）
            self.deep_thinking_llm = create_openai_compatible_llm(
                provider="qianfan",
                model=self.config["deep_think_llm"],
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = create_openai_compatible_llm(
                provider="qianfan",
                model=self.config["quick_think_llm"],
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )
            logger.info("✅ [千帆] 文心一言适配器已配置成功并应用用户配置的模型参数")
        else:
            # 🔧 通用的 OpenAI 兼容厂家支持（用于自定义厂家）
            logger.info(f"🔧 使用通用 OpenAI 兼容适配器处理自定义厂家: {self.config['llm_provider']}")
            from tradingagents.llm_adapters.openai_compatible_base import create_openai_compatible_llm

            # 获取厂家配置中的 API Key 和 base_url
            provider_name = self.config['llm_provider']

            # 尝试从环境变量获取 API Key（支持多种命名格式）
            api_key_candidates = [
                f"{provider_name.upper()}_API_KEY",  # 例如: KYX_API_KEY
                f"{provider_name}_API_KEY",          # 例如: kyx_API_KEY
                "CUSTOM_OPENAI_API_KEY"              # 通用环境变量
            ]

            custom_api_key = None
            for env_var in api_key_candidates:
                custom_api_key = os.getenv(env_var)
                if custom_api_key:
                    logger.info(f"✅ 从环境变量 {env_var} 获取到 API Key")
                    break

            if not custom_api_key:
                raise ValueError(
                    f"使用自定义厂家 {provider_name} 需要设置以下环境变量之一:\n"
                    f"  - {provider_name.upper()}_API_KEY\n"
                    f"  - CUSTOM_OPENAI_API_KEY"
                )

            # 获取 backend_url（从配置中获取）
            backend_url = self.config.get("backend_url")
            if not backend_url:
                raise ValueError(
                    f"使用自定义厂家 {provider_name} 需要在数据库配置中设置 default_base_url"
                )

            logger.info(f"🔧 [自定义厂家 {provider_name}] 使用端点: {backend_url}")

            # 🔧 从配置中读取模型参数
            quick_config = self.config.get("quick_model_config", {})
            deep_config = self.config.get("deep_model_config", {})

            quick_max_tokens = quick_config.get("max_tokens", 4000)
            quick_temperature = quick_config.get("temperature", 0.7)
            quick_timeout = quick_config.get("timeout", 180)

            deep_max_tokens = deep_config.get("max_tokens", 4000)
            deep_temperature = deep_config.get("temperature", 0.7)
            deep_timeout = deep_config.get("timeout", 180)

            logger.info(f"🔧 [{provider_name}-快速模型] max_tokens={quick_max_tokens}, temperature={quick_temperature}, timeout={quick_timeout}s")
            logger.info(f"🔧 [{provider_name}-深度模型] max_tokens={deep_max_tokens}, temperature={deep_temperature}, timeout={deep_timeout}s")

            # 使用 custom_openai 适配器创建 LLM 实例
            self.deep_thinking_llm = create_openai_compatible_llm(
                provider="custom_openai",
                model=self.config["deep_think_llm"],
                api_key=custom_api_key,
                base_url=backend_url,
                temperature=deep_temperature,
                max_tokens=deep_max_tokens,
                timeout=deep_timeout
            )
            self.quick_thinking_llm = create_openai_compatible_llm(
                provider="custom_openai",
                model=self.config["quick_think_llm"],
                api_key=custom_api_key,
                base_url=backend_url,
                temperature=quick_temperature,
                max_tokens=quick_max_tokens,
                timeout=quick_timeout
            )

            logger.info(f"✅ [自定义厂家 {provider_name}] 已配置自定义端点并应用用户配置的模型参数")
        
        self.toolkit = Toolkit(config=self.config)

        # Initialize memories (如果启用)
        memory_enabled = self.config.get("memory_enabled", True)
        if memory_enabled:
            # 使用单例ChromaDB管理器，避免并发创建冲突
            self.bull_memory = FinancialSituationMemory("bull_memory", self.config)
            self.bear_memory = FinancialSituationMemory("bear_memory", self.config)
            self.trader_memory = FinancialSituationMemory("trader_memory", self.config)
            self.invest_judge_memory = FinancialSituationMemory("invest_judge_memory", self.config)
            self.risk_manager_memory = FinancialSituationMemory("risk_manager_memory", self.config)
        else:
            # 创建空的内存对象
            self.bull_memory = None
            self.bear_memory = None
            self.trader_memory = None
            self.invest_judge_memory = None
            self.risk_manager_memory = None

        # Create tool nodes
        self.tool_nodes = self._create_tool_nodes()

        # Initialize components
        # 🔥 [修复] 从配置中读取辩论轮次参数
        self.conditional_logic = ConditionalLogic(
            max_debate_rounds=self.config.get("max_debate_rounds", 1),
            max_risk_discuss_rounds=self.config.get("max_risk_discuss_rounds", 1)
        )
        logger.info(f"🔧 [ConditionalLogic] 初始化完成:")
        logger.info(f"   - max_debate_rounds: {self.conditional_logic.max_debate_rounds}")
        logger.info(f"   - max_risk_discuss_rounds: {self.conditional_logic.max_risk_discuss_rounds}")

        self.graph_setup = GraphSetup(
            self.quick_thinking_llm,
            self.deep_thinking_llm,
            self.toolkit,
            self.tool_nodes,
            self.bull_memory,
            self.bear_memory,
            self.trader_memory,
            self.invest_judge_memory,
            self.risk_manager_memory,
            self.conditional_logic,
            self.config,
            getattr(self, 'react_llm', None),
        )

        self.propagator = Propagator()
        self.reflector = Reflector(self.quick_thinking_llm)
        self.signal_processor = SignalProcessor(self.quick_thinking_llm)

        # State tracking
        self.curr_state = None
        self.ticker = None
        self.log_states_dict = {}  # date to full state dict

        # Set up the graph
        self.graph = self.graph_setup.setup_graph(selected_analysts)

    def _create_tool_nodes(self) -> Dict[str, ToolNode]:
        """Create tool nodes for different data sources.

        创建不同数据源的工具节点，用于支持智能体获取市场数据、社交情绪、基本面和新闻信息。
        
        工具节点设计原则：
        - 统一工具（推荐）：get_stock_market_data_unified 等，支持多数据源自动切换
        - 在线工具（备用）：Yahoo Finance、股票统计指标等在线数据源
        - 离线工具（备用）：Reddit、本地缓存等离线数据源
        
        注意：ToolNode 包含所有可能的工具，但 LLM 只会调用它绑定的工具。
        ToolNode 的作用是执行 LLM 生成的 tool_calls，而不是限制 LLM 可以调用哪些工具。
        
        Returns:
            Dict[str, ToolNode]: 包含市场、社交、基本面、新闻四类工具节点的字典
            
        工具节点分类：
            - market: 市场数据工具（股价、技术指标等）
            - social: 社交情绪工具（Reddit、新闻情绪等）
            - fundamental: 基本面工具（财务报表、估值等）
            - news: 新闻工具（实时新闻、公告等）
        """
        return {
            "market": ToolNode(
                [
                    # 统一工具（推荐）
                    self.toolkit.get_stock_market_data_unified,
                    # 在线工具（备用）
                    self.toolkit.get_YFin_data_online,
                    self.toolkit.get_stockstats_indicators_report_online,
                    # 离线工具（备用）
                    self.toolkit.get_YFin_data,
                    self.toolkit.get_stockstats_indicators_report,
                ]
            ),
            "social": ToolNode(
                [
                    # 统一工具（推荐）
                    self.toolkit.get_stock_sentiment_unified,
                    # 在线工具（备用）
                    self.toolkit.get_stock_news_openai,
                    # 离线工具（备用）
                    self.toolkit.get_reddit_stock_info,
                ]
            ),
            "news": ToolNode(
                [
                    # 统一工具（推荐）
                    self.toolkit.get_stock_news_unified,
                    # 在线工具（备用）
                    self.toolkit.get_global_news_openai,
                    self.toolkit.get_google_news,
                    # 离线工具（备用）
                    self.toolkit.get_finnhub_news,
                    self.toolkit.get_reddit_news,
                ]
            ),
            "fundamentals": ToolNode(
                [
                    # 统一工具（推荐）
                    self.toolkit.get_stock_fundamentals_unified,
                    # 离线工具（备用）
                    self.toolkit.get_finnhub_company_insider_sentiment,
                    self.toolkit.get_finnhub_company_insider_transactions,
                    self.toolkit.get_simfin_balance_sheet,
                    self.toolkit.get_simfin_cashflow,
                    self.toolkit.get_simfin_income_stmt,
                    # 中国市场工具（备用）
                    self.toolkit.get_china_stock_data,
                    self.toolkit.get_china_fundamentals,
                ]
            ),
        }

    def propagate(self, company_name, trade_date, progress_callback=None, task_id=None):
        """执行完整的交易分析流程，协调多个智能体进行市场分析、辩论和决策。
        
        这是整个系统的核心入口方法，负责协调所有智能体完成从数据收集到最终决策的完整流程。
        
        核心流程：
        1. 初始化状态并记录开始时间
        2. 执行图结构工作流（分析师->研究员->交易员->风险管理）
        3. 收集各节点执行时间统计
        4. 生成详细的性能分析报告
        5. 保存历史状态到日志文件
        
        Args:
            company_name (str): 公司名称或股票代码（如 "NVDA", "AAPL"）
            trade_date (str): 交易日期，格式为YYYY-MM-DD（如 "2024-05-10"）
            progress_callback (callable, optional): 进度回调函数，用于实时更新分析进度
            task_id (str, optional): 任务ID，用于跟踪和日志记录
            
        Returns:
            dict: 包含完整分析结果和最终交易决策的字典对象
            
        返回数据结构：
            - company_of_interest: 目标公司信息
            - trade_date: 分析日期
            - market_report: 市场数据分析报告
            - sentiment_report: 情绪分析报告  
            - news_report: 新闻分析报告
            - fundamentals_report: 基本面分析报告
            - investment_debate_state: 投资辩论历史和决策
            - trader_investment_decision: 交易员投资计划
            - risk_debate_state: 风险辩论历史和决策
            - investment_plan: 最终投资计划
            - final_trade_decision: 最终交易决策
            - performance_metrics: 性能统计信息
            - llm_config: LLM配置信息
        """
        # 运行交易智能体图结构，为指定公司在特定日期执行完整分析

        # 添加详细的接收日志，便于调试和跟踪
        logger.debug(f"🔍 [GRAPH DEBUG] ===== TradingAgentsGraph.propagate 接收参数 =====")
        logger.debug(f"🔍 [GRAPH DEBUG] 接收到的company_name: '{company_name}' (类型: {type(company_name)})")
        logger.debug(f"🔍 [GRAPH DEBUG] 接收到的trade_date: '{trade_date}' (类型: {type(trade_date)})")
        logger.debug(f"🔍 [GRAPH DEBUG] 接收到的task_id: '{task_id}'")

        # 保存股票代码到实例变量，供后续使用
        self.ticker = company_name
        logger.debug(f"🔍 [GRAPH DEBUG] 设置self.ticker: '{self.ticker}'")

        # 初始化分析状态
        logger.debug(f"🔍 [GRAPH DEBUG] 创建初始状态，传递参数: company_name='{company_name}', trade_date='{trade_date}'")
        init_agent_state = self.propagator.create_initial_state(
            company_name, trade_date
        )
        logger.debug(f"🔍 [GRAPH DEBUG] 初始状态中的company_of_interest: '{init_agent_state.get('company_of_interest', 'NOT_FOUND')}'")
        logger.debug(f"🔍 [GRAPH DEBUG] 初始状态中的trade_date: '{init_agent_state.get('trade_date', 'NOT_FOUND')}'")

        # 初始化性能计时器
        node_timings = {}  # 记录每个节点的执行时间
        total_start_time = time.time()  # 总体开始时间
        current_node_start = None  # 当前节点开始时间
        current_node_name = None  # 当前节点名称

        # 保存task_id用于后续保存性能数据
        self._current_task_id = task_id

        # 根据是否有进度回调选择不同的stream_mode
        # - 有进度回调：使用 "updates" 模式，可以获取节点级进度
        # - 无进度回调：使用 "values" 模式，获取完整状态
        args = self.propagator.get_graph_args(use_progress_callback=bool(progress_callback))

        if self.debug:
            # Debug mode with tracing and progress updates
            trace = []
            final_state = None
            for chunk in self.graph.stream(init_agent_state, **args):
                # 记录节点计时
                for node_name in chunk.keys():
                    if not node_name.startswith('__'):
                        # 如果有上一个节点，记录其结束时间
                        if current_node_name and current_node_start:
                            elapsed = time.time() - current_node_start
                            node_timings[current_node_name] = elapsed
                            logger.info(f"⏱️ [{current_node_name}] 耗时: {elapsed:.2f}秒")

                        # 开始新节点计时
                        current_node_name = node_name
                        current_node_start = time.time()
                        break

                # 在 updates 模式下，chunk 格式为 {node_name: state_update}
                # 在 values 模式下，chunk 格式为完整的状态
                if progress_callback and args.get("stream_mode") == "updates":
                    # updates 模式：chunk = {"Market Analyst": {...}}
                    self._send_progress_update(chunk, progress_callback)
                    # 累积状态更新
                    if final_state is None:
                        final_state = init_agent_state.copy()
                    for node_name, node_update in chunk.items():
                        if not node_name.startswith('__'):
                            final_state.update(node_update)
                else:
                    # values 模式：chunk = {"messages": [...], ...}
                    if len(chunk.get("messages", [])) > 0:
                        chunk["messages"][-1].pretty_print()
                    trace.append(chunk)
                    final_state = chunk

            if not trace and final_state:
                # updates 模式下，使用累积的状态
                pass
            elif trace:
                final_state = trace[-1]
        else:
            # Standard mode without tracing but with progress updates
            if progress_callback:
                # 使用 updates 模式以便获取节点级别的进度
                trace = []
                final_state = None
                for chunk in self.graph.stream(init_agent_state, **args):
                    # 记录节点计时
                    for node_name in chunk.keys():
                        if not node_name.startswith('__'):
                            # 如果有上一个节点，记录其结束时间
                            if current_node_name and current_node_start:
                                elapsed = time.time() - current_node_start
                                node_timings[current_node_name] = elapsed
                                logger.info(f"⏱️ [{current_node_name}] 耗时: {elapsed:.2f}秒")
                                logger.info(f"🔍 [TIMING] 节点切换: {current_node_name} → {node_name}")

                            # 开始新节点计时
                            current_node_name = node_name
                            current_node_start = time.time()
                            logger.info(f"🔍 [TIMING] 开始计时: {node_name}")
                            break

                    self._send_progress_update(chunk, progress_callback)
                    # 累积状态更新
                    if final_state is None:
                        final_state = init_agent_state.copy()
                    for node_name, node_update in chunk.items():
                        if not node_name.startswith('__'):
                            final_state.update(node_update)
            else:
                # 原有的invoke模式（也需要计时）
                logger.info("⏱️ 使用 invoke 模式执行分析（无进度回调）")
                # 使用stream模式以便计时，但不发送进度更新
                trace = []
                final_state = None
                for chunk in self.graph.stream(init_agent_state, **args):
                    # 记录节点计时
                    for node_name in chunk.keys():
                        if not node_name.startswith('__'):
                            # 如果有上一个节点，记录其结束时间
                            if current_node_name and current_node_start:
                                elapsed = time.time() - current_node_start
                                node_timings[current_node_name] = elapsed
                                logger.info(f"⏱️ [{current_node_name}] 耗时: {elapsed:.2f}秒")

                            # 开始新节点计时
                            current_node_name = node_name
                            current_node_start = time.time()
                            break

                    # 累积状态更新
                    if final_state is None:
                        final_state = init_agent_state.copy()
                    for node_name, node_update in chunk.items():
                        if not node_name.startswith('__'):
                            final_state.update(node_update)

        # 记录最后一个节点的时间
        if current_node_name and current_node_start:
            elapsed = time.time() - current_node_start
            node_timings[current_node_name] = elapsed
            logger.info(f"⏱️ [{current_node_name}] 耗时: {elapsed:.2f}秒")

        # 计算总时间
        total_elapsed = time.time() - total_start_time

        # 调试日志
        logger.info(f"🔍 [TIMING DEBUG] 节点计时数量: {len(node_timings)}")
        logger.info(f"🔍 [TIMING DEBUG] 总耗时: {total_elapsed:.2f}秒")
        logger.info(f"🔍 [TIMING DEBUG] 节点列表: {list(node_timings.keys())}")

        # 打印详细的时间统计
        logger.info("🔍 [TIMING DEBUG] 准备调用 _print_timing_summary")
        self._print_timing_summary(node_timings, total_elapsed)
        logger.info("🔍 [TIMING DEBUG] _print_timing_summary 调用完成")

        # 构建性能数据
        performance_data = self._build_performance_data(node_timings, total_elapsed)

        # 将性能数据添加到状态中
        final_state['performance_metrics'] = performance_data

        # Store current state for reflection
        self.curr_state = final_state

        # Log state
        self._log_state(trade_date, final_state)

        # 获取模型信息
        model_info = ""
        try:
            if hasattr(self.deep_thinking_llm, 'model_name'):
                model_info = f"{self.deep_thinking_llm.__class__.__name__}:{self.deep_thinking_llm.model_name}"
            else:
                model_info = self.deep_thinking_llm.__class__.__name__
        except Exception:
            model_info = "Unknown"

        # 处理决策并添加模型信息
        decision = self.process_signal(final_state["final_trade_decision"], company_name)
        decision['model_info'] = model_info

        # Return decision and processed signal
        return final_state, decision

    def _send_progress_update(self, chunk, progress_callback):
        """发送进度更新到回调函数
        
        这个方法负责将 LangGraph 的执行进度实时反馈给前端界面。
        它会解析每个节点的执行状态，并将其转换为友好的中文进度提示。
        
        LangGraph stream 返回的 chunk 格式：{node_name: {...}}
        节点名称示例：
        - "Market Analyst", "Fundamentals Analyst", "News Analyst", "Social Analyst"
        - "tools_market", "tools_fundamentals", "tools_news", "tools_social"  
        - "Msg Clear Market", "Msg Clear Fundamentals", etc.
        - "Bull Researcher", "Bear Researcher", "Research Manager"
        - "Trader"
        - "Risky Analyst", "Safe Analyst", "Neutral Analyst", "Risk Judge"
        
        Args:
            chunk: LangGraph 返回的执行块，包含当前节点的执行信息
            progress_callback: 进度回调函数，用于向前端发送进度更新
        """
        try:
            # 验证 chunk 格式是否为字典类型
            if not isinstance(chunk, dict):
                logger.warning(f"⚠️ [Progress] 无效的 chunk 格式: {type(chunk)}")
                return

            # 从 chunk 中提取当前执行的节点名称
            # LangGraph 的 chunk 格式通常是 {node_name: node_output}
            node_name = None
            for key in chunk.keys():
                # 跳过 LangGraph 的内部键（以 __ 开头）
                if not key.startswith('__'):
                    node_name = key
                    break

            # 如果找不到有效的节点名称，跳过处理
            if not node_name:
                logger.debug(f"🔍 [Progress] 未找到有效节点名称")
                return

            logger.info(f"🔍 [Progress] 检测到节点执行: {node_name}")

            # 检查是否为图执行结束节点
            if '__end__' in chunk:
                logger.info(f"📊 [Progress] 图执行完成，进入报告生成阶段")
                progress_callback("📊 生成报告")
                return

            # 节点名称到进度消息的映射表
            # 将技术性的节点名称转换为友好的中文提示
            node_mapping = {
                # 分析师团队 - 负责不同类型的市场分析
                'Market Analyst': "📊 市场分析师",           # 技术分析、价格走势
                'Fundamentals Analyst': "💼 基本面分析师",   # 财务数据、公司业绩
                'News Analyst': "📰 新闻分析师",             # 新闻事件影响
                'Social Analyst': "💬 社交媒体分析师",       # 社交媒体情绪
                
                # 工具调用节点 - 不发送进度更新，避免界面过于频繁刷新
                'tools_market': None,      # 市场数据获取工具
                'tools_fundamentals': None,  # 基本面数据工具
                'tools_news': None,        # 新闻数据工具
                'tools_social': None,      # 社交媒体数据工具
                
                # 消息清理节点 - 内部处理节点，不显示给用户
                'Msg Clear Market': None,      # 清理市场分析消息
                'Msg Clear Fundamentals': None,  # 清理基本面分析消息
                'Msg Clear News': None,        # 清理新闻分析消息
                'Msg Clear Social': None,      # 清理社交媒体分析消息
                
                # 研究员团队 - 进行多空观点辩论
                'Bull Researcher': "🐂 看涨研究员",      # 乐观观点分析
                'Bear Researcher': "🐻 看跌研究员",      # 悲观观点分析
                'Research Manager': "👔 研究经理",        # 综合研究观点
                
                # 交易决策节点 - 制定交易策略
                'Trader': "💼 交易员决策",  # 基于分析结果制定交易建议
                
                # 风险管理团队 - 评估交易风险
                'Risky Analyst': "🔥 激进风险评估",      # 高风险偏好评估
                'Safe Analyst': "🛡️ 保守风险评估",      # 低风险偏好评估
                'Neutral Analyst': "⚖️ 中性风险评估",    # 平衡风险评估
                'Risk Judge': "🎯 风险经理",             # 最终风险决策
            }

            # 根据节点名称查找对应的中文消息
            message = node_mapping.get(node_name)

            # 处理映射结果
            if message is None:
                # None 表示需要跳过的节点（工具节点、消息清理节点等内部节点）
                logger.debug(f"⏭️ [Progress] 跳过内部节点: {node_name}")
                return

            if message:
                # 发送友好的中文进度更新
                logger.info(f"📤 [Progress] 发送进度: {message}")
                progress_callback(message)
            else:
                # 未知节点，使用节点名称作为后备
                logger.warning(f"⚠️ [Progress] 未映射的节点: {node_name}")
                progress_callback(f"🔍 {node_name}")

        except Exception as e:
            # 捕获并记录任何异常，确保进度更新不会影响主流程
            logger.error(f"❌ 进度更新失败: {e}", exc_info=True)

    def _build_performance_data(self, node_timings: Dict[str, float], total_elapsed: float) -> Dict[str, Any]:
        """构建性能分析数据结构
        
        这个方法负责将节点执行时间数据组织成结构化的性能报告，
        包括总体统计、分类统计、团队效率分析等多个维度。
        
        Args:
            node_timings: 每个节点的执行时间字典，格式为 {node_name: elapsed_seconds}
            total_elapsed: 整个分析流程的总执行时间（秒）

        Returns:
            完整的性能数据字典，包含以下关键信息：
            - 总体统计：总时间、节点数量、平均时间
            - 极值分析：最快/最慢节点
            - 分类统计：按团队分类的时间和占比
            - LLM配置：使用的模型信息
        """
        # 按功能团队对节点进行分类
        # 注意：分类顺序很重要，风险管理节点要先判断，因为它们也包含'Analyst'
        analyst_nodes = {}      # 分析师团队（市场、基本面、新闻、社交）
        tool_nodes = {}         # 工具调用节点（数据获取）
        msg_clear_nodes = {}    # 消息清理节点（内部处理）
        research_nodes = {}     # 研究团队（多空辩论）
        trader_nodes = {}       # 交易团队（策略制定）
        risk_nodes = {}         # 风险管理团队（风险评估）
        other_nodes = {}        # 其他未分类节点

        # 遍历所有节点，按名称特征进行分类
        for node_name, elapsed in node_timings.items():
            # 1. 优先匹配风险管理团队（Risky/Safe/Neutral Analyst, Risk Judge）
            if 'Risky' in node_name or 'Safe' in node_name or 'Neutral' in node_name or 'Risk Judge' in node_name:
                risk_nodes[node_name] = elapsed
                
            # 2. 然后匹配分析师团队（Market/Fundamentals/News/Social Analyst）
            elif 'Analyst' in node_name:
                analyst_nodes[node_name] = elapsed
                
            # 3. 工具调用节点（以 tools_ 开头）
            elif node_name.startswith('tools_'):
                tool_nodes[node_name] = elapsed
                
            # 4. 消息清理节点（以 Msg Clear 开头）
            elif node_name.startswith('Msg Clear'):
                msg_clear_nodes[node_name] = elapsed
                
            # 5. 研究团队节点（包含 Researcher 或 Research Manager）
            elif 'Researcher' in node_name or 'Research Manager' in node_name:
                research_nodes[node_name] = elapsed
                
            # 6. 交易团队节点（包含 Trader）
            elif 'Trader' in node_name:
                trader_nodes[node_name] = elapsed
                
            # 7. 其他未分类节点
            else:
                other_nodes[node_name] = elapsed

        # 计算基础统计数据
        # 找出执行时间最长和最短的节点
        slowest_node = max(node_timings.items(), key=lambda x: x[1]) if node_timings else (None, 0)
        fastest_node = min(node_timings.items(), key=lambda x: x[1]) if node_timings else (None, 0)
        
        # 计算平均执行时间
        avg_time = sum(node_timings.values()) / len(node_timings) if node_timings else 0

        # 构建完整的性能数据结构
        return {
            # 总体统计信息
            "total_time": round(total_elapsed, 2),                    # 总耗时（秒）
            "total_time_minutes": round(total_elapsed / 60, 2),     # 总耗时（分钟）
            "node_count": len(node_timings),                        # 执行的节点总数
            "average_node_time": round(avg_time, 2),              # 节点平均耗时
            
            # 极值分析
            "slowest_node": {
                "name": slowest_node[0],                              # 最慢节点名称
                "time": round(slowest_node[1], 2)                   # 最慢节点耗时
            } if slowest_node[0] else None,
            "fastest_node": {
                "name": fastest_node[0],                              # 最快节点名称  
                "time": round(fastest_node[1], 2)                   # 最快节点耗时
            } if fastest_node[0] else None,
            
            # 详细节点时间记录
            "node_timings": {k: round(v, 2) for k, v in node_timings.items()},
            
            # 按团队分类的详细统计
            "category_timings": {
                # 分析师团队统计
                "analyst_team": {
                    "nodes": {k: round(v, 2) for k, v in analyst_nodes.items()},  # 各分析师节点时间
                    "total": round(sum(analyst_nodes.values()), 2),                # 分析师团队总时间
                    "percentage": round(sum(analyst_nodes.values()) / total_elapsed * 100, 1) if total_elapsed > 0 else 0  # 占总时间比例
                },
                
                # 工具调用统计（数据获取时间）
                "tool_calls": {
                    "nodes": {k: round(v, 2) for k, v in tool_nodes.items()},
                    "total": round(sum(tool_nodes.values()), 2),
                    "percentage": round(sum(tool_nodes.values()) / total_elapsed * 100, 1) if total_elapsed > 0 else 0
                },
                
                # 消息处理统计（内部处理时间）
                "message_clearing": {
                    "nodes": {k: round(v, 2) for k, v in msg_clear_nodes.items()},
                    "total": round(sum(msg_clear_nodes.values()), 2),
                    "percentage": round(sum(msg_clear_nodes.values()) / total_elapsed * 100, 1) if total_elapsed > 0 else 0
                },
                
                # 研究团队统计（多空辩论时间）
                "research_team": {
                    "nodes": {k: round(v, 2) for k, v in research_nodes.items()},
                    "total": round(sum(research_nodes.values()), 2),
                    "percentage": round(sum(research_nodes.values()) / total_elapsed * 100, 1) if total_elapsed > 0 else 0
                },
                
                # 交易团队统计（策略制定时间）
                "trader_team": {
                    "nodes": {k: round(v, 2) for k, v in trader_nodes.items()},
                    "total": round(sum(trader_nodes.values()), 2),
                    "percentage": round(sum(trader_nodes.values()) / total_elapsed * 100, 1) if total_elapsed > 0 else 0
                },
                
                # 风险管理团队统计（风险评估时间）
                "risk_management_team": {
                    "nodes": {k: round(v, 2) for k, v in risk_nodes.items()},
                    "total": round(sum(risk_nodes.values()), 2),
                    "percentage": round(sum(risk_nodes.values()) / total_elapsed * 100, 1) if total_elapsed > 0 else 0
                },
                
                # 其他未分类节点
                "other": {
                    "nodes": {k: round(v, 2) for k, v in other_nodes.items()},
                    "total": round(sum(other_nodes.values()), 2),
                    "percentage": round(sum(other_nodes.values()) / total_elapsed * 100, 1) if total_elapsed > 0 else 0
                }
            },
            
            # LLM 配置信息（用于分析不同模型的性能表现）
            "llm_config": {
                "provider": self.config.get('llm_provider', 'unknown'),           # LLM 供应商
                "deep_think_model": self.config.get('deep_think_llm', 'unknown'),  # 深度思考模型
                "quick_think_model": self.config.get('quick_think_llm', 'unknown') # 快速思考模型
            }
        }

    def _print_timing_summary(self, node_timings: Dict[str, float], total_elapsed: float):
        """打印详细的时间统计报告
        
        这个方法负责将节点执行时间数据以易读的格式打印到日志中，
        帮助开发者了解系统性能瓶颈和优化方向。
        
        功能特点：
        - 按团队分类统计（分析师、研究、交易、风险管理等）
        - 显示每个节点的执行时间和占比
        - 提供总体统计信息（总时间、平均时间、极值分析）
        - 显示当前使用的LLM配置
        
        Args:
            node_timings: 每个节点的执行时间字典，格式为 {node_name: elapsed_seconds}
            total_elapsed: 整个分析流程的总执行时间（秒）
        """
        # 调试日志：记录方法调用信息
        logger.info("🔍 [_print_timing_summary] 方法被调用")
        logger.info("🔍 [_print_timing_summary] node_timings 数量: " + str(len(node_timings)))
        logger.info("🔍 [_print_timing_summary] total_elapsed: " + str(total_elapsed))

        # 打印报告标题
        logger.info("=" * 80)
        logger.info("⏱️  分析性能统计报告")
        logger.info("=" * 80)

        # 按功能团队对节点进行分类（与 _build_performance_data 相同的分类逻辑）
        # 注意：分类顺序很重要，风险管理节点要先判断，因为它们也包含'Analyst'
        analyst_nodes = []      # 分析师团队（市场、基本面、新闻、社交）
        tool_nodes = []         # 工具调用节点（数据获取）
        msg_clear_nodes = []    # 消息清理节点（内部处理）
        research_nodes = []     # 研究团队（多空辩论）
        trader_nodes = []       # 交易团队（策略制定）
        risk_nodes = []         # 风险管理团队（风险评估）
        other_nodes = []        # 其他未分类节点

        # 遍历所有节点，按名称特征进行分类
        for node_name, elapsed in node_timings.items():
            # 1. 优先匹配风险管理团队（Risky/Safe/Neutral Analyst, Risk Judge）
            if 'Risky' in node_name or 'Safe' in node_name or 'Neutral' in node_name or 'Risk Judge' in node_name:
                risk_nodes.append((node_name, elapsed))
                
            # 2. 然后匹配分析师团队（Market/Fundamentals/News/Social Analyst）
            elif 'Analyst' in node_name:
                analyst_nodes.append((node_name, elapsed))
                
            # 3. 工具调用节点（以 tools_ 开头）
            elif node_name.startswith('tools_'):
                tool_nodes.append((node_name, elapsed))
                
            # 4. 消息清理节点（以 Msg Clear 开头）
            elif node_name.startswith('Msg Clear'):
                msg_clear_nodes.append((node_name, elapsed))
                
            # 5. 研究团队节点（包含 Researcher 或 Research Manager）
            elif 'Researcher' in node_name or 'Research Manager' in node_name:
                research_nodes.append((node_name, elapsed))
                
            # 6. 交易团队节点（包含 Trader）
            elif 'Trader' in node_name:
                trader_nodes.append((node_name, elapsed))
                
            # 7. 其他未分类节点
            else:
                other_nodes.append((node_name, elapsed))

        # 定义分类统计打印函数
        def print_category(title: str, nodes: List[Tuple[str, float]]):
            """打印单个分类的统计信息"""
            if not nodes:
                return
            logger.info(f"\n📊 {title}")
            logger.info("-" * 80)
            total_category_time = sum(t for _, t in nodes)
            
            # 按耗时从长到短排序，便于识别性能瓶颈
            for node_name, elapsed in sorted(nodes, key=lambda x: x[1], reverse=True):
                percentage = (elapsed / total_elapsed * 100) if total_elapsed > 0 else 0
                logger.info(f"  • {node_name:40s} {elapsed:8.2f}秒  ({percentage:5.1f}%)")
            
            # 打印分类小计
            logger.info(f"  {'小计':40s} {total_category_time:8.2f}秒  ({total_category_time/total_elapsed*100:5.1f}%)")

        # 打印各团队分类统计
        print_category("分析师团队", analyst_nodes)
        print_category("工具调用", tool_nodes)
        print_category("消息清理", msg_clear_nodes)
        print_category("研究团队", research_nodes)
        print_category("交易团队", trader_nodes)
        print_category("风险管理团队", risk_nodes)
        print_category("其他节点", other_nodes)

        # 打印总体统计信息
        logger.info("\n" + "=" * 80)
        logger.info(f"🎯 总执行时间: {total_elapsed:.2f}秒 ({total_elapsed/60:.2f}分钟)")
        logger.info(f"📈 节点总数: {len(node_timings)}")
        
        if node_timings:
            # 计算基础统计数据
            avg_time = sum(node_timings.values()) / len(node_timings)
            slowest_node = max(node_timings.items(), key=lambda x: x[1])
            fastest_node = min(node_timings.items(), key=lambda x: x[1])
            
            logger.info(f"⏱️  平均节点耗时: {avg_time:.2f}秒")
            logger.info(f"🐌 最慢节点: {slowest_node[0]} ({slowest_node[1]:.2f}秒)")
            logger.info(f"⚡ 最快节点: {fastest_node[0]} ({fastest_node[1]:.2f}秒)")

        # 打印当前LLM配置信息（用于性能对比分析）
        logger.info(f"\n🤖 LLM配置:")
        logger.info(f"  • 提供商: {self.config.get('llm_provider', 'unknown')}")
        logger.info(f"  • 深度思考模型: {self.config.get('deep_think_llm', 'unknown')}")
        logger.info(f"  • 快速思考模型: {self.config.get('quick_think_llm', 'unknown')}")
        logger.info("=" * 80)

    def _log_state(self, trade_date, final_state):
        """将最终分析状态保存到JSON日志文件中
        
        这个方法负责将完整的分析结果保存到本地文件系统，便于后续的回测分析、
        性能评估和决策质量审查。日志文件采用结构化格式，包含所有关键分析结果。
        
        保存的数据结构：
        - 基础信息：公司名称、分析日期
        - 分析报告：市场、情绪、新闻、基本面四大分析报告
        - 辩论历史：投资辩论（多空观点）和风险辩论的完整历史
        - 决策结果：交易员投资计划、最终投资方案、交易决策
        - 时间戳：按日期索引，便于历史查询
        
        Args:
            trade_date (str): 交易日期，作为日志的主键
            final_state (dict): 完整的最终分析状态，包含所有智能体的分析结果
        """
        # 构建完整的分析结果数据结构
        self.log_states_dict[str(trade_date)] = {
            # 基础信息
            "company_of_interest": final_state["company_of_interest"],
            "trade_date": final_state["trade_date"],
            
            # 四大分析报告
            "market_report": final_state["market_report"],
            "sentiment_report": final_state["sentiment_report"],
            "news_report": final_state["news_report"],
            "fundamentals_report": final_state["fundamentals_report"],
            
            # 投资辩论历史（多空观点辩论）
            "investment_debate_state": {
                "bull_history": final_state["investment_debate_state"]["bull_history"],
                "bear_history": final_state["investment_debate_state"]["bear_history"],
                "history": final_state["investment_debate_state"]["history"],
                "current_response": final_state["investment_debate_state"][
                    "current_response"
                ],
                "judge_decision": final_state["investment_debate_state"][
                    "judge_decision"
                ],
            },
            
            # 交易员的投资决策
            "trader_investment_decision": final_state["trader_investment_plan"],
            
            # 风险辩论历史（风险评估辩论）
            "risk_debate_state": {
                "risky_history": final_state["risk_debate_state"]["risky_history"],
                "safe_history": final_state["risk_debate_state"]["safe_history"],
                "neutral_history": final_state["risk_debate_state"]["neutral_history"],
                "history": final_state["risk_debate_state"]["history"],
                "judge_decision": final_state["risk_debate_state"]["judge_decision"],
            },
            
            # 最终投资方案
            "investment_plan": final_state["investment_plan"],
            "final_trade_decision": final_state["final_trade_decision"],
        }

        # 创建日志文件保存目录
        # 目录结构：eval_results/{股票代码}/TradingAgentsStrategy_logs/
        directory = Path(f"eval_results/{self.ticker}/TradingAgentsStrategy_logs/")
        directory.mkdir(parents=True, exist_ok=True)

        # 将完整的分析历史保存到JSON文件
        # 文件路径：eval_results/{股票代码}/TradingAgentsStrategy_logs/full_states_log.json
        log_file_path = f"eval_results/{self.ticker}/TradingAgentsStrategy_logs/full_states_log.json"
        
        with open(log_file_path, "w", encoding='utf-8') as f:
            json.dump(self.log_states_dict, f, indent=4, ensure_ascii=False)
            
        logger.info(f"📊 分析结果已保存到: {log_file_path}")
        logger.info(f"📊 保存了 {len(self.log_states_dict)} 天的分析历史")

    def reflect_and_remember(self, returns_losses):
        """基于实际收益结果进行决策反思和记忆更新
        
        这个方法实现了系统的学习能力，通过对比分析结果与实际市场收益，
        让各个智能体从过去的决策中学习，改进未来的分析质量。
        
        反思过程：
        1. 对比分析预测与实际市场收益表现
        2. 识别决策中的成功经验和失败教训
        3. 更新各智能体的记忆，调整分析权重
        4. 优化未来分析的逻辑和参数
        
        涉及的智能体：
        - 多头研究员：反思看涨观点的准确性
        - 空头研究员：反思看跌观点的准确性  
        - 交易员：反思投资策略的有效性
        - 投资法官：反思投资决策的质量
        - 风险经理：反思风险评估的准确性
        
        Args:
            returns_losses: 实际的市场收益/损失数据，用于评估决策质量
        """
        # 让各智能体基于实际结果进行反思和学习
        logger.info("🧠 开始决策反思和记忆更新过程...")
        
        # 多头研究员反思：看涨观点是否准确
        logger.info("🐂 多头研究员反思看涨观点...")
        self.reflector.reflect_bull_researcher(
            self.curr_state, returns_losses, self.bull_memory
        )
        
        # 空头研究员反思：看跌观点是否准确
        logger.info("🐻 空头研究员反思看跌观点...")
        self.reflector.reflect_bear_researcher(
            self.curr_state, returns_losses, self.bear_memory
        )
        
        # 交易员反思：投资策略是否有效
        logger.info("💼 交易员反思投资策略...")
        self.reflector.reflect_trader(
            self.curr_state, returns_losses, self.trader_memory
        )
        
        # 投资法官反思：投资决策质量
        logger.info("⚖️ 投资法官反思决策质量...")
        self.reflector.reflect_invest_judge(
            self.curr_state, returns_losses, self.invest_judge_memory
        )
        
        # 风险经理反思：风险评估准确性
        logger.info("🛡️ 风险经理反思风险评估...")
        self.reflector.reflect_risk_manager(
            self.curr_state, returns_losses, self.risk_manager_memory
        )
        
        logger.info("✅ 决策反思和记忆更新完成")

    def process_signal(self, full_signal, stock_symbol=None):
        """处理分析信号，提取核心的投资决策
        
        这个方法作为信号处理器的外观接口，将复杂的分析结果信号
        转换为简洁的投资决策输出。
        
        信号处理过程：
        1. 接收完整的分析信号（包含所有智能体的分析结果）
        2. 调用信号处理器提取关键决策信息
        3. 返回结构化的投资决策数据
        
        Args:
            full_signal (dict): 完整的分析信号，包含所有智能体的分析结果和决策
            stock_symbol (str, optional): 股票代码，用于信号上下文化
            
        Returns:
            dict: 处理后的投资决策信号，包含核心决策信息
            
        示例返回格式：
        {
            "decision": "buy/hold/sell",
            "confidence": 0.8,
            "reasoning": "分析理由...",
            "risk_level": "low/medium/high"
        }
        """
        logger.info("📡 开始处理分析信号...")
        logger.info(f"📡 股票代码: {stock_symbol or '未指定'}")
        
        # 调用信号处理器进行信号提取
        processed_signal = self.signal_processor.process_signal(full_signal, stock_symbol)
        
        logger.info(f"📡 信号处理完成")
        logger.info(f"📡 决策结果: {processed_signal.get('decision', 'unknown')}")
        logger.info(f"📡 置信度: {processed_signal.get('confidence', 0):.2f}")
        
        return processed_signal
