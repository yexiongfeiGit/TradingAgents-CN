package com.tradingagents.config;

import com.tradingagents.agent.analyst.*;
import com.tradingagents.agent.researcher.*;
import com.tradingagents.agent.manager.*;
import com.tradingagents.debate.AgentCoordinator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智能体配置类
 * 配置所有智能体相关的Bean
 */
@Configuration
public class AgentConfig {

    /**
     * 技术分析师
     */
    @Bean
    public MarketAnalyst technicalAnalyst() {
        return new MarketAnalyst();
    }

    /**
     * 基本面分析师
     */
    @Bean
    public FundamentalsAnalyst fundamentalsAnalyst() {
        return new FundamentalsAnalyst();
    }

    /**
     * 情绪分析师
     */
    @Bean
    public SentimentAnalyst sentimentAnalyst() {
        return new SentimentAnalyst();
    }

    /**
     * 新闻分析师
     */
    @Bean
    public NewsAnalyst newsAnalyst() {
        return new NewsAnalyst();
    }

    /**
     * 多头研究员
     */
    @Bean
    public BullResearcher bullResearcher() {
        return new BullResearcher();
    }

    /**
     * 空头研究员
     */
    @Bean
    public BearResearcher bearResearcher() {
        return new BearResearcher();
    }

    /**
     * 风险管理者
     */
    @Bean
    public RiskManager riskManager() {
        return new RiskManager();
    }

    /**
     * 投资组合经理
     */
    @Bean
    public PortfolioManager portfolioManager() {
        return new PortfolioManager();
    }

    /**
     * 智能体协调器
     */
    @Bean
    public AgentCoordinator agentCoordinator() {
        return new AgentCoordinator();
    }
}