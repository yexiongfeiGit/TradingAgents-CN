package com.tradingagents.agent.analyst;

import com.tradingagents.agent.base.BaseAgent;
import com.tradingagents.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 新闻分析师智能体
 * 专注于公司新闻和行业动态分析
 */
@Slf4j
@Component
public class NewsAnalyst extends BaseAgent {
    

    public NewsAnalyst() {
        super("新闻分析师", "news_analyst");
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
        
        prompt.append("你是一个专业的新闻分析师，专注于公司新闻和行业动态分析。\n");
        prompt.append("你的任务是分析相关新闻、公司公告和行业动态，评估其对股价的潜在影响。\n");
        prompt.append("请基于最新的新闻报道、公司公告和行业信息进行深入分析。\n");
        prompt.append("分析应该包括：\n");
        prompt.append("1. 公司新闻分析（财报、并购、管理层变动等）\n");
        prompt.append("2. 行业动态分析（政策变化、行业趋势、竞争格局）\n");
        prompt.append("3. 宏观经济影响（利率、汇率、政策环境）\n");
        prompt.append("4. 新闻影响评估（短期、中期、长期影响）\n");
        prompt.append("5. 新闻可信度分析（来源可靠性、信息准确性）\n");
        prompt.append("请使用专业的新闻分析方法和影响评估模型。\n");
        
        return prompt.toString();
    }
    
    @Override
    protected String getUserPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请对以下股票进行新闻分析:\n");
        prompt.append("股票代码: ").append(state.getCompanyInfo().getStockCode()).append("\n");
        prompt.append("股票名称: ").append(state.getCompanyInfo().getStockName()).append("\n");
        prompt.append("市场类型: ").append(state.getCompanyInfo().getMarketType()).append("\n");
        
        // 添加市场和情绪分析作为上下文
        if (state.getMarketAnalysis() != null && !state.getMarketAnalysis().isEmpty()) {
            prompt.append("\n市场分析背景:\n").append(state.getMarketAnalysis()).append("\n");
        }
        
        if (state.getSentimentReport() != null && !state.getSentimentReport().isEmpty()) {
            prompt.append("\n情绪分析背景:\n").append(state.getSentimentReport()).append("\n");
        }
        
        // 添加历史消息作为上下文
        if (!state.getMessages().isEmpty()) {
            prompt.append("\n历史新闻分析:\n");
            for (int i = 0; i < Math.min(state.getMessages().size(), 2); i++) {
                prompt.append("- ").append(state.getMessages().get(i)).append("\n");
            }
        }
        
        prompt.append("\n请提供详细的新闻分析报告:\n");
        prompt.append("1. 最新公司新闻和公告\n");
        prompt.append("2. 相关行业动态和政策变化\n");
        prompt.append("3. 宏观经济环境影响\n");
        prompt.append("4. 新闻对股价的短期影响评估（-100到+100，负值表示看跌，正值表示看涨）\n");
        prompt.append("5. 新闻对股价的中期影响评估（-100到+100）\n");
        prompt.append("6. 新闻对股价的长期影响评估（-100到+100）\n");
        prompt.append("7. 新闻可信度评估（1-10分，10分为最可信）\n");
        prompt.append("8. 关键新闻事件的时间线和重要性排序\n");
        prompt.append("\n请提供具体的影响评分和详细的分析解释。");
        
        return prompt.toString();
    }
    
    @Override
    protected AgentState processResponse(String response, AgentState state) {
        log.debug("{} 处理AI响应，长度: {}", agentName, response.length());
        
        // 格式化响应
        String formattedResponse = llmService.formatResponse(response);
        
        // 更新状态
        state.setNewsReport(formattedResponse);
        
        // 添加消息到历史记录
        updateState(state, formattedResponse);
        
        return state;
    }
}