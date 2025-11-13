package com.tradingagents.agent.researcher;

import com.tradingagents.agent.base.BaseAgent;
import com.tradingagents.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 多头研究员智能体
 * 专注于寻找投资机会和积极因素
 */
@Slf4j
@Component
public class BullResearcher extends BaseAgent {
    
    private static final Logger log = LoggerFactory.getLogger(BullResearcher.class);
    
    public BullResearcher() {
        super("多头研究员", "bull_researcher");
        // 使用Claude模型进行深度分析
        setDefaultModel("anthropic", "claude-3-sonnet-20240229");
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
        
        prompt.append("你是一个专业的多头研究员，专注于寻找投资机会和积极因素。\n");
        prompt.append("你的任务是提供详细的看涨分析，包括增长潜力、竞争优势和投资亮点。\n");
        prompt.append("请基于基本面分析、行业趋势、市场情绪和技术指标寻找积极因素。\n");
        prompt.append("分析应该包括：\n");
        prompt.append("1. 增长潜力分析（营收增长、市场扩张、新产品）\n");
        prompt.append("2. 竞争优势识别（护城河、技术优势、品牌优势）\n");
        prompt.append("3. 行业趋势和机遇（行业发展、政策支持、技术变革）\n");
        prompt.append("4. 财务亮点（盈利增长、现金流改善、负债优化）\n");
        prompt.append("5. 估值吸引力（相对估值、历史估值、增长潜力）\n");
        prompt.append("6. 催化剂识别（即将发布的产品、政策变化、行业事件）\n");
        prompt.append("请客观、深入地分析积极因素，避免过度乐观。\n");
        
        return prompt.toString();
    }
    
    @Override
    protected String getUserPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请对以下股票进行多头分析，寻找投资机会和积极因素:\n");
        prompt.append("股票代码: ").append(state.getCompanyInfo().getStockCode()).append("\n");
        prompt.append("股票名称: ").append(state.getCompanyInfo().getStockName()).append("\n");
        prompt.append("市场类型: ").append(state.getCompanyInfo().getMarketType()).append("\n");
        
        // 添加其他分析师的分析作为上下文
        if (state.getMarketAnalysis() != null && !state.getMarketAnalysis().isEmpty()) {
            prompt.append("\n市场分析背景:\n").append(state.getMarketAnalysis()).append("\n");
        }
        
        if (state.getSentimentReport() != null && !state.getSentimentReport().isEmpty()) {
            prompt.append("\n情绪分析背景:\n").append(state.getSentimentReport()).append("\n");
        }
        
        if (state.getNewsReport() != null && !state.getNewsReport().isEmpty()) {
            prompt.append("\n新闻分析背景:\n").append(state.getNewsReport()).append("\n");
        }
        
        if (state.getFundamentalsReport() != null && !state.getFundamentalsReport().isEmpty()) {
            prompt.append("\n基本面分析背景:\n").append(state.getFundamentalsReport()).append("\n");
        }
        
        // 添加投资辩论历史
        if (state.getInvestmentDebate() != null && !state.getInvestmentDebate().getDebateHistory().isEmpty()) {
            prompt.append("\n投资辩论历史:\n");
            for (String debate : state.getInvestmentDebate().getDebateHistory()) {
                prompt.append("- ").append(debate).append("\n");
            }
        }
        
        prompt.append("\n请提供详细的多头分析报告:\n");
        prompt.append("1. 增长潜力评估（1-10分，10分为最高）\n");
        prompt.append("2. 竞争优势强度（1-10分）\n");
        prompt.append("3. 行业机遇大小（1-10分）\n");
        prompt.append("4. 财务亮点总结\n");
        prompt.append("5. 估值吸引力评估（1-10分）\n");
        prompt.append("6. 主要投资催化剂和时间表\n");
        prompt.append("7. 目标价位和上涨空间\n");
        prompt.append("8. 投资建议（强烈买入/买入/观望）\n");
        prompt.append("9. 投资时间框架建议\n");
        prompt.append("\n请基于现有分析，客观、深入地寻找积极因素和投资机会。");
        
        return prompt.toString();
    }
    
    @Override
    protected AgentState processResponse(String response, AgentState state) {
        log.debug("{} 处理AI响应，长度: {}", agentName, response.length());
        
        // 格式化响应
        String formattedResponse = llmService.formatResponse(response);
        
        // 更新投资辩论状态
        if (state.getInvestmentDebate() != null) {
            state.getInvestmentDebate().addBullArgument(formattedResponse);
            state.getInvestmentDebate().incrementBullSpeeches();
        }
        
        // 添加消息到历史记录
        updateState(state, formattedResponse);
        
        return state;
    }
}