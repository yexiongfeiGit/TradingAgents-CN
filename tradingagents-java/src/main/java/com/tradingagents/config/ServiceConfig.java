package com.tradingagents.config;

import com.tradingagents.data.FinancialDataUtils;
import com.tradingagents.data.MarketDataProvider;
import com.tradingagents.data.StockDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 服务配置类
 * 配置数据服务相关的Bean
 */
@Configuration
public class ServiceConfig {

    @Value("${tradingagents.cache.ttl-minutes:60}")
    private int cacheTtlMinutes;

    @Value("${tradingagents.cache.max-size:1000}")
    private int cacheMaxSize;

    @Value("${tradingagents.cache.auto-refresh:true}")
    private boolean autoRefresh;

    @Value("${tradingagents.cache.refresh-interval-minutes:5}")
    private int refreshIntervalMinutes;

    /**
     * 市场数据提供者
     */
    @Bean
    public MarketDataProvider marketDataProvider() {
        return new MarketDataProvider();
    }

    /**
     * 股票数据服务
     */
    @Bean
    public StockDataService stockDataService(MarketDataProvider marketDataProvider) {
        StockDataService service = new StockDataService(marketDataProvider);
        service.setCacheTtlMinutes(cacheTtlMinutes);
        service.setAutoRefresh(autoRefresh);
        service.setRefreshIntervalMinutes(refreshIntervalMinutes);
        return service;
    }

    /**
     * 金融数据工具
     */
    @Bean
    public FinancialDataUtils financialDataUtils(StockDataService stockDataService) {
        return new FinancialDataUtils(stockDataService);
    }
}