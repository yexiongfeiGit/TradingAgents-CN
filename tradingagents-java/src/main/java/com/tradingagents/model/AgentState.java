package com.tradingagents.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 智能体状态类 - 对应Python版本的AgentState
 * 用于在图执行过程中传递状态信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {
    
    // 消息列表
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    
    // 分析报告
    private String marketReport;
    private String sentimentReport;
    private String newsReport;
    private String fundamentalsReport;
    private String marketAnalysis;
    
    // 工具调用计数
    @Builder.Default
    private Map<String, Integer> toolCallCounts = new HashMap<>();
    
    // 投资辩论状态
    private InvestmentDebateState investmentDebateState;
    
    // 风险讨论状态
    private RiskDebateState riskDebateState;
    
    // 公司信息
    private String companyOfInterest;
    private String companyName;
    private MarketInfo marketInfo;
    
    // 内存系统
    private MemoryStore memory;
    
    // 错误信息
    private String error;
    
    /**
     * 消息类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;  // system, user, assistant, tool
        private String content;
        private List<ToolCall> toolCalls;
        private String toolCallId;
        private Map<String, Object> additionalProperties;
        
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
        
        public String getContent() {
            return content;
        }
    }
    
    /**
     * 工具调用类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String id;
        private String name;
        private Map<String, Object> args;
    }
    
    /**
     * 投资辩论状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentDebateState {
        private String history;
        private String bullHistory;
        private String bearHistory;
        private String currentResponse;
        private int count;
        private String judgeDecision;
        private Map<String, Object> additionalProperties;
        
        public List<String> getDebateHistory() {
            List<String> history = new ArrayList<>();
            if (bullHistory != null && !bullHistory.isEmpty()) {
                history.add("多头观点: " + bullHistory);
            }
            if (bearHistory != null && !bearHistory.isEmpty()) {
                history.add("空头观点: " + bearHistory);
            }
            if (this.history != null && !this.history.isEmpty()) {
                history.add("综合讨论: " + this.history);
            }
            return history;
        }
        
        public void addBullArgument(String argument) {
            if (bullHistory == null) {
                bullHistory = argument;
            } else {
                bullHistory += "\n" + argument;
            }
        }
        
        public void addBearArgument(String argument) {
            if (bearHistory == null) {
                bearHistory = argument;
            } else {
                bearHistory += "\n" + argument;
            }
        }
        
        public void incrementBullSpeeches() {
            count++;
        }
        
        public void incrementBearSpeeches() {
            count++;
        }
        
        public int getBullSpeeches() {
            return count;
        }
        
        public int getBearSpeeches() {
            return count;
        }
    }
    
    /**
     * 风险讨论状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskDebateState {
        private String history;
        private String aggressiveHistory;
        private String conservativeHistory;
        private String neutralHistory;
        private String currentResponse;
        private int count;
        private String finalDecision;
        private Map<String, Object> additionalProperties;
        
        public void addRiskAnalysis(String analysis) {
            if (history == null) {
                history = analysis;
            } else {
                history += "\n" + analysis;
            }
        }
        
        public void incrementRiskSpeeches() {
            count++;
        }
        
        public List<String> getRiskAnalyses() {
            List<String> analyses = new ArrayList<>();
            if (aggressiveHistory != null && !aggressiveHistory.isEmpty()) {
                analyses.add("激进观点: " + aggressiveHistory);
            }
            if (conservativeHistory != null && !conservativeHistory.isEmpty()) {
                analyses.add("保守观点: " + conservativeHistory);
            }
            if (neutralHistory != null && !neutralHistory.isEmpty()) {
                analyses.add("中性观点: " + neutralHistory);
            }
            if (history != null && !history.isEmpty()) {
                analyses.add("综合讨论: " + history);
            }
            return analyses;
        }
        
        public void setFinalDecision(String finalDecision) {
            this.finalDecision = finalDecision;
        }
        
        public String getFinalDecision() {
            return finalDecision;
        }
    }
    
    /**
     * 市场信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketInfo {
        private boolean isChina;
        private boolean isHK;
        private boolean isUS;
        private String marketName;
        private String currencyName;
        private String currencySymbol;
        
        public String getMarketName() {
            return marketName;
        }
    }
    
    /**
     * 内存存储
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryStore {
        private List<Memory> memories;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Memory {
            private String recommendation;
            private String timestamp;
            private double relevance;
        }
    }
    
    // 工具方法
    public void incrementToolCallCount(String toolName) {
        toolCallCounts.merge(toolName, 1, Integer::sum);
    }
    
    public int getToolCallCount(String toolName) {
        return toolCallCounts.getOrDefault(toolName, 0);
    }
    
    public Message getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
    
    public void addMessage(Message message) {
        messages.add(message);
    }
    
    /**
     * 获取公司信息
     */
    public CompanyInfo getCompanyInfo() {
        return new CompanyInfo(companyOfInterest, companyName, marketInfo);
    }
    
    /**
     * 公司信息类
     */
    @Data
    @AllArgsConstructor
    public static class CompanyInfo {
        private String stockCode;
        private String stockName;
        private MarketInfo marketType;
        
        public String getStockCode() {
            return stockCode;
        }
        
        public String getStockName() {
            return stockName;
        }
        
        public String getMarketType() {
            return marketType != null ? marketType.getMarketName() : "Unknown";
        }
    }
    
    /**
     * 获取市场分析
     */
    public String getMarketAnalysis() {
        return marketAnalysis;
    }
    
    /**
     * 设置市场分析
     */
    public void setMarketAnalysis(String marketAnalysis) {
        this.marketAnalysis = marketAnalysis;
    }
    
    /**
     * 设置情绪报告
     */
    public void setSentimentReport(String sentimentReport) {
        this.sentimentReport = sentimentReport;
    }
    
    /**
     * 获取消息内容列表
     */
    public List<String> getMessages() {
        List<String> messageContents = new ArrayList<>();
        for (Message message : messages) {
            if (message.getContent() != null) {
                messageContents.add(message.getContent());
            }
        }
        return messageContents;
    }
    
    /**
     * 获取情绪报告
     */
    public String getSentimentReport() {
        return sentimentReport;
    }
    
    /**
     * 获取新闻报告
     */
    public String getNewsReport() {
        return newsReport;
    }
    
    /**
     * 获取基本面报告
     */
    public String getFundamentalsReport() {
        return fundamentalsReport;
    }
    
    /**
     * 获取投资辩论状态
     */
    public InvestmentDebateState getInvestmentDebate() {
        return investmentDebateState;
    }
    
    /**
     * 获取风险讨论状态
     */
    public RiskDebateState getRiskDiscussion() {
        return riskDebateState;
    }
    
    /**
     * 设置新闻报告
     */
    public void setNewsReport(String newsReport) {
        this.newsReport = newsReport;
    }
    
    /**
     * 设置基本面报告
     */
    public void setFundamentalsReport(String fundamentalsReport) {
        this.fundamentalsReport = fundamentalsReport;
    }
    
    /**
     * 设置最终决策
     */
    public void setFinalDecision(String finalDecision) {
        if (riskDebateState == null) {
            riskDebateState = new RiskDebateState();
        }
        riskDebateState.setFinalDecision(finalDecision);
    }
}