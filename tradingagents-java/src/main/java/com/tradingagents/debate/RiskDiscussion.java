package com.tradingagents.debate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 风险评估讨论机制
 * 管理风险管理者与其他智能体之间的风险讨论
 */
@Data
@Slf4j
public class RiskDiscussion {
    

    private List<String> discussionHistory;
    private List<String> riskConcerns;
    private List<String> riskMitigations;
    private int discussionRounds;
    private int maxRounds;
    private boolean isActive;
    private double riskScore; // 风险评分 0-100
    
    public RiskDiscussion() {
        this.discussionHistory = new ArrayList<>();
        this.riskConcerns = new ArrayList<>();
        this.riskMitigations = new ArrayList<>();
        this.discussionRounds = 0;
        this.maxRounds = 2; // 默认最多2轮讨论
        this.isActive = true;
        this.riskScore = 50.0; // 初始风险评分
    }
    
    /**
     * 添加风险关注点
     */
    public void addRiskConcern(String concern) {
        if (concern != null && !concern.trim().isEmpty()) {
            riskConcerns.add(concern);
            discussionHistory.add("[风险关注] " + concern);
            discussionRounds++;
            log.debug("添加风险关注点: {}", concern);
        }
    }
    
    /**
     * 添加风险缓解措施
     */
    public void addRiskMitigation(String mitigation) {
        if (mitigation != null && !mitigation.trim().isEmpty()) {
            riskMitigations.add(mitigation);
            discussionHistory.add("[风险缓解] " + mitigation);
            log.debug("添加风险缓解措施: {}", mitigation);
        }
    }
    
    /**
     * 更新风险评分
     */
    public void updateRiskScore(double newScore) {
        this.riskScore = Math.max(0, Math.min(100, newScore));
        log.debug("更新风险评分: {}", riskScore);
    }
    
    /**
     * 检查讨论是否应该继续
     */
    public boolean shouldContinueDiscussion() {
        // 如果讨论不活跃，停止
        if (!isActive) {
            log.debug("风险讨论已停止");
            return false;
        }
        
        // 如果达到最大讨论轮次，停止
        if (discussionRounds >= maxRounds) {
            log.debug("达到最大讨论轮次: {}，停止讨论", maxRounds);
            return false;
        }
        
        // 如果风险评分过高，需要更多讨论
        if (riskScore > 70) {
            log.debug("风险评分过高 ({} > 70)，继续讨论", riskScore);
            return true;
        }
        
        // 如果风险评分过低，可以停止
        if (riskScore < 30) {
            log.debug("风险评分过低 ({} < 30)，可以停止", riskScore);
            return false;
        }
        
        log.debug("风险评分适中 ({}), 继续讨论", riskScore);
        return true;
    }
    
    /**
     * 获取风险等级
     */
    public String getRiskLevel() {
        if (riskScore >= 80) {
            return "极高风险";
        } else if (riskScore >= 60) {
            return "高风险";
        } else if (riskScore >= 40) {
            return "中等风险";
        } else if (riskScore >= 20) {
            return "低风险";
        } else {
            return "极低风险";
        }
    }
    
    /**
     * 获取讨论总结
     */
    public String getDiscussionSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("风险讨论总结:\n");
        summary.append("讨论轮次: ").append(discussionRounds).append("\n");
        summary.append("风险评分: ").append(String.format("%.1f", riskScore)).append("/100\n");
        summary.append("风险等级: ").append(getRiskLevel()).append("\n");
        summary.append("讨论状态: ").append(isActive ? "活跃" : "结束").append("\n\n");
        
        summary.append("主要风险关注点:\n");
        for (int i = 0; i < Math.min(riskConcerns.size(), 3); i++) {
            summary.append("- ").append(riskConcerns.get(i)).append("\n");
        }
        
        summary.append("\n主要风险缓解措施:\n");
        for (int i = 0; i < Math.min(riskMitigations.size(), 3); i++) {
            summary.append("- ").append(riskMitigations.get(i)).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 重置讨论
     */
    public void reset() {
        discussionHistory.clear();
        riskConcerns.clear();
        riskMitigations.clear();
        discussionRounds = 0;
        riskScore = 50.0;
        isActive = true;
        log.debug("风险讨论已重置");
    }
    
    /**
     * 停止讨论
     */
    public void stopDiscussion() {
        this.isActive = false;
        log.debug("风险讨论已手动停止");
    }
    
    /**
     * 获取最新讨论内容
     */
    public String getLatestDiscussion() {
        if (discussionHistory.isEmpty()) {
            return "";
        }
        return discussionHistory.get(discussionHistory.size() - 1);
    }
    
    /**
     * 获取讨论状态
     */
    public String getDiscussionStatus() {
        return String.format("讨论状态: %s, 轮次: %d, 风险评分: %.1f, 风险等级: %s",
                isActive ? "活跃" : "结束", discussionRounds, riskScore, getRiskLevel());
    }
    
    /**
     * 设置最大讨论轮次
     */
    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
        log.debug("设置最大讨论轮次: {}", maxRounds);
    }
    
    /**
     * 计算综合风险评分
     */
    public double calculateCompositeRiskScore() {
        double baseScore = riskScore;
        
        // 根据风险关注点数量调整
        double concernPenalty = riskConcerns.size() * 2.0;
        
        // 根据风险缓解措施数量调整
        double mitigationBonus = riskMitigations.size() * 1.5;
        
        // 计算最终评分
        double finalScore = baseScore + concernPenalty - mitigationBonus;
        
        return Math.max(0, Math.min(100, finalScore));
    }
    
    /**
     * 获取讨论轮次
     */
    public int getDiscussionRounds() {
        return discussionRounds;
    }
    
    /**
     * 获取风险评分
     */
    public double getRiskScore() {
        return riskScore;
    }
}