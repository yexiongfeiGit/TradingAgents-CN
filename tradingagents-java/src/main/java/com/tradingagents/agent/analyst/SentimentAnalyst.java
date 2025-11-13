package com.tradingagents.agent.analyst;

import com.tradingagents.agent.base.BaseAgent;
import com.tradingagents.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 情绪分析师智能体
 * 专注于社交媒体和新闻情绪分析
 */
@Slf4j
@Component
public class SentimentAnalyst extends BaseAgent {
    

    public SentimentAnalyst() {
        super("情绪分析师", "sentiment_analyst");
    }
    
    @Override
    public AgentState execute(AgentState state) {
        log.info("{} 开始执行分析: {}", agentName, state.getCompanyInfo().getStockCode());
        
        // 获取系统提示和用户提示
        String systemPrompt = getSystemPrompt(state);
        String userPrompt = getUserPrompt(state);
        
        // 调用AI进行分析
        String response = callAI(systemPrompt, userPrompt);
        
        // 处理响应并更新状态
        AgentState updatedState = processResponse(response, state);
        
        log.info("{} 分析完成: {}", agentName, state.getCompanyInfo().getStockCode());
        return updatedState;
    }
    
    @Override
    protected String getSystemPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个专业的情绪分析师，专注于社交媒体和新闻情绪分析。\n");
        prompt.append("你的任务是分析市场情绪、投资者情绪和媒体情绪，提供情绪指标和趋势分析。\n");
        prompt.append("请基于最新的社交媒体数据、新闻报道和市场评论进行分析。\n");
        prompt.append("分析应该包括：\n");
        prompt.append("1. 社交媒体情绪分析（微博、Twitter、论坛等）\n");
        prompt.append("2. 新闻情绪分析（财经媒体、分析师报告）\n");
        prompt.append("3. 投资者情绪指标（散户情绪、机构情绪）\n");
        prompt.append("4. 情绪变化趋势（短期、中期、长期）\n");
        prompt.append("5. 情绪与价格的相关性分析\n");
        prompt.append("请使用专业的情绪分析方法和量化指标。\n");
        
        return prompt.toString();
    }
    
    @Override
    protected String getUserPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请对以下股票进行情绪分析:\n");
        prompt.append("股票代码: ").append(state.getCompanyInfo().getStockCode()).append("\n");
        prompt.append("股票名称: ").append(state.getCompanyInfo().getStockName()).append("\n");
        prompt.append("市场类型: ").append(state.getCompanyInfo().getMarketType()).append("\n");
        
        // 添加市场分析作为上下文
        if (state.getMarketAnalysis() != null && !state.getMarketAnalysis().isEmpty()) {
            prompt.append("\n市场分析背景:\n").append(state.getMarketAnalysis()).append("\n");
        }
        
        // 添加历史消息作为上下文
        if (!state.getMessages().isEmpty()) {
            prompt.append("\n历史情绪分析:\n");
            for (int i = 0; i < Math.min(state.getMessages().size(), 2); i++) {
                prompt.append("- ").append(state.getMessages().get(i)).append("\n");
            }
        }
        
        prompt.append("\n请提供详细的情绪分析报告:\n");
        prompt.append("1. 社交媒体情绪指标（-100到+100，负值表示看跌，正值表示看涨）\n");
        prompt.append("2. 新闻情绪指标（-100到+100）\n");
        prompt.append("3. 投资者情绪指标（-100到+100）\n");
        prompt.append("4. 综合情绪得分（-100到+100）\n");
        prompt.append("5. 情绪变化趋势分析\n");
        prompt.append("6. 极端情绪事件识别\n");
        prompt.append("7. 情绪与价格走势的相关性\n");
        prompt.append("\n请提供具体的情绪数值和详细的分析解释。");
        
        return prompt.toString();
    }
    
    @Override
    protected AgentState processResponse(String response, AgentState state) {
        log.debug("{} 处理AI响应，长度: {}", agentName, response.length());
        
        // 格式化响应
        String formattedResponse = llmService.formatResponse(response);
        
        // 更新状态
        state.setSentimentReport(formattedResponse);
        
        // 添加消息到历史记录
        updateState(state, formattedResponse);
        
        return state;
    }
}