package com.tradingagents.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * 缓存配置类
 * 配置不同类型的缓存管理器
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 内存缓存管理器
     */
    @Bean
    public CacheManager memoryCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "stockData",
            "marketData", 
            "agentResponses",
            "analysisResults",
            "financialData",
            "portfolioData",
            "newsData",
            "sentimentData"
        ));
        return cacheManager;
    }

    /**
     * 股票数据缓存管理器
     */
    @Bean
    public CacheManager stockDataCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "stockRealTime",
            "stockHistorical",
            "stockTechnical",
            "stockFundamental",
            "stockNews",
            "stockSentiment"
        ));
        return cacheManager;
    }

    /**
     * 智能体缓存管理器
     */
    @Bean
    public CacheManager agentCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "agentConfigs",
            "agentStates",
            "agentMessages",
            "agentAnalysis",
            "agentDecisions"
        ));
        return cacheManager;
    }
}