package com.tradingagents.agent.researcher;

import com.tradingagents.agent.base.BaseAgent;
import com.tradingagents.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 空头研究员智能体
 * 专注于风险识别和负面因素分析
 */
@Slf4j
@Component
public class BearResearcher extends BaseAgent {
    
    private static final Logger log = LoggerFactory.getLogger(BearResearcher.class);
    
    public BearResearcher() {
        super("空头研究员", "bear_researcher");
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
        
        prompt.append("你是一个专业的空头研究员，专注于风险识别和负面因素分析。\n");
        prompt.append("你的任务是提供详细的看跌分析，包括风险因素、潜在问题和负面趋势。\n");
        prompt.append("请基于基本面分析、行业趋势、市场情绪和技术指标寻找风险因素。\n");
        prompt.append("分析应该包括：\n");
        prompt.append("1. 财务风险分析（债务风险、现金流风险、盈利风险）\n");
        prompt.append("2. 业务风险识别（竞争风险、技术风险、监管风险）\n");
        prompt.append("3. 行业风险和挑战（行业衰退、政策变化、技术颠覆）\n");
        prompt.append("4. 估值风险（高估风险、泡沫风险、估值收缩风险）\n");
        prompt.append("5. 管理层风险（治理风险、战略风险、道德风险）\n");
        prompt.append("6. 市场风险（系统性风险、流动性风险、波动性风险）\n");
        prompt.append("7. 负面催化剂识别（业绩下滑、竞争加剧、政策收紧）\n");
        prompt.append("请客观、深入地分析风险因素，避免过度悲观。\n");
        
        return prompt.toString();
    }
    
    @Override
    protected String getUserPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请对以下股票进行空头分析，识别风险因素和负面因素:\n");
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
        
        prompt.append("\n请提供详细的空头分析报告:\n");
        prompt.append("1. 财务风险等级（1-10分，10分为最高风险）\n");
        prompt.append("2. 业务风险等级（1-10分）\n");
        prompt.append("3. 行业风险等级（1-10分）\n");
        prompt.append("4. 估值风险等级（1-10分）\n");
        prompt.append("5. 管理层风险等级（1-10分）\n");
        prompt.append("6. 市场风险等级（1-10分）\n");
        prompt.append("7. 主要风险因素总结\n");
        prompt.append("8. 负面催化剂和时间表\n");
        prompt.append("9. 目标价位和下跌空间\n");
        prompt.append("10. 投资建议（强烈卖出/卖出/观望）\n");
        prompt.append("11. 风险规避建议\n");
        prompt.append("\n请基于现有分析，客观、深入地识别风险因素和负面因素。");
        
        return prompt.toString();
    }
    
    @Override
    protected AgentState processResponse(String response, AgentState state) {
        log.debug("{} 处理AI响应，长度: {}", agentName, response.length());
        
        // 格式化响应
        String formattedResponse = llmService.formatResponse(response);
        
        // 更新投资辩论状态
        if (state.getInvestmentDebate() != null) {
            state.getInvestmentDebate().addBearArgument(formattedResponse);
            state.getInvestmentDebate().incrementBearSpeeches();
        }
        
        // 添加消息到历史记录
        updateState(state, formattedResponse);
        
        return state;
    }
}