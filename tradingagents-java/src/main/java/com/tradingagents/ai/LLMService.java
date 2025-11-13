package com.tradingagents.ai;

import com.tradingagents.model.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * LLM服务类 - 简化版本，不依赖Spring AI
 */
@Service
public class LLMService {
    
    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    @Autowired
    private LLMManager llmManager;

    /**
     * 生成响应 - 模拟实现
     */
    public String generateResponse(String prompt, String model) {
        try {
            log.info("生成LLM响应，模型: {}, 提示长度: {}", model, prompt.length());
            
            // 模拟AI响应
            String response = simulateAIResponse(prompt, model);
            
            log.info("LLM响应生成完成，响应长度: {}", response.length());
            return response;
            
        } catch (Exception e) {
            log.error("生成LLM响应失败", e);
            throw new RuntimeException("生成LLM响应失败", e);
        }
    }

    /**
     * 生成响应 - 带系统提示
     */
    public String generateResponse(String systemPrompt, String userPrompt, String model) {
        try {
            log.info("生成LLM响应，模型: {}, 系统提示长度: {}, 用户提示长度: {}", 
                    model, systemPrompt.length(), userPrompt.length());
            
            // 模拟AI响应
            String fullPrompt = systemPrompt + "\n\n用户问题: " + userPrompt;
            String response = simulateAIResponse(fullPrompt, model);
            
            log.info("LLM响应生成完成，响应长度: {}", response.length());
            return response;
            
        } catch (Exception e) {
            log.error("生成LLM响应失败", e);
            throw new RuntimeException("生成LLM响应失败", e);
        }
    }

    /**
     * 模拟AI响应
     */
    private String simulateAIResponse(String prompt, String model) {
        // 根据模型类型返回不同的模拟响应
        if (model.contains("claude")) {
            return simulateClaudeResponse(prompt);
        } else if (model.contains("gpt")) {
            return simulateGPTResponse(prompt);
        } else {
            return simulateGenericResponse(prompt);
        }
    }

    /**
     * 模拟Claude响应
     */
    private String simulateClaudeResponse(String prompt) {
        return "基于我的分析，这是一个专业的股票分析报告。\n\n" +
               "关键观察点：\n" +
               "1. 技术指标显示当前趋势\n" +
               "2. 基本面数据表现良好\n" +
               "3. 市场情绪相对积极\n" +
               "4. 风险因素需要关注\n\n" +
               "建议：谨慎乐观，建议持续关注市场动态。\n\n" +
               "（模拟Claude响应）";
    }

    /**
     * 模拟GPT响应
     */
    private String simulateGPTResponse(String prompt) {
        return "根据我的分析，提供以下投资建议：\n\n" +
               "**技术分析**：\n" +
               "- 当前价格处于合理区间\n" +
               "- 成交量保持稳定\n" +
               "- 技术指标显示中性偏强\n\n" +
               "**基本面分析**：\n" +
               "- 财务状况健康\n" +
               "- 行业前景良好\n" +
               "- 估值相对合理\n\n" +
               "**风险评估**：\n" +
               "- 市场风险：中等\n" +
               "- 个股风险：较低\n" +
               "- 建议仓位：适中\n\n" +
               "（模拟GPT响应）";
    }

    /**
     * 模拟通用响应
     */
    private String simulateGenericResponse(String prompt) {
        return "这是一个基于AI的股票分析报告。\n\n" +
               "分析要点：\n" +
               "• 当前市场表现\n" +
               "• 技术指标分析\n" +
               "• 基本面评估\n" +
               "• 风险提示\n\n" +
               "投资建议：建议投资者在充分了解风险的基础上做出投资决策。\n\n" +
               "（模拟AI响应）";
    }

    /**
     * 根据智能体类型选择模型
     */
    public String selectModelByAgent(String agentType) {
        switch (agentType.toLowerCase()) {
            case "market_analyst":
                return "openai/gpt-4o-mini";
            case "sentiment_analyst":
                return "openai/gpt-4o-mini";
            case "news_analyst":
                return "openai/gpt-4o-mini";
            case "fundamentals_analyst":
                return "openai/gpt-4o";
            case "bull_researcher":
                return "anthropic/claude-3-sonnet-20240229";
            case "bear_researcher":
                return "anthropic/claude-3-sonnet-20240229";
            case "risk_manager":
                return "openai/gpt-4o";
            case "portfolio_manager":
                return "openai/gpt-4o";
            default:
                return "openai/gpt-4o-mini";
        }
    }

    /**
     * 获取模型信息
     */
    public LLMManager.ModelInfo getModelInfo(String provider, String model) {
        return llmManager.getModelInfo(provider, model);
    }

    /**
     * 获取所有支持的模型
     */
    public Map<String, LLMManager.ModelInfo> getAllSupportedModels() {
        return llmManager.getAllSupportedModels();
    }

    /**
     * 检查模型是否支持函数调用
     */
    public boolean supportsFunctionCalling(String provider, String model) {
        return llmManager.supportsFunctionCalling(provider, model);
    }

    /**
     * 检查模型是否支持流式调用
     */
    public boolean supportsStreaming(String provider, String model) {
        return llmManager.supportsStreaming(provider, model);
    }

    /**
     * 格式化模型响应
     */
    public String formatResponse(String response) {
        if (response == null) {
            return "";
        }
        
        // 移除多余的空白字符
        response = response.trim();
        
        // 如果响应以代码块开始和结束，移除代码块标记
        if (response.startsWith("```") && response.endsWith("```")) {
            int startIndex = response.indexOf('\n');
            int endIndex = response.lastIndexOf("```");
            if (startIndex > 0 && endIndex > startIndex) {
                response = response.substring(startIndex + 1, endIndex).trim();
            }
        }
        
        return response;
    }

    /**
     * 创建带有上下文的提示
     */
    public String createContextualPrompt(String basePrompt, Map<String, Object> context) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // 添加上下文信息
        if (context != null && !context.isEmpty()) {
            promptBuilder.append("上下文信息:\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                promptBuilder.append("- ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append("\n");
            }
            promptBuilder.append("\n");
        }
        
        // 添加基础提示
        promptBuilder.append(basePrompt);
        
        return promptBuilder.toString();
    }

    /**
     * 创建智能体特定的系统提示
     */
    public String createAgentSystemPrompt(String agentType, String stockCode, String stockName) {
        StringBuilder systemPrompt = new StringBuilder();
        
        systemPrompt.append("你是一个专业的股票分析智能体。\n");
        systemPrompt.append("股票代码: ").append(stockCode).append("\n");
        systemPrompt.append("股票名称: ").append(stockName).append("\n");
        
        switch (agentType.toLowerCase()) {
            case "market_analyst":
                systemPrompt.append("你是一个专业的市场分析师，专注于技术分析和市场趋势分析。\n");
                systemPrompt.append("请提供客观、准确的市场分析，包括技术指标、价格走势和交易量分析。\n");
                break;
            case "sentiment_analyst":
                systemPrompt.append("你是一个专业的情绪分析师，专注于社交媒体和新闻情绪分析。\n");
                systemPrompt.append("请分析市场情绪、投资者情绪和媒体情绪，提供情绪指标和趋势分析。\n");
                break;
            case "news_analyst":
                systemPrompt.append("你是一个专业的新闻分析师，专注于公司新闻和行业动态分析。\n");
                systemPrompt.append("请分析相关新闻、公司公告和行业动态，评估其对股价的潜在影响。\n");
                break;
            case "fundamentals_analyst":
                systemPrompt.append("你是一个专业的基本面分析师，专注于公司财务和基本面分析。\n");
                systemPrompt.append("请提供深入的基本面分析，包括财务指标、估值分析和业务分析。\n");
                break;
            case "bull_researcher":
                systemPrompt.append("你是一个专业的看涨研究员，专注于寻找投资机会和积极因素。\n");
                systemPrompt.append("请提供详细的看涨分析，包括增长潜力、竞争优势和投资亮点。\n");
                break;
            case "bear_researcher":
                systemPrompt.append("你是一个专业的看跌研究员，专注于风险识别和负面因素分析。\n");
                systemPrompt.append("请提供详细的看跌分析，包括风险因素、潜在问题和负面趋势。\n");
                break;
            case "risk_manager":
                systemPrompt.append("你是一个专业的风险管理师，专注于投资风险评估和控制。\n");
                systemPrompt.append("请提供全面的风险分析，包括市场风险、信用风险和操作风险。\n");
                break;
            case "portfolio_manager":
                systemPrompt.append("你是一个专业的投资组合经理，专注于资产配置和投资决策。\n");
                systemPrompt.append("请提供专业的投资建议，包括买卖建议、目标价格和风险评估。\n");
                break;
            default:
                systemPrompt.append("你是一个专业的股票分析师，提供客观、准确的股票分析。\n");
                break;
        }
        
        systemPrompt.append("请确保分析的专业性、准确性和实用性。\n");
        
        return systemPrompt.toString();
    }
}