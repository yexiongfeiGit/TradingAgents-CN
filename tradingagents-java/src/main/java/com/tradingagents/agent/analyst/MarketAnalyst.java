package com.tradingagents.agent.analyst;

import com.tradingagents.agent.base.BaseAgent;
import com.tradingagents.model.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 市场分析师智能体
 * 专注于技术分析和市场趋势分析
 */
@Slf4j
@Component
public class MarketAnalyst extends BaseAgent {
    
    private static final Logger log = LoggerFactory.getLogger(MarketAnalyst.class);
    
    public MarketAnalyst() {
        super("市场分析师", "market_analyst");
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
        
        prompt.append("你是一个专业的市场分析师，专注于技术分析和市场趋势分析。\n");
        prompt.append("你的任务是提供客观、准确的市场分析，包括技术指标、价格走势和交易量分析。\n");
        prompt.append("请基于最新的市场数据和技术指标进行分析。\n");
        prompt.append("分析应该包括：\n");
        prompt.append("1. 技术分析（移动平均线、RSI、MACD等）\n");
        prompt.append("2. 价格走势分析（支撑位、阻力位、趋势方向）\n");
        prompt.append("3. 交易量分析（成交量变化、量价关系）\n");
        prompt.append("4. 市场情绪指标（VIX、恐慌贪婪指数等）\n");
        prompt.append("5. 短期和中期趋势预测\n");
        prompt.append("请确保分析的专业性、准确性和实用性。\n");
        
        return prompt.toString();
    }
    
    @Override
    protected String getUserPrompt(AgentState state) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请对以下股票进行市场分析:\n");
        prompt.append("股票代码: ").append(state.getCompanyInfo().getStockCode()).append("\n");
        prompt.append("股票名称: ").append(state.getCompanyInfo().getStockName()).append("\n");
        prompt.append("市场类型: ").append(state.getCompanyInfo().getMarketType()).append("\n");
        
        // 添加历史消息作为上下文
        if (!state.getMessages().isEmpty()) {
            prompt.append("\n历史分析:\n");
            for (int i = 0; i < Math.min(state.getMessages().size(), 3); i++) {
                prompt.append("- ").append(state.getMessages().get(i)).append("\n");
            }
        }
        
        prompt.append("\n请提供详细的市场分析报告:\n");
        prompt.append("1. 当前技术指标分析\n");
        prompt.append("2. 价格走势和关键价位\n");
        prompt.append("3. 交易量和市场活跃度\n");
        prompt.append("4. 短期（1-2周）趋势预测\n");
        prompt.append("5. 中期（1-3个月）趋势预测\n");
        prompt.append("6. 关键风险点和机会\n");
        prompt.append("\n请确保分析基于最新的市场数据和技术指标。");
        
        return prompt.toString();
    }
    
    @Override
    protected AgentState processResponse(String response, AgentState state) {
        log.debug("{} 处理AI响应，长度: {}", agentName, response.length());
        
        // 格式化响应
        String formattedResponse = llmService.formatResponse(response);
        
        // 更新状态
        state.setMarketAnalysis(formattedResponse);
        
        // 添加消息到历史记录
        updateState(state, formattedResponse);
        
        return state;
    }
}