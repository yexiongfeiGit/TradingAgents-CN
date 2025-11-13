package com.tradingagents.workflow;

import com.tradingagents.debate.AgentCoordinator;
import com.tradingagents.debate.InvestmentDebate;
import com.tradingagents.debate.MultiRoundDialogue;
import com.tradingagents.debate.RiskDiscussion;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 投资分析工作流
 * 管理完整的投资分析流程
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
        this.workflowEngine = new WorkflowEngine("investment_analysis_" + stockSymbol, "投资分析工作流");
        this.agentCoordinator = new AgentCoordinator();
        this.isComplete = false;
        
        initializeWorkflow();
    }
    
    /**
     * 初始化工作流
     */
    private void initializeWorkflow() {
        // 步骤1: 技术分析
        WorkflowStep technicalAnalysis = WorkflowStep.createAnalysisStep("technical_analysis", "技术分析");
        technicalAnalysis.addParameter("analysis_type", "technical");
        technicalAnalysis.addParameter("stock_symbol", stockSymbol);
        workflowEngine.addStep(technicalAnalysis);
        
        // 步骤2: 情绪分析
        WorkflowStep sentimentAnalysis = WorkflowStep.createAnalysisStep("sentiment_analysis", "情绪分析");
        sentimentAnalysis.addDependency("technical_analysis");
        sentimentAnalysis.addParameter("analysis_type", "sentiment");
        sentimentAnalysis.addParameter("stock_symbol", stockSymbol);
        workflowEngine.addStep(sentimentAnalysis);
        
        // 步骤3: 新闻分析
        WorkflowStep newsAnalysis = WorkflowStep.createAnalysisStep("news_analysis", "新闻分析");
        newsAnalysis.addDependency("sentiment_analysis");
        newsAnalysis.addParameter("analysis_type", "news");
        newsAnalysis.addParameter("stock_symbol", stockSymbol);
        workflowEngine.addStep(newsAnalysis);
        
        // 步骤4: 基本面分析
        WorkflowStep fundamentalsAnalysis = WorkflowStep.createAnalysisStep("fundamentals_analysis", "基本面分析");
        fundamentalsAnalysis.addDependency("news_analysis");
        fundamentalsAnalysis.addParameter("analysis_type", "fundamentals");
        fundamentalsAnalysis.addParameter("stock_symbol", stockSymbol);
        workflowEngine.addStep(fundamentalsAnalysis);
        
        // 步骤5: 投资辩论
        WorkflowStep investmentDebate = WorkflowStep.createDebateStep("investment_debate", "投资辩论");
        investmentDebate.addDependency("fundamentals_analysis");
        investmentDebate.addParameter("debate_type", "bull_bear");
        investmentDebate.addParameter("max_rounds", 3);
        workflowEngine.addStep(investmentDebate);
        
        // 步骤6: 风险评估
        WorkflowStep riskAssessment = WorkflowStep.createRiskAssessmentStep("risk_assessment", "风险评估");
        riskAssessment.addDependency("investment_debate");
        riskAssessment.addParameter("assessment_type", "comprehensive");
        workflowEngine.addStep(riskAssessment);
        
        // 步骤7: 最终决策
        WorkflowStep finalDecision = WorkflowStep.createDecisionStep("final_decision", "最终决策");
        finalDecision.addDependency("risk_assessment");
        finalDecision.addParameter("decision_type", "investment_recommendation");
        workflowEngine.addStep(finalDecision);
        
        log.info("初始化投资分析工作流完成，股票: {}, 类型: {}", stockSymbol, analysisType);
    }
    
    /**
     * 执行工作流
     */
    public void execute() {
        log.info("开始执行投资分析工作流 - 股票: {}, 类型: {}", stockSymbol, analysisType);
        
        // 开始工作流
        workflowEngine.start();
        agentCoordinator.nextPhase("ANALYSIS");
        
        // 执行工作流步骤
        while (workflowEngine.executeNextStep()) {
            updateAgentCoordinator();
            
            // 检查是否完成
            if (workflowEngine.isCompleted()) {
                break;
            }
            
            try {
                Thread.sleep(100); // 模拟执行间隔
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 完成工作流
        if (workflowEngine.isCompleted()) {
            workflowEngine.completeWorkflow();
            agentCoordinator.nextPhase("COMPLETED");
            isComplete = true;
            log.info("投资分析工作流完成 - 股票: {}", stockSymbol);
        } else {
            workflowEngine.failWorkflow("工作流执行失败");
            agentCoordinator.nextPhase("FAILED");
            log.error("投资分析工作流失败 - 股票: {}", stockSymbol);
        }
    }
    
    /**
     * 更新智能体协调器
     */
    private void updateAgentCoordinator() {
        String currentStepId = workflowEngine.getCurrentStepId();
        
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
        result.append(workflowEngine.getStepExecutionStats()).append("\n\n");
        
        // 添加智能体协调总结
        result.append(agentCoordinator.getCoordinationSummary()).append("\n");
        
        return result.toString();
    }
    
    /**
     * 获取工作流状态
     */
    public String getWorkflowStatus() {
        return workflowEngine.getExecutionStatus();
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
        progress.append("工作流: ").append(workflowEngine.getExecutionStatus()).append("\n");
        progress.append("协调: ").append(agentCoordinator.getCurrentStatus()).append("\n");
        
        // 当前步骤详细信息
        String currentStepId = workflowEngine.getCurrentStepId();
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
        workflowEngine.failWorkflow("手动停止");
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
        report.append(workflowEngine.getStepExecutionStats()).append("\n\n");
        
        // 智能体协调详细信息
        report.append("=== 智能体协调详情 ===\n");
        report.append(agentCoordinator.getCoordinationSummary()).append("\n");
        
        return report.toString();
    }
}