package com.tradingagents.agent.analyst;

import com.tradingagents.agent.base.BaseAgent;
import com.tradingagents.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 基本面分析师智能体
 * 专注于公司财务和基本面分析
 */
@Slf4j
@Component
public class FundamentalsAnalyst extends BaseAgent {
    

    public FundamentalsAnalyst() {
        super("基本面分析师", "fundamentals_analyst");
        // 基本面分析需要更强的模型
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
        
        prompt.append("你是一个专业的基本面分析师，专注于公司财务和基本面分析。\n");
        prompt.append("你的任务是提供深入的基本面分析，包括财务指标、估值分析和业务分析。\n");
        prompt.append("请基于最新的财务报表、行业数据和宏观经济环境进行深入分析。\n");
        prompt.append("分析应该包括：\n");
        prompt.append("1. 财务指标分析（盈利能力、偿债能力、运营效率）\n");
        prompt.append("2. 估值分析（PE、PB、EV/EBITDA等）\n");
        prompt.append("3. 业务模式和竞争优势分析\n");
        prompt.append("4. 管理层质量和公司治理评估\n");
        prompt.append("5. 行业地位和市场份额分析\n");
        prompt.append("6. 风险因素识别和评估\n");
        prompt.append("请使用专业的财务分析方法和估值模型。\n");
        
        return prompt.toString();
    }
    
    @Override
    protected String getUserPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请对以下股票进行基本面分析:\n");
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
        
        // 添加历史消息作为上下文
        if (!state.getMessages().isEmpty()) {
            prompt.append("\n历史基本面分析:\n");
            for (int i = 0; i < Math.min(state.getMessages().size(), 2); i++) {
                prompt.append("- ").append(state.getMessages().get(i)).append("\n");
            }
        }
        
        prompt.append("\n请提供详细的基本面分析报告:\n");
        prompt.append("1. 财务指标分析（ROE、ROA、毛利率、净利率等）\n");
        prompt.append("2. 估值水平评估（当前PE、PB与历史平均和行业平均比较）\n");
        prompt.append("3. 成长性分析（营收增长率、利润增长率、现金流增长率）\n");
        prompt.append("4. 财务健康度评估（资产负债率、流动比率、速动比率）\n");
        prompt.append("5. 业务模式和竞争优势分析\n");
        prompt.append("6. 管理层质量和公司治理评分（1-10分）\n");
        prompt.append("7. 行业地位和市场份额\n");
        prompt.append("8. 主要风险因素识别\n");
        prompt.append("9. 投资建议和目标价位\n");
        prompt.append("\n请提供具体的财务数据和详细的分析解释。");
        
        return prompt.toString();
    }
    
    @Override
    protected AgentState processResponse(String response, AgentState state) {
        log.debug("{} 处理AI响应，长度: {}", agentName, response.length());
        
        // 格式化响应
        String formattedResponse = llmService.formatResponse(response);
        
        // 更新状态
        state.setFundamentalsReport(formattedResponse);
        
        // 添加消息到历史记录
        updateState(state, formattedResponse);
        
        return state;
    }
}