package com.tradingagents.debate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 投资辩论机制
 * 管理多头和空头研究员之间的辩论过程
 */
@Data
@Slf4j
public class InvestmentDebate {
    
    private static final Logger log = LoggerFactory.getLogger(InvestmentDebate.class);
    
    private List<String> debateHistory;
    private List<String> bullArguments;
    private List<String> bearArguments;
    private int bullSpeeches;
    private int bearSpeeches;
    private int maxDebates;
    private boolean isActive;
    
    public InvestmentDebate() {
        this.debateHistory = new ArrayList<>();
        this.bullArguments = new ArrayList<>();
        this.bearArguments = new ArrayList<>();
        this.bullSpeeches = 0;
        this.bearSpeeches = 0;
        this.maxDebates = 3; // 默认最多3轮辩论
        this.isActive = true;
    }
    
    /**
     * 添加多头论点
     */
    public void addBullArgument(String argument) {
        if (argument != null && !argument.trim().isEmpty()) {
            bullArguments.add(argument);
            debateHistory.add("[多头] " + argument);
            bullSpeeches++;
            log.debug("添加多头论点，当前数量: {}", bullSpeeches);
        }
    }
    
    /**
     * 添加空头论点
     */
    public void addBearArgument(String argument) {
        if (argument != null && !argument.trim().isEmpty()) {
            bearArguments.add(argument);
            debateHistory.add("[空头] " + argument);
            bearSpeeches++;
            log.debug("添加空头论点，当前数量: {}", bearSpeeches);
        }
    }
    
    /**
     * 检查辩论是否应该继续
     */
    public boolean shouldContinueDebate() {
        // 如果辩论不活跃，停止
        if (!isActive) {
            log.debug("辩论已停止");
            return false;
        }
        
        // 如果达到最大辩论次数，停止
        int totalSpeeches = bullSpeeches + bearSpeeches;
        if (totalSpeeches >= maxDebates * 2) { // 每轮包含多头和空头各一次
            log.debug("达到最大辩论次数: {}，停止辩论", maxDebates);
            return false;
        }
        
        // 确保双方都有发言机会
        if (bullSpeeches == 0 || bearSpeeches == 0) {
            log.debug("确保双方都有发言机会，继续辩论");
            return true;
        }
        
        // 检查是否达到平衡（双方发言次数相近）
        int speechDiff = Math.abs(bullSpeeches - bearSpeeches);
        if (speechDiff > 1) {
            log.debug("发言次数不平衡，继续辩论");
            return true;
        }
        
        log.debug("辩论平衡，可以停止");
        return false;
    }
    
    /**
     * 获取下一位发言者
     */
    public String getNextSpeaker() {
        if (bullSpeeches <= bearSpeeches) {
            return "bull";
        } else {
            return "bear";
        }
    }
    
    /**
     * 获取辩论总结
     */
    public String getDebateSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("投资辩论总结:\n");
        summary.append("总发言次数: ").append(bullSpeeches + bearSpeeches).append("\n");
        summary.append("多头发言次数: ").append(bullSpeeches).append("\n");
        summary.append("空头发言次数: ").append(bearSpeeches).append("\n");
        summary.append("辩论状态: ").append(isActive ? "活跃" : "结束").append("\n\n");
        
        summary.append("主要多头论点:\n");
        for (int i = 0; i < Math.min(bullArguments.size(), 3); i++) {
            summary.append("- ").append(bullArguments.get(i)).append("\n");
        }
        
        summary.append("\n主要空头论点:\n");
        for (int i = 0; i < Math.min(bearArguments.size(), 3); i++) {
            summary.append("- ").append(bearArguments.get(i)).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 重置辩论
     */
    public void reset() {
        debateHistory.clear();
        bullArguments.clear();
        bearArguments.clear();
        bullSpeeches = 0;
        bearSpeeches = 0;
        isActive = true;
        log.debug("辩论已重置");
    }
    
    /**
     * 停止辩论
     */
    public void stopDebate() {
        this.isActive = false;
        log.debug("辩论已手动停止");
    }
    
    /**
     * 获取最新论点
     */
    public String getLatestArgument() {
        if (debateHistory.isEmpty()) {
            return "";
        }
        return debateHistory.get(debateHistory.size() - 1);
    }
    
    /**
     * 获取辩论状态
     */
    public String getDebateStatus() {
        return String.format("辩论状态: %s, 多头: %d, 空头: %d, 总计: %d",
                isActive ? "活跃" : "结束", bullSpeeches, bearSpeeches, bullSpeeches + bearSpeeches);
    }
    
    /**
     * 设置最大辩论次数
     */
    public void setMaxDebates(int maxDebates) {
        this.maxDebates = maxDebates;
        log.debug("设置最大辩论次数: {}", maxDebates);
    }
    
    /**
     * 获取多头发言次数
     */
    public int getBullSpeeches() {
        return bullSpeeches;
    }
    
    /**
     * 获取空头发言次数
     */
    public int getBearSpeeches() {
        return bearSpeeches;
    }
    
    /**
     * 获取多头论点列表
     */
    public List<String> getBullArguments() {
        return bullArguments;
    }
    
    /**
     * 获取空头论点列表
     */
    public List<String> getBearArguments() {
        return bearArguments;
    }
}