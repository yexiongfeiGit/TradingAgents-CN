package com.tradingagents.workflow;

import com.tradingagents.debate.AgentCoordinator;
import com.tradingagents.debate.InvestmentDebate;
import com.tradingagents.debate.MultiRoundDialogue;
import com.tradingagents.debate.RiskDiscussion;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 投资分析工作流
 * 管理完整的投资分析流程
 * 
 * ⚠️ 重要说明：当前实现为演示版本
 * - WorkflowEngine.executeStepLogic() 为模拟实现，返回预设的模拟结果
 * - 所有分析类型参数（如"technical"、"sentiment"等）存储在步骤参数中但未实际使用
 * - 智能体调用机制尚未实现，当前仅通过AgentManagementController管理智能体状态
 * - 实际智能体执行逻辑待后续版本实现
 */
@Data
@Slf4j
public class InvestmentAnalysisWorkflow {
    

    private WorkflowEngine workflowEngine;
    private AgentCoordinator agentCoordinator;
    private String stockSymbol;
    private String analysisType;
    private boolean isComplete;
    
    public InvestmentAnalysisWorkflow(String stockSymbol, String analysisType) {
        this.stockSymbol = stockSymbol;
        this.analysisType = analysisType;
        this.workflowEngine = new WorkflowEngine();
        this.workflowEngine.setWorkflowId("investment_analysis_" + stockSymbol);
        this.workflowEngine.setWorkflowName("投资分析工作流");
        this.agentCoordinator = new AgentCoordinator();
        this.isComplete = false;
        
        initializeWorkflow();
    }
    
    /**
     * 初始化工作流 - 构建完整的投资分析流程
     * 
     * 工作流执行顺序：技术分析 → 情绪分析 → 新闻分析 → 基本面分析 → 投资辩论 → 风险评估 → 最终决策
     * 每个步骤都有明确的AI调用目标和参数配置
     */
    private void initializeWorkflow() {
        
        // =============================================================================
        // 步骤1: 技术分析 (Technical Analysis)
        // 分析股票的K线图、移动平均线、RSI、MACD等技术指标
        // 
        // 当前实现状态说明：
        // - "technical_analysis"：步骤ID，工作流引擎用于识别和跟踪此步骤
        // - "技术分析"：步骤名称，中文显示名称，用于日志和状态显示  
        // - "technical"：分析类型参数，存储在步骤参数中但当前未实际使用
        // 
        // ⚠️ 注意：当前WorkflowEngine.executeStepLogic()为模拟实现，
        //    并未真正调用MarketAnalyst智能体。实际映射机制待实现。
        // =============================================================================
        WorkflowStep technicalAnalysis = WorkflowStep.createAnalysisStep(
            "technical_analysis",           // 步骤ID：技术分析的唯一标识符
            "技术分析",                      // 步骤名称：中文显示名称，用于日志和状态显示
            "technical"                     // 分析类型：告诉AI进行技术分析，会触发技术指标计算
        );
        technicalAnalysis.addParameter("stock_symbol", stockSymbol);  // 传入股票代码参数，如"AAPL"
        workflowEngine.addStep(technicalAnalysis);
        
        // =============================================================================
        // 步骤2: 情绪分析 (Sentiment Analysis) 
        // 分析市场对股票的整体情绪，包括社交媒体情绪、分析师评级等
        // AI调用：情绪分析师智能体，分析新闻、社交媒体、分析师报告的情绪倾向
        // 依赖：需要技术分析结果作为参考背景
        //
        // 当前实现状态说明：
        // - "sentiment_analysis"：步骤ID，工作流引擎用于识别和跟踪此步骤
        // - "情绪分析"：步骤名称，中文显示名称，用于日志和状态显示
        // - "sentiment"：分析类型参数，存储在步骤参数中但当前未实际使用
        // 
        // ⚠️ 注意：当前WorkflowEngine.executeStepLogic()为模拟实现，
        //    并未真正调用SentimentAnalyst智能体。实际映射机制待实现。
        // =============================================================================
        WorkflowStep sentimentAnalysis = WorkflowStep.createAnalysisStep(
            "sentiment_analysis",           // 步骤ID：情绪分析的唯一标识符
            "情绪分析",                      // 步骤名称：分析市场对股票的情绪态度
            "sentiment"                     // 分析类型：存储为参数，待后续实现中用于选择智能体
        );
        sentimentAnalysis.addDependency("technical_analysis");      // 依赖：等待技术分析完成后执行
        sentimentAnalysis.addParameter("stock_symbol", stockSymbol);  // 传入股票代码
        workflowEngine.addStep(sentimentAnalysis);
        
        // =============================================================================
        // 步骤3: 新闻分析 (News Analysis)
        // 收集和分析与股票相关的最新新闻，评估新闻对股价的影响
        // AI调用：新闻分析师智能体，筛选重要新闻并分析影响程度
        // 依赖：需要情绪分析结果作为参考背景
        //
        // 当前实现状态说明：
        // - "news_analysis"：步骤ID，工作流引擎用于识别和跟踪此步骤
        // - "新闻分析"：步骤名称，中文显示名称，用于日志和状态显示
        // - "news"：分析类型参数，存储在步骤参数中但当前未实际使用
        // 
        // ⚠️ 注意：当前WorkflowEngine.executeStepLogic()为模拟实现，
        //    并未真正调用NewsAnalyst智能体。实际映射机制待实现。
        // =============================================================================
        WorkflowStep newsAnalysis = WorkflowStep.createAnalysisStep(
            "news_analysis",                // 步骤ID：新闻分析的唯一标识符
            "新闻分析",                      // 步骤名称：分析相关新闻和事件
            "news"                          // 分析类型：存储为参数，待后续实现中用于选择智能体
        );
        newsAnalysis.addDependency("sentiment_analysis");         // 依赖：等待情绪分析完成后执行
        newsAnalysis.addParameter("stock_symbol", stockSymbol);     // 传入股票代码
        workflowEngine.addStep(newsAnalysis);
        
        // =============================================================================
        // 步骤4: 基本面分析 (Fundamental Analysis)
        // 分析公司财务报表、盈利能力、成长性、估值水平等基本面数据
        // AI调用：基本面分析师智能体，分析财务数据和公司业务模式
        // 依赖：需要新闻分析结果作为参考背景
        //
        // 当前实现状态说明：
        // - "fundamentals_analysis"：步骤ID，工作流引擎用于识别和跟踪此步骤
        // - "基本面分析"：步骤名称，中文显示名称，用于日志和状态显示
        // - "fundamentals"：分析类型参数，存储在步骤参数中但当前未实际使用
        // 
        // ⚠️ 注意：当前WorkflowEngine.executeStepLogic()为模拟实现，
        //    并未真正调用FundamentalsAnalyst智能体。实际映射机制待实现。
        // =============================================================================
        WorkflowStep fundamentalsAnalysis = WorkflowStep.createAnalysisStep(
            "fundamentals_analysis",        // 步骤ID：基本面分析的唯一标识符
            "基本面分析",                    // 步骤名称：分析公司财务和经营状况
            "fundamentals"                  // 分析类型：存储为参数，待后续实现中用于选择智能体
        );
        fundamentalsAnalysis.addDependency("news_analysis");        // 依赖：等待新闻分析完成后执行
        fundamentalsAnalysis.addParameter("stock_symbol", stockSymbol); // 传入股票代码
        workflowEngine.addStep(fundamentalsAnalysis);
        
        // =============================================================================
        // 步骤5: 投资辩论 (Investment Debate)
        // 模拟投资团队的讨论过程，多头、空头、中性分析师进行辩论
        // AI调用：三个不同立场的分析师智能体进行多轮对话辩论
        // 依赖：需要前面所有分析结果作为辩论依据
        //
        // 当前实现状态说明：
        // - "investment_debate"：步骤ID，工作流引擎用于识别和跟踪此步骤
        // - "投资辩论"：步骤名称，中文显示名称，用于日志和状态显示
        // - "bull_bear_debate"：辩论主题参数，存储在步骤参数中但当前未实际使用
        // - participants列表：参与辩论的智能体标识符，但当前未实际调用这些智能体
        // 
        // ⚠️ 注意：当前WorkflowEngine.executeStepLogic()为模拟实现，
        //    并未真正调用BullResearcher、BearResearcher等智能体。实际映射机制待实现。
        // =============================================================================
        List<String> participants = Arrays.asList(
            "bull_analyst",     // 多头分析师标识符，当前未实际调用
            "bear_analyst",     // 空头分析师标识符，当前未实际调用
            "neutral_analyst"   // 中性分析师标识符，当前未实际调用
        );
        WorkflowStep investmentDebate = WorkflowStep.createDebateStep(
            "investment_debate",            // 步骤ID：投资辩论的唯一标识符
            "投资辩论",                      // 步骤名称：模拟投资团队讨论
            "bull_bear_debate",             // 辩论主题：多头vs空头辩论
            participants                      // 参与者：三个不同立场的分析师
        );
        investmentDebate.addDependency("fundamentals_analysis");    // 依赖：等待基本面分析完成后执行
        investmentDebate.addParameter("max_rounds", 3);               // 参数：辩论最多进行3轮
        workflowEngine.addStep(investmentDebate);
        
        // =============================================================================
        // 步骤6: 风险评估 (Risk Assessment)
        // 全面评估投资风险，包括市场风险、信用风险、操作风险、流动性风险
        // AI调用：风险评估智能体，综合分析各种风险因素
        // 依赖：需要投资辩论结果作为参考背景
        //
        // 当前实现状态说明：
        // - "risk_assessment"：步骤ID，工作流引擎用于识别和跟踪此步骤
        // - "风险评估"：步骤名称，中文显示名称，用于日志和状态显示
        // - riskFactors列表：风险因子类型字符串，存储为参数但当前未实际使用
        //
        // ⚠️ 注意：当前WorkflowEngine.executeStepLogic()为模拟实现，
        //    并未真正调用RiskManager智能体。实际映射机制待实现。
        // =============================================================================
        List<String> riskFactors = Arrays.asList(
            "market",       // 市场风险因子，当前未实际使用
            "credit",       // 信用风险因子，当前未实际使用
            "operational",  // 操作风险因子，当前未实际使用
            "liquidity"     // 流动性风险因子，当前未实际使用
        );
        WorkflowStep riskAssessment = WorkflowStep.createRiskAssessmentStep(
            "risk_assessment",              // 步骤ID：风险评估的唯一标识符
            "风险评估",                      // 步骤名称：全面评估投资风险
            riskFactors                     // 风险因子：需要评估的风险类型列表
        );
        riskAssessment.addDependency("investment_debate");          // 依赖：等待投资辩论完成后执行
        workflowEngine.addStep(riskAssessment);
        
        // =============================================================================
        // 步骤7: 最终决策 (Final Decision)
        // 整合所有分析结果，生成最终投资建议和决策理由
        // AI调用：投资组合管理智能体，综合所有信息做出投资决策
        // 依赖：需要风险评估结果作为决策依据
        //
        // 当前实现状态说明：
        // - "final_decision"：步骤ID，工作流引擎用于识别和跟踪此步骤
        // - "最终决策"：步骤名称，中文显示名称，用于日志和状态显示
        // - "investment_recommendation"：决策类型参数，存储在步骤参数中但当前未实际使用
        // 
        // ⚠️ 注意：当前WorkflowEngine.executeStepLogic()为模拟实现，
        //    并未真正调用PortfolioManager智能体。实际映射机制待实现。
        // =============================================================================
        WorkflowStep finalDecision = WorkflowStep.createDecisionStep(
            "final_decision",               // 步骤ID：最终决策的唯一标识符
            "最终决策",                      // 步骤名称：生成投资建议
            "investment_recommendation"     // 决策类型：生成投资建议和买卖信号
        );
        finalDecision.addDependency("risk_assessment");             // 依赖：等待风险评估完成后执行
        finalDecision.addParameter("decision_type", "investment_recommendation");  // 决策类型：存储为参数，待后续实现中用于选择智能体
        workflowEngine.addStep(finalDecision);
        
        log.info("初始化投资分析工作流完成，股票: {}, 类型: {}", stockSymbol, analysisType);
    }
    
    /**
     * 执行工作流
     */
    public void execute() {
        log.info("开始执行投资分析工作流 - 股票: {}, 类型: {}", stockSymbol, analysisType);
        
        // 开始工作流
        agentCoordinator.nextPhase("ANALYSIS");
        
        try {
            // 执行工作流
            Map<String, Object> result = workflowEngine.startExecution();
            
            // 完成工作流
            agentCoordinator.nextPhase("COMPLETED");
            isComplete = true;
            log.info("投资分析工作流完成 - 股票: {}", stockSymbol);
            
        } catch (Exception e) {
            // 工作流执行失败
            workflowEngine.setStatus("FAILED");
            agentCoordinator.nextPhase("FAILED");
            log.error("投资分析工作流失败 - 股票: {}", stockSymbol, e);
        }
    }
    
    /**
     * 更新智能体协调器
     */
    private void updateAgentCoordinator() {
        Map<String, Object> workflowStatus = workflowEngine.getWorkflowStatus();
        List<Map<String, Object>> stepStatuses = (List<Map<String, Object>>) workflowStatus.get("steps");
        String currentStepId = null;
        
        // 找到当前正在执行的步骤
        for (Map<String, Object> stepStatus : stepStatuses) {
            if ("RUNNING".equals(stepStatus.get("status"))) {
                currentStepId = (String) stepStatus.get("stepId");
                break;
            }
        }
        
        if ("investment_debate".equals(currentStepId)) {
            // 更新投资辩论状态
            InvestmentDebate debate = agentCoordinator.getInvestmentDebate();
            if (debate.shouldContinueDebate()) {
                String nextSpeaker = debate.getNextSpeaker();
                log.debug("投资辩论 - 下一位发言者: {}", nextSpeaker);
            }
        }
        
        if ("risk_assessment".equals(currentStepId)) {
            // 更新风险评估状态
            RiskDiscussion riskDiscussion = agentCoordinator.getRiskDiscussion();
            if (riskDiscussion.shouldContinueDiscussion()) {
                log.debug("风险评估讨论继续进行中");
            }
        }
    }
    
    /**
     * 获取分析结果
     */
    public String getAnalysisResult() {
        if (!isComplete) {
            return "分析未完成";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("投资分析报告 - ").append(stockSymbol).append("\n");
        result.append("分析类型: ").append(analysisType).append("\n\n");
        
        // 添加工作流执行统计
        result.append(workflowEngine.getWorkflowStatus()).append("\n\n");
        
        // 添加智能体协调总结
        result.append(agentCoordinator.getCoordinationSummary()).append("\n");
        
        return result.toString();
    }
    
    /**
     * 获取工作流状态
     */
    public String getWorkflowStatus() {
        return workflowEngine.getStatus();
    }
    
    /**
     * 获取协调状态
     */
    public String getCoordinationStatus() {
        return agentCoordinator.getCurrentStatus();
    }
    
    /**
     * 获取实时进展
     */
    public String getProgressUpdate() {
        StringBuilder progress = new StringBuilder();
        
        progress.append("=== 投资分析进展 ===\n");
        progress.append("股票: ").append(stockSymbol).append("\n");
        progress.append("工作流: ").append(workflowEngine.getStatus()).append("\n");
        progress.append("协调: ").append(agentCoordinator.getCurrentStatus()).append("\n");
        
        // 当前步骤详细信息
        Map<String, Object> workflowStatus = workflowEngine.getWorkflowStatus();
        List<Map<String, Object>> stepStatuses = (List<Map<String, Object>>) workflowStatus.get("steps");
        String currentStepId = null;
        
        // 找到当前正在执行的步骤
        for (Map<String, Object> stepStatus : stepStatuses) {
            if ("RUNNING".equals(stepStatus.get("status"))) {
                currentStepId = (String) stepStatus.get("stepId");
                break;
            }
        }
        
        if (currentStepId != null && !currentStepId.isEmpty()) {
            progress.append("当前步骤: ").append(currentStepId).append("\n");
        }
        
        return progress.toString();
    }
    
    /**
     * 重置工作流
     */
    public void reset() {
        workflowEngine.reset();
        agentCoordinator.reset();
        isComplete = false;
        log.info("投资分析工作流已重置 - 股票: {}", stockSymbol);
    }
    
    /**
     * 停止工作流
     */
    public void stop() {
        workflowEngine.setStatus("CANCELLED");
        agentCoordinator.stopCoordination();
        isComplete = false;
        log.info("投资分析工作流已停止 - 股票: {}", stockSymbol);
    }
    
    /**
     * 获取详细报告
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== 投资分析详细报告 ===\n");
        report.append("股票代码: ").append(stockSymbol).append("\n");
        report.append("分析类型: ").append(analysisType).append("\n");
        report.append("完成状态: ").append(isComplete ? "已完成" : "进行中").append("\n\n");
        
        // 工作流详细信息
        report.append("=== 工作流执行详情 ===\n");
        report.append(workflowEngine.getWorkflowStatus()).append("\n\n");
        
        // 智能体协调详细信息
        report.append("=== 智能体协调详情 ===\n");
        report.append(agentCoordinator.getCoordinationSummary()).append("\n");
        
        return report.toString();
    }
}