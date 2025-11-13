package com.tradingagents.ai.adapter;

import com.tradingagents.model.AgentState;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * LLM适配器接口
 * 统一不同AI模型提供商的调用接口
 */
public interface LLMAdapter {
    
    /**
     * 同步调用LLM
     */
    ChatResponse invoke(Prompt prompt);
    
    /**
     * 流式调用LLM
     */
    Flux<ChatResponse> stream(Prompt prompt);
    
    /**
     * 调用LLM并返回字符串内容
     */
    String invokeString(String prompt);
    
    /**
     * 流式调用LLM并返回字符串内容
     */
    Flux<String> streamString(String prompt);
    
    /**
     * 获取适配器名称
     */
    String getAdapterName();
    
    /**
     * 获取支持的模型列表
     */
    Map<String, ModelInfo> getSupportedModels();
    
    /**
     * 检查是否支持函数调用
     */
    boolean supportsFunctionCalling();
    
    /**
     * 检查是否支持流式调用
     */
    boolean supportsStreaming();
    
    /**
     * 获取模型信息
     */
    ModelInfo getModelInfo(String modelName);
    
    /**
     * 模型信息类
     */
    class ModelInfo {
        private final String modelName;
        private final String description;
        private final int contextLength;
        private final boolean supportsFunctionCalling;
        private final boolean supportsStreaming;
        private final double avgResponseTime;
        private final String[] recommendedFor;
        
        public ModelInfo(String modelName, String description, int contextLength, 
                        boolean supportsFunctionCalling, boolean supportsStreaming, 
                        double avgResponseTime, String[] recommendedFor) {
            this.modelName = modelName;
            this.description = description;
            this.contextLength = contextLength;
            this.supportsFunctionCalling = supportsFunctionCalling;
            this.supportsStreaming = supportsStreaming;
            this.avgResponseTime = avgResponseTime;
            this.recommendedFor = recommendedFor;
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public String getDescription() { return description; }
        public int getContextLength() { return contextLength; }
        public boolean isSupportsFunctionCalling() { return supportsFunctionCalling; }
        public boolean isSupportsStreaming() { return supportsStreaming; }
        public double getAvgResponseTime() { return avgResponseTime; }
        public String[] getRecommendedFor() { return recommendedFor; }
    }
}