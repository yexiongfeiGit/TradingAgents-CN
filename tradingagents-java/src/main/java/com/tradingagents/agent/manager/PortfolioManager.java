package com.tradingagents.agent.manager;

import com.tradingagents.agent.base.BaseAgent;
import com.tradingagents.model.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 投资组合经理智能体
 * 专注于资产配置和投资决策
 */
@Component
public class PortfolioManager extends BaseAgent {
    
    private static final Logger log = LoggerFactory.getLogger(PortfolioManager.class);
    
    public PortfolioManager() {
        super("投资组合经理", "portfolio_manager");
        // 使用GPT-4o进行专业投资决策
        setDefaultModel("openai", "gpt-4o");
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
        
        prompt.append("你是一个专业的投资组合经理，专注于资产配置和投资决策。\n");
        prompt.append("你的任务是提供专业的投资建议，包括买卖建议、目标价格和风险评估。\n");
        prompt.append("请基于技术分析、基本面分析、情绪分析、新闻分析和风险管理进行综合投资决策。\n");
        prompt.append("分析应该包括：\n");
        prompt.append("1. 综合投资评级（基于多维度分析）\n");
        prompt.append("2. 目标价格和时间框架\n");
        prompt.append("3. 投资建议（强烈买入/买入/持有/卖出/强烈卖出）\n");
        prompt.append("4. 投资组合配置建议（权重分配）\n");
        prompt.append("5. 风险调整后的收益预期\n");
        prompt.append("6. 投资时间框架（短期/中期/长期）\n");
        prompt.append("7. 退出策略和止损设置\n");
        prompt.append("8. 投资组合风险贡献分析\n");
        prompt.append("请使用专业的投资分析和组合管理方法。\n");
        
        return prompt.toString();
    }
    
    @Override
    protected String getUserPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请基于以下综合分析制定投资决策:\n");
        prompt.append("股票代码: ").append(state.getCompanyInfo().getStockCode()).append("\n");
        prompt.append("股票名称: ").append(state.getCompanyInfo().getStockName()).append("\n");
        prompt.append("市场类型: ").append(state.getCompanyInfo().getMarketType()).append("\n");
        
        // 添加所有分析师的分析作为决策依据
        if (state.getMarketAnalysis() != null && !state.getMarketAnalysis().isEmpty()) {
            prompt.append("\n市场分析:\n").append(state.getMarketAnalysis()).append("\n");
        }
        
        if (state.getSentimentReport() != null && !state.getSentimentReport().isEmpty()) {
            prompt.append("\n情绪分析:\n").append(state.getSentimentReport()).append("\n");
        }
        
        if (state.getNewsReport() != null && !state.getNewsReport().isEmpty()) {
            prompt.append("\n新闻分析:\n").append(state.getNewsReport()).append("\n");
        }
        
        if (state.getFundamentalsReport() != null && !state.getFundamentalsReport().isEmpty()) {
            prompt.append("\n基本面分析:\n").append(state.getFundamentalsReport()).append("\n");
        }
        
        // 添加投资辩论历史
        if (state.getInvestmentDebateState() != null && !state.getInvestmentDebateState().getDebateHistory().isEmpty()) {
            prompt.append("\n投资辩论历史:\n");
            for (String debate : state.getInvestmentDebateState().getDebateHistory()) {
                prompt.append("- ").append(debate).append("\n");
            }
            prompt.append("\n多头观点数量: ").append(state.getInvestmentDebateState().getBullSpeeches()).append("\n");
            prompt.append("空头观点数量: ").append(state.getInvestmentDebateState().getBearSpeeches()).append("\n");
        }
        
        // 添加风险讨论
        if (state.getRiskDiscussionState() != null && !state.getRiskDiscussionState().getRiskAnalyses().isEmpty()) {
            prompt.append("\n风险分析:\n");
            for (String risk : state.getRiskDiscussionState().getRiskAnalyses()) {
                prompt.append("- ").append(risk).append("\n");
            }
        }
        
        prompt.append("\n请提供详细的投资决策报告:\n");
        prompt.append("1. 综合投资评级（强烈买入/买入/持有/卖出/强烈卖出）\n");
        prompt.append("2. 投资信心度（1-10分，10分为最高信心）\n");
        prompt.append("3. 目标价位（12个月目标价）\n");
        prompt.append("4. 上涨空间百分比\n");
        prompt.append("5. 下跌风险百分比\n");
        prompt.append("6. 投资建议权重（0-100%，相对于投资组合）\n");
        prompt.append("7. 风险调整后的预期年化收益率\n");
        prompt.append("8. 投资时间框架（短期1-3个月/中期3-12个月/长期1年以上）\n");
        prompt.append("9. 止损价位和止损策略\n");
        prompt.append("10. 加仓和减仓策略\n");
        prompt.append("11. 投资组合风险贡献分析\n");
        prompt.append("12. 关键监控指标和预警机制\n");
        prompt.append("\n请基于综合分析，提供专业的投资决策建议。");
        
        return prompt.toString();
    }
    
    @Override
    protected AgentState processResponse(String response, AgentState state) {
        log.debug("{} 处理AI响应，长度: {}", agentName, response.length());
        
        // 格式化响应
        String formattedResponse = llmService.formatResponse(response);
        
        // 更新投资组合分析
        state.setPortfolioAnalysis(formattedResponse);
        
        // 添加消息到历史记录
        updateState(state, formattedResponse);
        
        return state;
    }
}