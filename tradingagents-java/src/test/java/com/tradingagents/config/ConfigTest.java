package com.tradingagents.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("配置测试")
public class ConfigTest {

    @Autowired
    private TradingAgentsConfig config;

    @Test
    @DisplayName("测试配置加载")
    void testConfigLoading() {
        assertNotNull(config);
    }

    @Test
    @DisplayName("测试分析配置")
    void testAnalysisConfig() {
        assertNotNull(config.getAnalysis());
        assertTrue(config.getAnalysis().getMaxDebateRounds() > 0);
        assertTrue(config.getAnalysis().getMaxRiskDiscussRounds() > 0);
        assertTrue(config.getAnalysis().getMaxToolCalls() > 0);
    }

    @Test
    @DisplayName("测试智能体配置")
    void testAgentsConfig() {
        assertNotNull(config.getAgents());
        assertNotNull(config.getAgents().getMarketAnalyst());
        assertNotNull(config.getAgents().getSentimentAnalyst());
        assertNotNull(config.getAgents().getNewsAnalyst());
        assertNotNull(config.getAgents().getFundamentalsAnalyst());
        assertNotNull(config.getAgents().getBullResearcher());
        assertNotNull(config.getAgents().getBearResearcher());
        assertNotNull(config.getAgents().getResearchManager());
        assertNotNull(config.getAgents().getRiskAnalysts());
        assertNotNull(config.getAgents().getPortfolioManager());
    }

    @Test
    @DisplayName("测试股票数据配置")
    void testStockDataConfig() {
        assertNotNull(config.getStockData());
        assertNotNull(config.getStockData().getProviders());
        assertNotNull(config.getStockData().getCache());
    }

    @Test
    @DisplayName("测试提供商配置")
    void testProvidersConfig() {
        assertNotNull(config.getStockData().getProviders().getTushare());
        assertNotNull(config.getStockData().getProviders().getAkshare());
        assertNotNull(config.getStockData().getProviders().getYahooFinance());
    }

    @Test
    @DisplayName("测试缓存配置")
    void testCacheConfig() {
        assertNotNull(config.getStockData().getCache().getTtl());
        assertTrue(config.getStockData().getCache().getTtl().getUsStock() > 0);
        assertTrue(config.getStockData().getCache().getTtl().getChinaStock() > 0);
        assertTrue(config.getStockData().getCache().getTtl().getHkStock() > 0);
    }
}