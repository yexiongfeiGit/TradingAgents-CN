package com.tradingagents.workflow;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流引擎
 * 管理智能体工作流的执行
 */
@Data
@Slf4j
public class WorkflowEngine {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);
    
    private String workflowId;
    private String workflowName;
    private List<WorkflowStep> steps;
    private Map<String, Object> context;
    private String currentStepId;
    private String status;
    private boolean isActive;
    private long startTime;
    private long endTime;
    private int maxRetries;
    private int currentRetry;
    
    public WorkflowEngine(String workflowId, String workflowName) {
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.steps = new ArrayList<>();
        this.context = new ConcurrentHashMap<>();
        this.currentStepId = "";
        this.status = "PENDING";
        this.isActive = false;
        this.startTime = 0;
        this.endTime = 0;
        this.maxRetries = 3;
        this.currentRetry = 0;
    }
    
    /**
     * 添加步骤
     */
    public void addStep(WorkflowStep step) {
        steps.add(step);
        log.debug("添加步骤到工作流 {}: {}", workflowId, step.getStepName());
    }
    
    /**
     * 添加上下文参数
     */
    public void addContextParameter(String key, Object value) {
        context.put(key, value);
        log.debug("添加上下文参数: {} = {}", key, value);
    }
    
    /**
     * 开始执行工作流
     */
    public void start() {
        this.isActive = true;
        this.status = "RUNNING";
        this.startTime = System.currentTimeMillis();
        this.currentRetry = 0;
        log.info("开始执行工作流 {}: {}", workflowId, workflowName);
    }
    
    /**
     * 执行下一步
     */
    public boolean executeNextStep() {
        if (!isActive) {
            log.warn("工作流 {} 未激活，无法执行步骤", workflowId);
            return false;
        }
        
        // 查找下一个可执行的步骤
        WorkflowStep nextStep = findNextExecutableStep();
        if (nextStep == null) {
            log.debug("工作流 {} 没有可执行的步骤", workflowId);
            return false;
        }
        
        try {
            // 执行步骤
            executeStep(nextStep);
            return true;
            
        } catch (Exception e) {
            log.error("执行步骤 {} 失败", nextStep.getStepId(), e);
            nextStep.failExecution("执行失败: " + e.getMessage());
            
            // 重试逻辑
            if (currentRetry < maxRetries) {
                currentRetry++;
                log.info("重试工作流 {}，第 {} 次", workflowId, currentRetry);
                return executeNextStep();
            } else {
                failWorkflow("达到最大重试次数");
                return false;
            }
        }
    }
    
    /**
     * 查找下一个可执行的步骤
     */
    private WorkflowStep findNextExecutableStep() {
        for (WorkflowStep step : steps) {
            if (step.canExecute(context)) {
                return step;
            }
        }
        return null;
    }
    
    /**
     * 执行具体步骤
     */
    private void executeStep(WorkflowStep step) {
        currentStepId = step.getStepId();
        step.startExecution();
        
        log.info("执行步骤 {}: {}", step.getStepId(), step.getStepName());
        
        // 模拟步骤执行时间
        try {
            Thread.sleep(100); // 模拟执行时间
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 根据步骤类型执行不同逻辑
        String result = executeStepLogic(step);
        
        step.completeExecution(result);
        
        // 更新上下文
        updateContextWithStepResult(step, result);
        
        // 重置重试计数
        currentRetry = 0;
    }
    
    /**
     * 执行步骤逻辑
     */
    private String executeStepLogic(WorkflowStep step) {
        switch (step.getStepType()) {
            case "ANALYSIS":
                return "分析完成";
            case "DEBATE":
                return "辩论完成";
            case "RISK_ASSESSMENT":
                return "风险评估完成";
            case "DECISION":
                return "决策完成";
            case "COORDINATION":
                return "协调完成";
            default:
                return "步骤完成";
        }
    }
    
    /**
     * 更新上下文
     */
    private void updateContextWithStepResult(WorkflowStep step, String result) {
        context.put(step.getStepId() + "_result", result);
        context.put(step.getStepId() + "_status", step.getStatus());
        
        // 根据步骤类型添加特定上下文
        switch (step.getStepType()) {
            case "ANALYSIS":
                context.put("last_analysis_result", result);
                break;
            case "DEBATE":
                context.put("last_debate_result", result);
                break;
            case "RISK_ASSESSMENT":
                context.put("last_risk_result", result);
                break;
            case "DECISION":
                context.put("final_decision", result);
                break;
        }
        
        log.debug("更新上下文 - 步骤: {}, 结果: {}", step.getStepId(), result);
    }
    
    /**
     * 完成工作流
     */
    public void completeWorkflow() {
        this.isActive = false;
        this.status = "COMPLETED";
        this.endTime = System.currentTimeMillis();
        log.info("工作流 {} 完成，总耗时: {}ms", workflowId, getTotalExecutionTime());
    }
    
    /**
     * 失败工作流
     */
    public void failWorkflow(String errorMessage) {
        this.isActive = false;
        this.status = "FAILED";
        this.endTime = System.currentTimeMillis();
        log.error("工作流 {} 失败: {}", workflowId, errorMessage);
    }
    
    /**
     * 检查是否完成
     */
    public boolean isCompleted() {
        if (!isActive) {
            return true;
        }
        
        // 检查是否所有步骤都已完成
        for (WorkflowStep step : steps) {
            if (!step.isCompleted()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取总执行时间
     */
    public long getTotalExecutionTime() {
        if (startTime == 0) {
            return 0;
        }
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }
    
    /**
     * 获取执行状态
     */
    public String getExecutionStatus() {
        return String.format("工作流 %s (%s): %s, 当前步骤: %s, 总耗时: %dms",
                workflowId, workflowName, status, currentStepId, getTotalExecutionTime());
    }
    
    /**
     * 获取步骤执行统计
     */
    public String getStepExecutionStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("工作流步骤执行统计:\n");
        
        int completed = 0;
        int failed = 0;
        int pending = 0;
        
        for (WorkflowStep step : steps) {
            switch (step.getStatus()) {
                case "COMPLETED":
                    completed++;
                    break;
                case "FAILED":
                    failed++;
                    break;
                default:
                    pending++;
                    break;
            }
            stats.append(String.format("- %s: %s (%dms)\n", 
                    step.getStepName(), step.getStatus(), step.getExecutionTime()));
        }
        
        stats.append(String.format("总计: %d 完成, %d 失败, %d 待执行", completed, failed, pending));
        return stats.toString();
    }
    
    /**
     * 获取当前步骤ID
     */
    public String getCurrentStepId() {
        return currentStepId;
    }
    
    /**
     * 重置工作流
     */
    public void reset() {
        for (WorkflowStep step : steps) {
            step.reset();
        }
        context.clear();
        currentStepId = "";
        status = "PENDING";
        isActive = false;
        startTime = 0;
        endTime = 0;
        currentRetry = 0;
        log.debug("工作流 {} 已重置", workflowId);
    }
}