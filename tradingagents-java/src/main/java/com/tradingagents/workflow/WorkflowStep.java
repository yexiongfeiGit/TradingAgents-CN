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
 * 工作流步骤
 * 定义工作流中的单个步骤
 */
@Data
@Slf4j
public class WorkflowStep {
    

    private String stepId;
    private String stepName;
    private String stepType; // ANALYSIS, DEBATE, RISK_ASSESSMENT, DECISION, COORDINATION
    private List<WorkflowCondition> conditions;
    private Map<String, Object> parameters;
    private List<String> dependencies;
    private boolean isCompleted;
    private boolean isActive;
    private String status;
    private String result;
    private long startTime;
    private long endTime;
    private long timeout; // 超时时间（毫秒）
    
    public WorkflowStep(String stepId, String stepName, String stepType) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.stepType = stepType;
        this.conditions = new ArrayList<>();
        this.parameters = new ConcurrentHashMap<>();
        this.dependencies = new ArrayList<>();
        this.isCompleted = false;
        this.isActive = false;
        this.status = "PENDING";
        this.result = "";
        this.startTime = 0;
        this.endTime = 0;
        this.timeout = 300000; // 默认5分钟超时
    }
    
    /**
     * 添加条件
     */
    public void addCondition(WorkflowCondition condition) {
        conditions.add(condition);
        log.debug("添加条件到步骤 {}: {}", stepId, condition.getDescription());
    }
    
    /**
     * 添加参数
     */
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
        log.debug("添加参数到步骤 {}: {} = {}", stepId, key, value);
    }
    
    /**
     * 添加依赖
     */
    public void addDependency(String dependencyStepId) {
        dependencies.add(dependencyStepId);
        log.debug("添加依赖到步骤 {}: {}", stepId, dependencyStepId);
    }
    
    /**
     * 检查是否可以执行
     */
    public boolean canExecute(Map<String, Object> context) {
        // 检查是否已完成
        if (isCompleted) {
            log.debug("步骤 {} 已完成，跳过执行", stepId);
            return false;
        }
        
        // 检查是否正在执行
        if (isActive) {
            log.debug("步骤 {} 正在执行中，跳过重复执行", stepId);
            return false;
        }
        
        // 检查条件
        for (WorkflowCondition condition : conditions) {
            Object contextValue = context.get(condition.getParameter());
            if (!condition.evaluate(contextValue)) {
                log.debug("步骤 {} 条件不满足: {}", stepId, condition.getDescription());
                return false;
            }
        }
        
        log.debug("步骤 {} 可以执行", stepId);
        return true;
    }
    
    /**
     * 开始执行
     */
    public void startExecution() {
        this.isActive = true;
        this.status = "RUNNING";
        this.startTime = System.currentTimeMillis();
        log.debug("开始执行步骤 {}: {}", stepId, stepName);
    }
    
    /**
     * 完成执行
     */
    public void completeExecution(String result) {
        this.isActive = false;
        this.isCompleted = true;
        this.status = "COMPLETED";
        this.result = result;
        this.endTime = System.currentTimeMillis();
        log.debug("完成执行步骤 {}: {}, 耗时: {}ms", stepId, stepName, getExecutionTime());
    }
    
    /**
     * 执行失败
     */
    public void failExecution(String errorMessage) {
        this.isActive = false;
        this.status = "FAILED";
        this.result = errorMessage;
        this.endTime = System.currentTimeMillis();
        log.error("步骤 {} 执行失败: {}", stepId, errorMessage);
    }
    
    /**
     * 检查是否超时
     */
    public boolean isTimeout() {
        if (!isActive || startTime == 0) {
            return false;
        }
        return (System.currentTimeMillis() - startTime) > timeout;
    }
    
    /**
     * 获取执行时间
     */
    public long getExecutionTime() {
        if (startTime == 0) {
            return 0;
        }
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return String.format("步骤 %s (%s): %s, 耗时: %dms, 结果: %s",
                stepId, stepName, status, getExecutionTime(), result);
    }
    
    /**
     * 重置步骤
     */
    public void reset() {
        this.isCompleted = false;
        this.isActive = false;
        this.status = "PENDING";
        this.result = "";
        this.startTime = 0;
        this.endTime = 0;
        log.debug("重置步骤 {}: {}", stepId, stepName);
    }
    
    /**
     * 创建分析步骤
     */
    public static WorkflowStep createAnalysisStep(String stepId, String stepName) {
        return new WorkflowStep(stepId, stepName, "ANALYSIS");
    }
    
    /**
     * 创建辩论步骤
     */
    public static WorkflowStep createDebateStep(String stepId, String stepName) {
        return new WorkflowStep(stepId, stepName, "DEBATE");
    }
    
    /**
     * 创建风险评估步骤
     */
    public static WorkflowStep createRiskAssessmentStep(String stepId, String stepName) {
        return new WorkflowStep(stepId, stepName, "RISK_ASSESSMENT");
    }
    
    /**
     * 创建决策步骤
     */
    public static WorkflowStep createDecisionStep(String stepId, String stepName) {
        return new WorkflowStep(stepId, stepName, "DECISION");
    }
    
    /**
     * 创建协调步骤
     */
    public static WorkflowStep createCoordinationStep(String stepId, String stepName) {
        return new WorkflowStep(stepId, stepName, "COORDINATION");
    }
    
    /**
     * 获取步骤名称
     */
    public String getStepName() {
        return stepName;
    }
    
    /**
     * 获取步骤ID
     */
    public String getStepId() {
        return stepId;
    }
    
    /**
     * 获取步骤类型
     */
    public String getStepType() {
        return stepType;
    }
    
    /**
     * 获取状态
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * 检查是否完成
     */
    public boolean isCompleted() {
        return isCompleted;
    }
    
    /**
     * 检查是否活跃
     */
    public boolean isActive() {
        return isActive;
    }
}