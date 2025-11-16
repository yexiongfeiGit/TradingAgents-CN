#!/usr/bin/env python3
"""
TradingAgents 数据流模块初始化文件

提供统一的数据获取接口，包括股票数据、新闻数据、技术指标、财务报表等。

主要功能：
1. 多数据源支持：Finnhub、Yahoo Finance、Tushare、AKShare等
2. 新闻和情感分析：Google News、Reddit、Finnhub新闻
3. 技术指标计算：基于stockstats的技术分析
4. 财务报表获取：SimFin数据源
5. 中国市场数据：统一的中国股票数据接口
6. 香港市场数据：港股数据获取接口

模块结构：
- interface.py：统一数据接口定义
- providers/：数据提供商实现
  - us.py：美股数据（Finnhub、Yahoo Finance）
  - china.py：中国股票数据（Tushare、AKShare）
  - hk.py：香港股票数据
- news/：新闻数据模块
  - google_news.py：Google新闻
  - reddit.py：Reddit新闻
- technical/：技术指标模块
  - stockstats.py：基于stockstats的技术分析
- finnhub_utils.py：Finnhub工具函数（向后兼容）
- yfin_utils.py：Yahoo Finance工具函数（向后兼容）

数据源支持：
- Finnhub：美股数据、新闻、内部人交易
- Yahoo Finance：美股数据、历史价格
- Tushare：中国A股数据、基本面数据
- AKShare：中国股票数据备选源
- SimFin：财务报表数据
- Google News：全球新闻
- Reddit：社交媒体新闻

使用示例：
    from tradingagents.dataflows import (
        get_YFin_data, get_finnhub_news, 
        get_china_stock_data_unified, get_stockstats_indicator
    )
    
    # 获取美股数据
    stock_data = get_YFin_data("AAPL", "2024-01-01", "2024-12-31")
    
    # 获取新闻数据
    news = get_finnhub_news("AAPL")
    
    # 获取中国股票数据
    china_data = get_china_stock_data_unified("000001.SZ")

向后兼容性：
- 支持从旧路径导入（finnhub_utils、yfin_utils等）
- 提供兼容性警告和自动迁移
- 保持API接口一致性

错误处理：
- 模块导入失败时提供详细警告
- 数据源不可用时返回None或空数据
- 记录详细的错误日志便于调试

性能优化：
- 懒加载导入减少启动时间
- 缓存机制避免重复请求
- 连接池管理优化网络性能

作者：TradingAgents-CN团队
版本：2.0.0
创建时间：2024-01-01
"""

# 导入基础模块
# Finnhub 工具（支持新旧路径）
try:
    from .providers.us import get_data_in_range
except ImportError:
    try:
        from .finnhub_utils import get_data_in_range
    except ImportError:
        get_data_in_range = None

# 导入新闻模块（新路径）
try:
    from .news import getNewsData, fetch_top_from_category
except ImportError:
    # 向后兼容：尝试从旧路径导入
    try:
        from .news.google_news import getNewsData
    except ImportError:
        getNewsData = None
    try:
        from .news.reddit import fetch_top_from_category
    except ImportError:
        fetch_top_from_category = None

# 导入日志模块
from tradingagents.utils.logging_manager import get_logger
logger = get_logger('agents')

# 尝试导入yfinance相关模块（支持新旧路径）
try:
    from .providers.us import YFinanceUtils, YFINANCE_AVAILABLE
except ImportError:
    try:
        from .yfin_utils import YFinanceUtils
        YFINANCE_AVAILABLE = True
    except ImportError as e:
        logger.warning(f"⚠️ yfinance模块不可用: {e}")
        YFinanceUtils = None
        YFINANCE_AVAILABLE = False

# 导入技术指标模块（新路径）
try:
    from .technical import StockstatsUtils, STOCKSTATS_AVAILABLE
except ImportError as e:
    # 向后兼容：尝试从旧路径导入
    try:
        from .technical.stockstats import StockstatsUtils
        STOCKSTATS_AVAILABLE = True
    except ImportError as e:
        logger.warning(f"⚠️ stockstats模块不可用: {e}")
        StockstatsUtils = None
        STOCKSTATS_AVAILABLE = False

from .interface import (

    # News and sentiment functions
    get_finnhub_news,
    get_finnhub_company_insider_sentiment,
    get_finnhub_company_insider_transactions,
    get_google_news,
    get_reddit_global_news,
    get_reddit_company_news,
    # Financial statements functions
    get_simfin_balance_sheet,
    get_simfin_cashflow,
    get_simfin_income_statements,
    # Technical analysis functions
    get_stock_stats_indicators_window,
    get_stockstats_indicator,
    # Market data functions
    get_YFin_data_window,
    get_YFin_data,
    # Tushare data functions
    get_china_stock_data_tushare,
    get_china_stock_fundamentals_tushare,
    # Unified China data functions (recommended)
    get_china_stock_data_unified,
    get_china_stock_info_unified,
    switch_china_data_source,
    get_current_china_data_source,
    # Hong Kong stock functions
    get_hk_stock_data_unified,
    get_hk_stock_info_unified,
    get_stock_data_by_market,
)

__all__ = [
    # News and sentiment functions
    "get_finnhub_news",
    "get_finnhub_company_insider_sentiment",
    "get_finnhub_company_insider_transactions",
    "get_google_news",
    "get_reddit_global_news",
    "get_reddit_company_news",
    # Financial statements functions
    "get_simfin_balance_sheet",
    "get_simfin_cashflow",
    "get_simfin_income_statements",
    # Technical analysis functions
    "get_stock_stats_indicators_window",
    "get_stockstats_indicator",
    # Market data functions
    "get_YFin_data_window",
    "get_YFin_data",
    # Tushare data functions
    "get_china_stock_data_tushare",
    "get_china_stock_fundamentals_tushare",
    # Unified China data functions
    "get_china_stock_data_unified",
    "get_china_stock_info_unified",
    "switch_china_data_source",
    "get_current_china_data_source",
    # Hong Kong stock functions
    "get_hk_stock_data_unified",
    "get_hk_stock_info_unified",
    "get_stock_data_by_market",
]
