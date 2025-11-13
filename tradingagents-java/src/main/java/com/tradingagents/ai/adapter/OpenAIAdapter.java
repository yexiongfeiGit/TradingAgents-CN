package com.tradingagents.ai.adapter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI适配器实现
 */
@Slf4j
@Component
public class OpenAIAdapter implements LLMAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(OpenAIAdapter.class);
    
    private final OpenAiChatModel chatModel;
    
    @Value("${spring.ai.openai.chat.options.model:gpt-4}")
    private String defaultModel;
    
    private final Map<String, ModelInfo> supportedModels = new HashMap<>();
    
    public OpenAIAdapter(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
        initializeModels();
    }
    
    private void initializeModels() {
        // GPT-4系列
        supportedModels.put("gpt-4", new ModelInfo(
            "gpt-4", "GPT-4 - 强大的推理能力", 8192, true, true, 3.5,
            new String[]{"复杂推理", "专业分析", "高质量输出"}
        ));
        
        supportedModels.put("gpt-4-turbo", new ModelInfo(
            "gpt-4-turbo", "GPT-4 Turbo - 更快的响应速度", 128000, true, true, 2.0,
            new String[]{"快速响应", "实时分析", "长文本处理"}
        ));
        
        supportedModels.put("gpt-4o", new ModelInfo(
            "gpt-4o", "GPT-4o - 多模态全能模型", 128000, true, true, 1.5,
            new String[]{"多模态分析", "快速响应", "全能型"}
        ));
        
        // GPT-3.5系列
        supportedModels.put("gpt-3.5-turbo", new ModelInfo(
            "gpt-3.5-turbo", "GPT-3.5 Turbo - 经济实惠的选择", 16384, true, true, 1.0,
            new String[]{"经济型", "快速响应", "日常使用"}
        ));
        
        // 推理模型
        supportedModels.put("o1-preview", new ModelInfo(
            "o1-preview", "O1 Preview - 深度推理模型", 32768, false, false, 15.0,
            new String[]{"深度推理", "复杂问题", "科学研究"}
        ));
        
        supportedModels.put("o1-mini", new ModelInfo(
            "o1-mini", "O1 Mini - 轻量级推理模型", 65536, false, false, 8.0,
            new String[]{"推理分析", "中等复杂度", "平衡选择"}
        ));
    }
    
    @Override
    public ChatResponse invoke(Prompt prompt) {
        log.debug("调用OpenAI模型: {}", defaultModel);
        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            log.error("OpenAI调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        log.debug("流式调用OpenAI模型: {}", defaultModel);
        try {
            return chatModel.stream(prompt);
        } catch (Exception e) {
            log.error("OpenAI流式调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI流式调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String invokeString(String prompt) {
        log.debug("调用OpenAI字符串响应: {}", defaultModel);
        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("OpenAI字符串调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI字符串调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> streamString(String prompt) {
        log.debug("流式调用OpenAI字符串响应: {}", defaultModel);
        try {
            return chatModel.stream(new Prompt(prompt))
                    .map(response -> response.getResult().getOutput().getContent());
        } catch (Exception e) {
            log.error("OpenAI字符串流式调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI字符串流式调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getAdapterName() {
        return "openai";
    }
    
    @Override
    public Map<String, ModelInfo> getSupportedModels() {
        return new HashMap<>(supportedModels);
    }
    
    @Override
    public boolean supportsFunctionCalling() {
        ModelInfo modelInfo = getModelInfo(defaultModel);
        return modelInfo != null && modelInfo.isSupportsFunctionCalling();
    }
    
    @Override
    public boolean supportsStreaming() {
        ModelInfo modelInfo = getModelInfo(defaultModel);
        return modelInfo != null && modelInfo.isSupportsStreaming();
    }
    
    @Override
    public ModelInfo getModelInfo(String modelName) {
        return supportedModels.get(modelName);
    }
}