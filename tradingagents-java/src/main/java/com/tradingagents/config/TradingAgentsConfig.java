package com.tradingagents.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * TradingAgents配置属性类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tradingagents")
public class TradingAgentsConfig {
    
    private AnalysisConfig analysis;
    private AgentsConfig agents;
    private StockDataConfig stockData;
    
    @Data
    public static class AnalysisConfig {
        private int maxDebateRounds = 2;
        private int maxRiskDiscussRounds = 3;
        private int maxToolCalls = 3;
        private Map<Integer, String> researchDepths;
    }
    
    @Data
    public static class AgentsConfig {
        private AgentConfig marketAnalyst;
        private AgentConfig sentimentAnalyst;
        private AgentConfig newsAnalyst;
        private AgentConfig fundamentalsAnalyst;
        private AgentConfig bullResearcher;
        private AgentConfig bearResearcher;
        private AgentConfig researchManager;
        private RiskAnalystsConfig riskAnalysts;
        private AgentConfig portfolioManager;
    }
    
    @Data
    public static class AgentConfig {
        private boolean enabled = true;
        private int maxToolCalls = 3;
    }
    
    @Data
    public static class RiskAnalystsConfig {
        private AgentConfig aggressive;
        private AgentConfig conservative;
        private AgentConfig neutral;
    }
    
    @Data
    public static class StockDataConfig {
        private StockProvidersConfig providers;
        private CacheConfig cache;
    }
    
    @Data
    public static class StockProvidersConfig {
        private TushareConfig tushare;
        private AkshareConfig akshare;
        private YahooFinanceConfig yahooFinance;
    }
    
    @Data
    public static class TushareConfig {
        private String apiKey;
        private boolean enabled = true;
    }
    
    @Data
    public static class AkshareConfig {
        private boolean enabled = true;
    }
    
    @Data
    public static class YahooFinanceConfig {
        private boolean enabled = true;
        private String baseUrl = "https://query1.finance.yahoo.com";
    }
    
    @Data
    public static class CacheConfig {
        private boolean enabled = true;
        private TtlConfig ttl;
    }
    
    @Data
    public static class TtlConfig {
        private int usStock = 3600;
        private int chinaStock = 1800;
        private int hkStock = 2700;
    }
}