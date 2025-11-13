package com.tradingagents.debate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 多轮对话管理器
 * 管理智能体之间的多轮对话和协作
 */
@Data
@Slf4j
public class MultiRoundDialogue {
    

    private List<String> dialogueHistory;
    private List<String> keyInsights;
    private List<String> actionItems;
    private int currentRound;
    private int maxRounds;
    private boolean isActive;
    private String dialogueContext;
    private String currentSpeaker;
    
    public MultiRoundDialogue() {
        this.dialogueHistory = new ArrayList<>();
        this.keyInsights = new ArrayList<>();
        this.actionItems = new ArrayList<>();
        this.currentRound = 0;
        this.maxRounds = 5; // 默认最多5轮对话
        this.isActive = true;
        this.dialogueContext = "";
        this.currentSpeaker = "";
    }
    
    /**
     * 添加对话内容
     */
    public void addDialogue(String speaker, String content) {
        if (content != null && !content.trim().isEmpty()) {
            String dialogueEntry = String.format("[%s] %s", speaker, content);
            dialogueHistory.add(dialogueEntry);
            currentSpeaker = speaker;
            log.debug("添加对话内容 - 发言者: {}, 内容长度: {}", speaker, content.length());
        }
    }
    
    /**
     * 添加关键洞察
     */
    public void addKeyInsight(String insight) {
        if (insight != null && !insight.trim().isEmpty()) {
            keyInsights.add(insight);
            dialogueHistory.add("[关键洞察] " + insight);
            log.debug("添加关键洞察: {}", insight);
        }
    }
    
    /**
     * 添加行动项
     */
    public void addActionItem(String actionItem) {
        if (actionItem != null && !actionItem.trim().isEmpty()) {
            actionItems.add(actionItem);
            dialogueHistory.add("[行动项] " + actionItem);
            log.debug("添加行动项: {}", actionItem);
        }
    }
    
    /**
     * 更新对话上下文
     */
    public void updateContext(String context) {
        this.dialogueContext = context;
        log.debug("更新对话上下文");
    }
    
    /**
     * 检查对话是否应该继续
     */
    public boolean shouldContinueDialogue() {
        // 如果对话不活跃，停止
        if (!isActive) {
            log.debug("对话已停止");
            return false;
        }
        
        // 如果达到最大轮次，停止
        if (currentRound >= maxRounds) {
            log.debug("达到最大对话轮次: {}，停止对话", maxRounds);
            return false;
        }
        
        // 如果有足够的行动项，可以停止
        if (actionItems.size() >= 3) {
            log.debug("已有足够的行动项 ({} >= 3)，可以停止对话", actionItems.size());
            return false;
        }
        
        // 如果有足够的关键洞察，可以停止
        if (keyInsights.size() >= 5) {
            log.debug("已有足够的关键洞察 ({} >= 5)，可以停止对话", keyInsights.size());
            return false;
        }
        
        log.debug("对话继续进行中");
        return true;
    }
    
    /**
     * 进入下一轮对话
     */
    public void nextRound() {
        currentRound++;
        log.debug("进入第 {} 轮对话", currentRound);
    }
    
    /**
     * 获取对话总结
     */
    public String getDialogueSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("多轮对话总结:\n");
        summary.append("当前轮次: ").append(currentRound).append("/").append(maxRounds).append("\n");
        summary.append("对话状态: ").append(isActive ? "活跃" : "结束").append("\n");
        summary.append("关键洞察数量: ").append(keyInsights.size()).append("\n");
        summary.append("行动项数量: ").append(actionItems.size()).append("\n\n");
        
        summary.append("主要关键洞察:\n");
        for (int i = 0; i < Math.min(keyInsights.size(), 3); i++) {
            summary.append("- ").append(keyInsights.get(i)).append("\n");
        }
        
        summary.append("\n主要行动项:\n");
        for (int i = 0; i < Math.min(actionItems.size(), 3); i++) {
            summary.append("- ").append(actionItems.get(i)).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 获取最新对话内容
     */
    public String getLatestDialogue() {
        if (dialogueHistory.isEmpty()) {
            return "";
        }
        return dialogueHistory.get(dialogueHistory.size() - 1);
    }
    
    /**
     * 获取对话历史（最近N条）
     */
    public List<String> getRecentDialogue(int count) {
        int startIndex = Math.max(0, dialogueHistory.size() - count);
        return dialogueHistory.subList(startIndex, dialogueHistory.size());
    }
    
    /**
     * 获取当前对话状态
     */
    public String getDialogueStatus() {
        return String.format("对话状态: %s, 轮次: %d/%d, 洞察: %d, 行动: %d",
                isActive ? "活跃" : "结束", currentRound, maxRounds, keyInsights.size(), actionItems.size());
    }
    
    /**
     * 获取对话历史
     */
    public List<String> getDialogueHistory() {
        return dialogueHistory;
    }
    
    /**
     * 获取关键洞察列表
     */
    public List<String> getKeyInsights() {
        return keyInsights;
    }
    
    /**
     * 重置对话
     */
    public void reset() {
        dialogueHistory.clear();
        keyInsights.clear();
        actionItems.clear();
        currentRound = 0;
        dialogueContext = "";
        currentSpeaker = "";
        isActive = true;
        log.debug("多轮对话已重置");
    }
    
    /**
     * 停止对话
     */
    public void stopDialogue() {
        this.isActive = false;
        log.debug("多轮对话已手动停止");
    }
    
    /**
     * 设置最大对话轮次
     */
    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
        log.debug("设置最大对话轮次: {}", maxRounds);
    }
    
    /**
     * 获取当前对话轮次
     */
    public int getCurrentRound() {
        return currentRound;
    }
    
    /**
     * 获取行动项列表
     */
    public List<String> getActionItems() {
        return actionItems;
    }
    
    /**
     * 获取对话统计信息
     */
    public String getDialogueStats() {
        return String.format("轮次: %d/%d, 洞察: %d, 行动: %d, 历史: %d条",
                currentRound, maxRounds, keyInsights.size(), actionItems.size(), dialogueHistory.size());
    }
}