package com.tradingagents.debate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体协调器
 * 管理多个智能体之间的对话、辩论和协作
 */
@Data
@Slf4j
public class AgentCoordinator {
    
    private static final Logger log = LoggerFactory.getLogger(AgentCoordinator.class);
    
    private Map<String, MultiRoundDialogue> agentDialogues;
    private InvestmentDebate investmentDebate;
    private RiskDiscussion riskDiscussion;
    private List<String> coordinationLog;
    private boolean isActive;
    private String currentPhase;
    
    public AgentCoordinator() {
        this.agentDialogues = new ConcurrentHashMap<>();
        this.investmentDebate = new InvestmentDebate();
        this.riskDiscussion = new RiskDiscussion();
        this.coordinationLog = new ArrayList<>();
        this.isActive = true;
        this.currentPhase = "INIT";
    }
    
    /**
     * 初始化智能体对话
     */
    public void initializeAgentDialogue(String agentName) {
        MultiRoundDialogue dialogue = new MultiRoundDialogue();
        agentDialogues.put(agentName, dialogue);
        log.debug("初始化智能体对话: {}", agentName);
    }
    
    /**
     * 添加智能体对话
     */
    public void addAgentDialogue(String agentName, String content) {
        MultiRoundDialogue dialogue = agentDialogues.get(agentName);
        if (dialogue != null) {
            dialogue.addDialogue(agentName, content);
            log.debug("添加智能体对话 - 智能体: {}, 内容长度: {}", agentName, content.length());
        } else {
            log.warn("智能体对话未找到: {}", agentName);
        }
    }
    
    /**
     * 添加投资辩论论点
     */
    public void addInvestmentArgument(String side, String argument) {
        if ("bull".equalsIgnoreCase(side)) {
            investmentDebate.addBullArgument(argument);
        } else if ("bear".equalsIgnoreCase(side)) {
            investmentDebate.addBearArgument(argument);
        }
        log.debug("添加投资辩论论点 - 方: {}, 内容长度: {}", side, argument.length());
    }
    
    /**
     * 添加风险讨论
     */
    public void addRiskDiscussion(String type, String content) {
        if ("concern".equalsIgnoreCase(type)) {
            riskDiscussion.addRiskConcern(content);
        } else if ("mitigation".equalsIgnoreCase(type)) {
            riskDiscussion.addRiskMitigation(content);
        }
        log.debug("添加风险讨论 - 类型: {}, 内容长度: {}", type, content.length());
    }
    
    /**
     * 更新风险评分
     */
    public void updateRiskScore(double score) {
        riskDiscussion.updateRiskScore(score);
        log.debug("更新风险评分: {}", score);
    }
    
    /**
     * 检查协调是否应该继续
     */
    public boolean shouldContinueCoordination() {
        // 如果协调不活跃，停止
        if (!isActive) {
            log.debug("协调已停止");
            return false;
        }
        
        // 检查投资辩论状态
        if (investmentDebate.shouldContinueDebate()) {
            log.debug("投资辩论继续进行中");
            return true;
        }
        
        // 检查风险讨论状态
        if (riskDiscussion.shouldContinueDiscussion()) {
            log.debug("风险讨论继续进行中");
            return true;
        }
        
        // 检查智能体对话状态
        for (Map.Entry<String, MultiRoundDialogue> entry : agentDialogues.entrySet()) {
            if (entry.getValue().shouldContinueDialogue()) {
                log.debug("智能体对话继续进行中 - 智能体: {}", entry.getKey());
                return true;
            }
        }
        
        log.debug("所有协调活动已完成");
        return false;
    }
    
    /**
     * 进入下一阶段
     */
    public void nextPhase(String newPhase) {
        this.currentPhase = newPhase;
        coordinationLog.add(String.format("进入新阶段: %s", newPhase));
        log.debug("进入新阶段: {}", newPhase);
    }
    
    /**
     * 获取协调总结
     */
    public String getCoordinationSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("智能体协调总结:\n");
        summary.append("当前阶段: ").append(currentPhase).append("\n");
        summary.append("协调状态: ").append(isActive ? "活跃" : "结束").append("\n");
        summary.append("活跃对话数量: ").append(agentDialogues.size()).append("\n\n");
        
        // 投资辩论总结
        if (investmentDebate.getBullSpeeches() > 0 || investmentDebate.getBearSpeeches() > 0) {
            summary.append("投资辩论:\n");
            summary.append(investmentDebate.getDebateSummary()).append("\n\n");
        }
        
        // 风险讨论总结
        if (riskDiscussion.getDiscussionRounds() > 0) {
            summary.append("风险讨论:\n");
            summary.append(riskDiscussion.getDiscussionSummary()).append("\n\n");
        }
        
        // 智能体对话统计
        summary.append("智能体对话统计:\n");
        for (Map.Entry<String, MultiRoundDialogue> entry : agentDialogues.entrySet()) {
            summary.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue().getDialogueStats()));
        }
        
        return summary.toString();
    }
    
    /**
     * 获取当前状态
     */
    public String getCurrentStatus() {
        return String.format("阶段: %s, 状态: %s, 对话: %d, 辩论: %d/%d, 风险: %.1f",
                currentPhase, isActive ? "活跃" : "结束", agentDialogues.size(),
                investmentDebate.getBullSpeeches(), investmentDebate.getBearSpeeches(),
                riskDiscussion.getRiskScore());
    }
    
    /**
     * 重置协调
     */
    public void reset() {
        agentDialogues.clear();
        investmentDebate.reset();
        riskDiscussion.reset();
        coordinationLog.clear();
        currentPhase = "INIT";
        isActive = true;
        log.debug("智能体协调已重置");
    }
    
    /**
     * 停止协调
     */
    public void stopCoordination() {
        this.isActive = false;
        log.debug("智能体协调已手动停止");
    }
    
    /**
     * 获取特定智能体的对话
     */
    public MultiRoundDialogue getAgentDialogue(String agentName) {
        return agentDialogues.get(agentName);
    }
    
    /**
     * 获取投资辩论
     */
    public InvestmentDebate getInvestmentDebate() {
        return investmentDebate;
    }
    
    /**
     * 获取风险讨论
     */
    public RiskDiscussion getRiskDiscussion() {
        return riskDiscussion;
    }
}