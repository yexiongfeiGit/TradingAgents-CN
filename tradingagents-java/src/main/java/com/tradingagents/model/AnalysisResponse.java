package com.tradingagents.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分析响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    
    private String requestId;
    private String stockCode;
    private String stockName;
    private int researchDepth;
    private String status;  // pending, running, completed, failed
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long duration;  // 毫秒
    
    // 分析报告
    private MarketAnalysis marketAnalysis;
    private SentimentAnalysis sentimentAnalysis;
    private NewsAnalysis newsAnalysis;
    private FundamentalsAnalysis fundamentalsAnalysis;
    
    // 投资辩论结果
    private InvestmentDebate investmentDebate;
    
    // 风险分析结果
    private RiskAnalysis riskAnalysis;
    
    // 最终决策
    private FinalDecision finalDecision;
    
    // 统计信息
    private AnalysisStats stats;
    
    // 错误信息
    private String error;
    private String errorDetails;
    
    /**
     * 市场分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketAnalysis {
        private String report;
        private String trend;
        private double confidence;
        private List<String> keyPoints;
        private Map<String, Object> technicalIndicators;
    }
    
    /**
     * 情绪分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentAnalysis {
        private String report;
        private String overallSentiment;  // bullish, bearish, neutral
        private double sentimentScore;
        private List<String> socialMediaSources;
        private Map<String, Double> sentimentBreakdown;
    }
    
    /**
     * 新闻分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsAnalysis {
        private String report;
        private List<NewsItem> recentNews;
        private String newsImpact;  // positive, negative, neutral
        private double impactScore;
    }
    
    /**
     * 新闻条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsItem {
        private String title;
        private String summary;
        private LocalDateTime publishedDate;
        private String source;
        private String sentiment;  // positive, negative, neutral
    }
    
    /**
     * 基本面分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundamentalsAnalysis {
        private String report;
        private FinancialMetrics financialMetrics;
        private String financialHealth;  // strong, moderate, weak
        private double healthScore;
    }
    
    /**
     * 财务指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialMetrics {
        private double peRatio;
        private double pbRatio;
        private double debtToEquity;
        private double currentRatio;
        private double roe;
        private double profitMargin;
        private double revenueGrowth;
    }
    
    /**
     * 投资辩论
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentDebate {
        private List<DebateRound> rounds;
        private String finalDecision;
        private double bullConfidence;
        private double bearConfidence;
        private String debateSummary;
    }
    
    /**
     * 辩论轮次
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebateRound {
        private int roundNumber;
        private String bullArgument;
        private String bearArgument;
        private String managerDecision;
        private double bullScore;
        private double bearScore;
    }
    
    /**
     * 风险分析
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAnalysis {
        private List<RiskDiscussion> discussions;
        private String finalRiskAssessment;
        private double riskScore;  // 0-100
        private String riskLevel;  // low, medium, high, extreme
        private List<String> keyRisks;
        private List<String> mitigationStrategies;
    }
    
    /**
     * 风险讨论
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskDiscussion {
        private int roundNumber;
        private String aggressiveView;
        private String conservativeView;
        private String neutralView;
        private String consensus;
    }
    
    /**
     * 最终决策
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalDecision {
        private String recommendation;  // buy, hold, sell
        private double confidence;
        private String rationale;
        private List<String> supportingEvidence;
        private List<String> concerns;
        private PriceTargets priceTargets;
    }
    
    /**
     * 价格目标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceTargets {
        private double currentPrice;
        private double targetPrice1M;
        private double targetPrice3M;
        private double targetPrice6M;
        private double targetPrice1Y;
        private double upsidePotential;
    }
    
    /**
     * 分析统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisStats {
        private int totalAiCalls;
        private int debateRounds;
        private int riskDiscussionRounds;
        private long totalTokensUsed;
        private double estimatedCost;
        private Map<String, Integer> agentCalls;
    }
}