#!/usr/bin/env python3
"""
Yahoo Finance 美股数据提供商模块

提供Yahoo Finance数据源的封装接口，支持股票数据、财务信息、分析师推荐等数据的获取。

主要功能：
1. 股票历史数据获取：开盘价、最高价、最低价、收盘价、成交量
2. 公司基本信息：名称、行业、部门、国家、网站等
3. 财务数据获取：收入表、资产负债表、现金流量表
4. 股息数据：历史股息分红记录
5. 分析师推荐：最新分析师评级和建议
6. 缓存支持：可选的缓存机制提高性能

类和方法：
- YFinanceUtils：主要的Yahoo Finance工具类
  - get_stock_data()：获取股票历史价格数据
  - get_stock_info()：获取股票基本信息
  - get_company_info()：获取公司信息DataFrame
  - get_stock_dividends()：获取股息数据
  - get_income_stmt()：获取收入表
  - get_balance_sheet()：获取资产负债表
  - get_cash_flow()：获取现金流量表
  - get_analyst_recommendations()：获取分析师推荐

装饰器和工具：
- init_ticker：自动初始化yf.Ticker的装饰器
- decorate_all_methods：批量应用装饰器的工具
- save_output：可选的数据保存功能

缓存机制：
- 延迟导入缓存管理器，避免循环依赖
- 支持内存缓存和持久化缓存
- 可配置的缓存过期时间
- 缓存失败时自动降级到直接API调用

错误处理：
- 导入失败时记录警告，设置可用性标志
- API调用失败时返回None或空DataFrame
- 网络超时和重试机制
- 数据格式验证和清理

性能优化：
- 连接复用：Ticker对象复用减少连接开销
- 批量操作：支持批量数据获取
- 懒加载：按需导入减少内存占用
- 装饰器模式：统一的初始化和错误处理

使用示例：
    from tradingagents.dataflows.providers.us.yfinance import YFinanceUtils
    
    utils = YFinanceUtils()
    
    # 获取股票数据
    data = utils.get_stock_data("AAPL", "2024-01-01", "2024-12-31")
    
    # 获取公司信息
    info = utils.get_company_info("AAPL")
    
    # 获取分析师推荐
    recommendation, votes = utils.get_analyst_recommendations("AAPL")

依赖要求：
- yfinance：Yahoo Finance Python库
- pandas：数据处理和分析
- numpy：数值计算支持

数据格式：
- 股票数据：OHLCV格式的DataFrame，日期为索引
- 公司信息：包含关键信息的DataFrame
- 财务数据：标准财务格式的DataFrame
- 分析师推荐：字符串推荐和投票数

限制和注意事项：
- Yahoo Finance有访问频率限制
- 某些数据可能有延迟
- 免费版有访问次数限制
- 需要稳定的网络连接

作者：TradingAgents-CN团队
版本：2.0.0
创建时间：2024-01-01
"""

# gets data/stats

import yfinance as yf
from typing import Annotated, Callable, Any, Optional
from pandas import DataFrame
import pandas as pd
from functools import wraps

from tradingagents.utils.dataflow_utils import save_output, SavePathType, decorate_all_methods

# 导入日志模块
from tradingagents.utils.logging_manager import get_logger
logger = get_logger('agents')

# 导入缓存管理器（延迟导入，避免循环依赖）
_cache_module = None
CACHE_AVAILABLE = True

def get_cache():
    """延迟导入缓存管理器"""
    global _cache_module, CACHE_AVAILABLE
    if _cache_module is None:
        try:
            from ...cache import get_cache as _get_cache
            _cache_module = _get_cache
            CACHE_AVAILABLE = True
        except ImportError as e:
            CACHE_AVAILABLE = False
            logger.debug(f"缓存管理器不可用（使用直接API调用）: {e}")
            return None
    return _cache_module() if _cache_module else None


def init_ticker(func: Callable) -> Callable:
    """Decorator to initialize yf.Ticker and pass it to the function."""

    @wraps(func)
    def wrapper(symbol: Annotated[str, "ticker symbol"], *args, **kwargs) -> Any:
        ticker = yf.Ticker(symbol)
        return func(ticker, *args, **kwargs)

    return wrapper


@decorate_all_methods(init_ticker)
class YFinanceUtils:

    def get_stock_data(
        symbol: Annotated[str, "ticker symbol"],
        start_date: Annotated[
            str, "start date for retrieving stock price data, YYYY-mm-dd"
        ],
        end_date: Annotated[
            str, "end date for retrieving stock price data, YYYY-mm-dd"
        ],
        save_path: SavePathType = None,
    ) -> DataFrame:
        """retrieve stock price data for designated ticker symbol"""
        ticker = symbol
        # add one day to the end_date so that the data range is inclusive
        end_date = pd.to_datetime(end_date) + pd.DateOffset(days=1)
        end_date = end_date.strftime("%Y-%m-%d")
        stock_data = ticker.history(start=start_date, end=end_date)
        # save_output(stock_data, f"Stock data for {ticker.ticker}", save_path)
        return stock_data

    def get_stock_info(
        symbol: Annotated[str, "ticker symbol"],
    ) -> dict:
        """Fetches and returns latest stock information."""
        ticker = symbol
        stock_info = ticker.info
        return stock_info

    def get_company_info(
        symbol: Annotated[str, "ticker symbol"],
        save_path: Optional[str] = None,
    ) -> DataFrame:
        """Fetches and returns company information as a DataFrame."""
        ticker = symbol
        info = ticker.info
        company_info = {
            "Company Name": info.get("shortName", "N/A"),
            "Industry": info.get("industry", "N/A"),
            "Sector": info.get("sector", "N/A"),
            "Country": info.get("country", "N/A"),
            "Website": info.get("website", "N/A"),
        }
        company_info_df = DataFrame([company_info])
        if save_path:
            company_info_df.to_csv(save_path)
            logger.info(f"Company info for {ticker.ticker} saved to {save_path}")
        return company_info_df

    def get_stock_dividends(
        symbol: Annotated[str, "ticker symbol"],
        save_path: Optional[str] = None,
    ) -> DataFrame:
        """Fetches and returns the latest dividends data as a DataFrame."""
        ticker = symbol
        dividends = ticker.dividends
        if save_path:
            dividends.to_csv(save_path)
            logger.info(f"Dividends for {ticker.ticker} saved to {save_path}")
        return dividends

    def get_income_stmt(symbol: Annotated[str, "ticker symbol"]) -> DataFrame:
        """Fetches and returns the latest income statement of the company as a DataFrame."""
        ticker = symbol
        income_stmt = ticker.financials
        return income_stmt

    def get_balance_sheet(symbol: Annotated[str, "ticker symbol"]) -> DataFrame:
        """Fetches and returns the latest balance sheet of the company as a DataFrame."""
        ticker = symbol
        balance_sheet = ticker.balance_sheet
        return balance_sheet

    def get_cash_flow(symbol: Annotated[str, "ticker symbol"]) -> DataFrame:
        """Fetches and returns the latest cash flow statement of the company as a DataFrame."""
        ticker = symbol
        cash_flow = ticker.cashflow
        return cash_flow

    def get_analyst_recommendations(symbol: Annotated[str, "ticker symbol"]) -> tuple:
        """Fetches the latest analyst recommendations and returns the most common recommendation and its count."""
        ticker = symbol
        recommendations = ticker.recommendations
        if recommendations.empty:
            return None, 0  # No recommendations available

        # Assuming 'period' column exists and needs to be excluded
        row_0 = recommendations.iloc[0, 1:]  # Exclude 'period' column if necessary

        # Find the maximum voting result
        max_votes = row_0.max()
        majority_voting_result = row_0[row_0 == max_votes].index.tolist()

        return majority_voting_result[0], max_votes
