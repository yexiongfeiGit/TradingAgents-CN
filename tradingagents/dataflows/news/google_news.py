#!/usr/bin/env python3
"""
Google 新闻数据获取模块

提供Google新闻搜索结果的爬取功能，支持按关键词和日期范围获取新闻数据。

主要功能：
1. 新闻搜索：基于关键词和日期范围的新闻搜索
2. 结果解析：解析Google新闻搜索结果页面
3. 数据提取：提取标题、摘要、链接、日期、来源等信息
4. 分页支持：支持多页新闻结果的获取
5. 错误重试：智能的重试机制和错误处理
6. 反爬虫策略：随机的请求延迟和用户代理伪装

请求参数：
- query：搜索关键词（如公司名称、股票代码等）
- start_date：开始日期（YYYY-MM-DD或MM/DD/YYYY格式）
- end_date：结束日期（YYYY-MM-DD或MM/DD/YYYY格式）

返回数据：
- 链接：新闻原文链接
- 标题：新闻标题
- 摘要：新闻内容摘要
- 日期：新闻发布日期
- 来源：新闻来源网站

反爬虫策略：
- 随机延迟：2-6秒的随机请求间隔
- 用户代理：模拟真实浏览器的User-Agent
- 超时设置：连接超时10秒，读取超时30秒
- 重试机制：指数退避的重试策略

错误处理：
- 连接超时：最多重试3次后跳过当前页
- 连接错误：最多重试3次后跳过当前页
- 解析错误：单个结果解析失败时跳过继续处理
- 频率限制：检测到429状态码时自动重试

性能优化：
- 分页处理：逐页获取避免一次性大量请求
- 智能停止：检测到无结果或没有下一页时停止
- 异常恢复：部分页面失败时继续获取其他页面
- 日志记录：详细的请求和错误日志

配置参数：
- TA_GOOGLE_NEWS_SLEEP_MIN_SECONDS：最小延迟时间（默认2秒）
- TA_GOOGLE_NEWS_SLEEP_MAX_SECONDS：最大延迟时间（默认6秒）

使用示例：
    from tradingagents.dataflows.news.google_news import getNewsData
    
    # 获取苹果公司的新闻
    news = getNewsData("Apple Inc", "2024-01-01", "2024-01-31")
    
    for article in news:
        print(f"标题: {article['title']}")
        print(f"来源: {article['source']}")
        print(f"日期: {article['date']}")
        print(f"摘要: {article['snippet']}")
        print("-" * 50)

依赖要求：
- requests：HTTP请求库
- beautifulsoup4：HTML解析库
- tenacity：重试机制库

限制和注意事项：
- Google可能有访问频率限制
- 过多的请求可能导致IP被封
- 搜索结果可能受Google算法影响
- 需要稳定的网络连接

数据质量：
- 结果完整性：取决于Google新闻的收录情况
- 实时性：可能存在一定的延迟
- 准确性：自动提取可能存在解析误差
- 覆盖范围：主要覆盖主流媒体的新闻

作者：TradingAgents-CN团队
版本：2.0.0
创建时间：2024-01-01
"""

import json
import requests
from bs4 import BeautifulSoup
from datetime import datetime
import time
import random
import os
from tenacity import (
    retry,
    stop_after_attempt,
    wait_exponential,
    retry_if_exception_type,
    retry_if_result,
)

from tradingagents.config.runtime_settings import get_float
# 导入日志模块
from tradingagents.utils.logging_manager import get_logger
logger = get_logger('agents')

SLEEP_MIN = get_float("TA_GOOGLE_NEWS_SLEEP_MIN_SECONDS", "ta_google_news_sleep_min_seconds", 2.0)
SLEEP_MAX = get_float("TA_GOOGLE_NEWS_SLEEP_MAX_SECONDS", "ta_google_news_sleep_max_seconds", 6.0)


def is_rate_limited(response):
    """Check if the response indicates rate limiting (status code 429)"""
    return response.status_code == 429


@retry(
    retry=(retry_if_result(is_rate_limited) | retry_if_exception_type(requests.exceptions.ConnectionError) | retry_if_exception_type(requests.exceptions.Timeout)),
    wait=wait_exponential(multiplier=1, min=4, max=60),
    stop=stop_after_attempt(5),
)
def make_request(url, headers):
    """Make a request with retry logic for rate limiting and connection issues"""
    # Random delay before each request to avoid detection
    time.sleep(random.uniform(SLEEP_MIN, SLEEP_MAX))
    # 添加超时参数，设置连接超时和读取超时
    response = requests.get(url, headers=headers, timeout=(10, 30))  # 连接超时10秒，读取超时30秒
    return response


def getNewsData(query, start_date, end_date):
    """
    Scrape Google News search results for a given query and date range.
    query: str - search query
    start_date: str - start date in the format yyyy-mm-dd or mm/dd/yyyy
    end_date: str - end date in the format yyyy-mm-dd or mm/dd/yyyy
    """
    if "-" in start_date:
        start_date = datetime.strptime(start_date, "%Y-%m-%d")
        start_date = start_date.strftime("%m/%d/%Y")
    if "-" in end_date:
        end_date = datetime.strptime(end_date, "%Y-%m-%d")
        end_date = end_date.strftime("%m/%d/%Y")

    headers = {
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/101.0.4951.54 Safari/537.36"
        )
    }

    news_results = []
    page = 0
    while True:
        offset = page * 10
        url = (
            f"https://www.google.com/search?q={query}"
            f"&tbs=cdr:1,cd_min:{start_date},cd_max:{end_date}"
            f"&tbm=nws&start={offset}"
        )

        try:
            response = make_request(url, headers)
            soup = BeautifulSoup(response.content, "html.parser")
            results_on_page = soup.select("div.SoaBEf")

            if not results_on_page:
                break  # No more results found

            for el in results_on_page:
                try:
                    link = el.find("a")["href"]
                    title = el.select_one("div.MBeuO").get_text()
                    snippet = el.select_one(".GI74Re").get_text()
                    date = el.select_one(".LfVVr").get_text()
                    source = el.select_one(".NUnG9d span").get_text()
                    news_results.append(
                        {
                            "link": link,
                            "title": title,
                            "snippet": snippet,
                            "date": date,
                            "source": source,
                        }
                    )
                except Exception as e:
                    logger.error(f"Error processing result: {e}")
                    # If one of the fields is not found, skip this result
                    continue

            # Update the progress bar with the current count of results scraped

            # Check for the "Next" link (pagination)
            next_link = soup.find("a", id="pnnext")
            if not next_link:
                break

            page += 1

        except requests.exceptions.Timeout as e:
            logger.error(f"连接超时: {e}")
            # 不立即中断，记录错误后继续尝试下一页
            page += 1
            if page > 3:  # 如果连续多页都超时，则退出循环
                logger.error("多次连接超时，停止获取Google新闻")
                break
            continue
        except requests.exceptions.ConnectionError as e:
            logger.error(f"连接错误: {e}")
            # 不立即中断，记录错误后继续尝试下一页
            page += 1
            if page > 3:  # 如果连续多页都连接错误，则退出循环
                logger.error("多次连接错误，停止获取Google新闻")
                break
            continue
        except Exception as e:
            logger.error(f"获取Google新闻失败: {e}")
            break

    return news_results
