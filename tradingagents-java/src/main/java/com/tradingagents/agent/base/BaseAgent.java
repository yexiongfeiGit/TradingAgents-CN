package com.tradingagents.agent.base;

import com.tradingagents.ai.LLMService;
import com.tradingagents.model.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 基础智能体类
 * 所有智能体的基类，提供通用的AI调用功能
 */
public abstract class BaseAgent {
    
    private static final Logger log = LoggerFactory.getLogger(BaseAgent.class);
    
    @Autowired
    protected LLMService llmService;
    
    protected String agentName;
    protected String agentType;
    protected String defaultProvider;
    protected String defaultModel;
    
    public BaseAgent(String agentName, String agentType) {
        this.agentName = agentName;
        this.agentType = agentType;
        this.defaultProvider = "openai";
        this.defaultModel = "gpt-4o-mini";
    }
    
    /**
     * 智能体执行逻辑
     */
    public abstract AgentState execute(AgentState state);
    
    /**
     * 获取系统提示
     */
    protected abstract String getSystemPrompt(AgentState state);
    
    /**
     * 获取用户提示
     */
    protected abstract String getUserPrompt(AgentState state);
    
    /**
     * 处理AI响应
     */
    protected abstract AgentState processResponse(String response, AgentState state);
    
    /**
     * 调用AI模型
     */
    protected String callAI(String systemPrompt, String userPrompt) {
        log.debug("{} 调用AI模型: {}/{}", agentName, defaultProvider, defaultModel);
        
        // 构建完整提示
        String fullPrompt = buildFullPrompt(systemPrompt, userPrompt);
        
        try {
            String response = llmService.generateResponse(fullPrompt, defaultProvider + "/" + defaultModel);
            log.debug("{} AI调用完成，响应长度: {}", agentName, response.length());
            return response;
        } catch (Exception e) {
            log.error("{} AI调用失败: {}", agentName, e.getMessage(), e);
            throw new RuntimeException(agentName + " AI调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 流式调用AI模型
     */
    protected String streamAI(String systemPrompt, String userPrompt) {
        log.debug("{} 流式调用AI模型: {}/{}", agentName, defaultProvider, defaultModel);
        
        // 构建完整提示
        String fullPrompt = buildFullPrompt(systemPrompt, userPrompt);
        
        try {
            // 简化流式调用，直接返回生成的响应
            String response = llmService.generateResponse(fullPrompt, defaultProvider + "/" + defaultModel);
            log.debug("{} 流式AI调用完成，响应长度: {}", agentName, response.length());
            return response;
        } catch (Exception e) {
            log.error("{} 流式AI调用失败: {}", agentName, e.getMessage(), e);
            throw new RuntimeException(agentName + " 流式AI调用失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建完整提示
     */
    private String buildFullPrompt(String systemPrompt, String userPrompt) {
        StringBuilder prompt = new StringBuilder();
        
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            prompt.append("系统提示:\n").append(systemPrompt).append("\n\n");
        }
        
        prompt.append("用户提示:\n").append(userPrompt);
        
        return prompt.toString();
    }
    
    /**
     * 更新智能体状态
     */
    protected AgentState updateState(AgentState state, String response) {
        // 添加消息到状态
        AgentState.Message message = AgentState.Message.builder()
                .senderId(agentName)
                .content(response)
                .messageType("RESPONSE")
                .build();
        state.addMessage(message);
        
        // 增加工具调用计数
        state.incrementToolCallCount(agentType);
        
        return state;
    }
    
    /**
     * 获取当前工具调用次数
     */
    protected int getToolCallCount(AgentState state) {
        Map<String, Integer> toolCallCounts = (Map<String, Integer>) state.getSharedContextValue("toolCallCounts");
        if (toolCallCounts == null) {
            return 0;
        }
        return toolCallCounts.getOrDefault(agentType, 0);
    }
    
    /**
     * 检查是否达到工具调用限制
     */
    protected boolean isToolCallLimitReached(AgentState state, int limit) {
        int count = getToolCallCount(state);
        return count >= limit;
    }
    
    /**
     * 设置默认模型
     */
    public void setDefaultModel(String provider, String model) {
        this.defaultProvider = provider;
        this.defaultModel = model;
        log.info("{} 设置默认模型: {}/{}", agentName, provider, model);
    }
    
    /**
     * 获取智能体名称
     */
    public String getAgentName() {
        return agentName;
    }
    
    /**
     * 获取智能体类型
     */
    public String getAgentType() {
        return agentType;
    }
    
    /**
     * 获取默认提供商
     */
    public String getDefaultProvider() {
        return defaultProvider;
    }
    
    /**
     * 获取默认模型
     */
    public String getDefaultModel() {
        return defaultModel;
    }
}