package com.tradingagents.agent.manager;

import com.tradingagents.agent.base.BaseAgent;
import com.tradingagents.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 风险管理师智能体
 * 专注于投资风险评估和控制
 */
@Slf4j
@Component
public class RiskManager extends BaseAgent {
    
    private static final Logger log = LoggerFactory.getLogger(RiskManager.class);
    
    public RiskManager() {
        super("风险管理师", "risk_manager");
        // 使用GPT-4o进行专业风险分析
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
        
        prompt.append("你是一个专业的风险管理师，专注于投资风险评估和控制。\n");
        prompt.append("你的任务是提供全面的风险分析，包括市场风险、信用风险和操作风险。\n");
        prompt.append("请基于技术分析、基本面分析、情绪分析和新闻分析进行综合风险评估。\n");
        prompt.append("分析应该包括：\n");
        prompt.append("1. 市场风险（系统性风险、波动性风险、流动性风险）\n");
        prompt.append("2. 信用风险（违约风险、评级风险、信用利差风险）\n");
        prompt.append("3. 操作风险（交易风险、结算风险、技术风险）\n");
        prompt.append("4. 行业风险（周期性风险、竞争风险、监管风险）\n");
        prompt.append("5. 公司特定风险（经营风险、财务风险、管理风险）\n");
        prompt.append("6. 估值风险（高估风险、估值收缩风险）\n");
        prompt.append("7. 投资组合风险（集中度风险、相关性风险）\n");
        prompt.append("请使用专业的风险管理模型和量化方法。\n");
        
        return prompt.toString();
    }
    
    @Override
    protected String getUserPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请对以下股票进行综合风险评估:\n");
        prompt.append("股票代码: ").append(state.getCompanyInfo().getStockCode()).append("\n");
        prompt.append("股票名称: ").append(state.getCompanyInfo().getStockName()).append("\n");
        prompt.append("市场类型: ").append(state.getCompanyInfo().getMarketType()).append("\n");
        
        // 添加所有分析师的分析作为上下文
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
        
        prompt.append("\n请提供详细的风险管理报告:\n");
        prompt.append("1. 综合风险评级（1-10分，10分为最高风险）\n");
        prompt.append("2. 市场风险等级（1-10分）\n");
        prompt.append("3. 信用风险等级（1-10分）\n");
        prompt.append("4. 操作风险等级（1-10分）\n");
        prompt.append("5. 行业风险等级（1-10分）\n");
        prompt.append("6. 公司特定风险等级（1-10分）\n");
        prompt.append("7. 估值风险等级（1-10分）\n");
        prompt.append("8. 投资组合风险（集中度、相关性）\n");
        prompt.append("9. 风险调整后的投资建议\n");
        prompt.append("10. 风险控制和缓释建议\n");
        prompt.append("11. 风险监控指标和预警阈值\n");
        prompt.append("12. 压力测试和情景分析结果\n");
        prompt.append("\n请提供具体的风险评分和详细的风险管理建议。");
        
        return prompt.toString();
    }
    
    @Override
    protected AgentState processResponse(String response, AgentState state) {
        log.debug("{} 处理AI响应，长度: {}", agentName, response.length());
        
        // 格式化响应
        String formattedResponse = llmService.formatResponse(response);
        
        // 更新风险讨论状态
        if (state.getRiskDiscussion() != null) {
            state.getRiskDiscussion().addRiskAnalysis(formattedResponse);
            state.getRiskDiscussion().incrementRiskSpeeches();
        }
        
        // 添加消息到历史记录
        updateState(state, formattedResponse);
        
        return state;
    }
}