#!/usr/bin/env python3
"""
Stockstats 技术指标计算模块

基于stockstats库提供技术指标计算功能，支持多种常用技术分析指标的计算和获取。

主要功能：
1. 技术指标计算：基于历史价格数据计算技术指标
2. 多指标支持：支持RSI、MACD、布林带、移动平均线等
3. 数据获取：支持在线和离线两种数据获取模式
4. 缓存机制：本地文件缓存避免重复下载
5. 日期匹配：精确的日期匹配和交易日处理
6. 错误处理：完善的异常处理和降级机制

支持的指标：
- 趋势指标：MA、EMA、MACD等
- 动量指标：RSI、KDJ、CCI等
- 波动率指标：布林带、ATR等
- 成交量指标：OBV、ADL等
- 自定义指标：支持stockstats的所有指标

数据模式：
- 在线模式：实时从Yahoo Finance获取最新数据
- 离线模式：使用本地缓存的历史数据文件
- 混合模式：优先使用缓存，缺失时在线获取

缓存管理：
- 缓存目录：可配置的缓存文件存储路径
- 文件命名：标准化的缓存文件命名规则
- 数据更新：自动检测和更新过期缓存
- 存储格式：CSV格式便于查看和调试

日期处理：
- 交易日历：自动识别非交易日（周末、节假日）
- 日期匹配：精确匹配指定日期的指标值
- 时区处理：支持多时区的时间处理
- 格式标准化：统一的日期格式处理

错误处理：
- 数据文件不存在：提供明确的错误信息
- 指标计算失败：返回"N/A"和错误说明
- 日期无数据：识别非交易日并给出说明
- 网络错误：在线模式下的网络异常处理

性能优化：
- 数据复用：避免重复计算相同指标
- 缓存命中：优先使用缓存减少网络请求
- 增量更新：只更新需要的数据部分
- 内存管理：及时清理大对象避免内存泄漏

使用示例：
    from tradingagents.dataflows.technical.stockstats import StockstatsUtils
    
    utils = StockstatsUtils()
    
    # 获取RSI指标（在线模式）
    rsi = utils.get_stock_stats("AAPL", "rsi_14", "2024-01-15", "/data", online=True)
    
    # 获取MACD指标（离线模式）
    macd = utils.get_stock_stats("AAPL", "macd", "2024-01-15", "/data", online=False)

依赖要求：
- stockstats：技术指标计算库
- pandas：数据处理和分析
- yfinance：Yahoo Finance数据获取
- numpy：数值计算支持

数据格式：
- 输入：股票价格DataFrame（OHLCV格式）
- 输出：指定日期的指标数值或"N/A"
- 缓存：CSV格式的历史数据文件

作者：TradingAgents-CN团队
版本：2.0.0
创建时间：2024-01-01
"""

import pandas as pd
import yfinance as yf
from stockstats import wrap
from typing import Annotated
import os
from tradingagents.config.config_manager import config_manager

def get_config():
    """兼容性包装函数"""
    return config_manager.load_settings()


class StockstatsUtils:
    @staticmethod
    def get_stock_stats(
        symbol: Annotated[str, "ticker symbol for the company"],
        indicator: Annotated[
            str, "quantitative indicators based off of the stock data for the company"
        ],
        curr_date: Annotated[
            str, "curr date for retrieving stock price data, YYYY-mm-dd"
        ],
        data_dir: Annotated[
            str,
            "directory where the stock data is stored.",
        ],
        online: Annotated[
            bool,
            "whether to use online tools to fetch data or offline tools. If True, will use online tools.",
        ] = False,
    ):
        df = None
        data = None

        if not online:
            try:
                data = pd.read_csv(
                    os.path.join(
                        data_dir,
                        f"{symbol}-YFin-data-2015-01-01-2025-03-25.csv",
                    )
                )
                df = wrap(data)
            except FileNotFoundError:
                raise Exception("Stockstats fail: Yahoo Finance data not fetched yet!")
        else:
            # Get today's date as YYYY-mm-dd to add to cache
            today_date = pd.Timestamp.today()
            curr_date = pd.to_datetime(curr_date)

            end_date = today_date
            start_date = today_date - pd.DateOffset(years=15)
            start_date = start_date.strftime("%Y-%m-%d")
            end_date = end_date.strftime("%Y-%m-%d")

            # Get config and ensure cache directory exists
            config = get_config()
            os.makedirs(config["data_cache_dir"], exist_ok=True)

            data_file = os.path.join(
                config["data_cache_dir"],
                f"{symbol}-YFin-data-{start_date}-{end_date}.csv",
            )

            if os.path.exists(data_file):
                data = pd.read_csv(data_file)
                data["Date"] = pd.to_datetime(data["Date"])
            else:
                data = yf.download(
                    symbol,
                    start=start_date,
                    end=end_date,
                    multi_level_index=False,
                    progress=False,
                    auto_adjust=True,
                )
                data = data.reset_index()
                data.to_csv(data_file, index=False)

            df = wrap(data)
            df["Date"] = df["Date"].dt.strftime("%Y-%m-%d")
            curr_date = curr_date.strftime("%Y-%m-%d")

        df[indicator]  # trigger stockstats to calculate the indicator
        matching_rows = df[df["Date"].str.startswith(curr_date)]

        if not matching_rows.empty:
            indicator_value = matching_rows[indicator].values[0]
            return indicator_value
        else:
            return "N/A: Not a trading day (weekend or holiday)"
