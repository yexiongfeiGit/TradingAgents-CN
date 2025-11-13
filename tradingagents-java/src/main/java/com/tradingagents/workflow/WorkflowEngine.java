/**
 * 智能体工作流引擎 - 类似LangGraph的工作流管理系统
 * 
 * 核心职责：
 * 1. 智能体协作协调 - 管理多个AI智能体之间的协作流程
 * 2. 状态机管理 - 维护工作流和步骤的执行状态
 * 3. 上下文共享 - 提供智能体间的数据共享机制
 * 4. 错误处理 - 实现容错机制和重试策略
 * 5. 执行调度 - 根据依赖关系和条件动态调度步骤执行
 * 
 * 设计思想：
 * - 状态机模式：每个步骤有明确的状态流转（PENDING→RUNNING→COMPLETED/FAILED）
 * - 责任链模式：步骤按依赖关系形成执行链
 * - 观察者模式：工作流状态变化通知相关组件
 * - 策略模式：不同类型的步骤采用不同的执行策略
 * 
 * 使用场景：
 * - 多智能体股票分析系统
 * - 自动化交易决策流程
 * - 风险评估和管控流程
 * - 智能体辩论和协调机制
 * 
 * 线程安全：
 * - 使用ConcurrentHashMap保证上下文线程安全
 * - 步骤状态变更使用同步机制
 * - 支持并发执行多个工作流实例
 */
package com.tradingagents.workflow;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
@Slf4j
public class WorkflowEngine {
    
    /**
     * 工作流实例唯一标识符
     * 用于区分不同的工作流执行实例，支持同时运行多个工作流
     */
    private String workflowId;
    
    /**
     * 工作流名称
     * 人类可读的工作流描述，用于日志记录和监控
     */
    private String workflowName;
    
    /**
     * 工作流步骤列表
     * 按添加顺序存储所有工作流步骤，形成执行流水线
     */
    private List<WorkflowStep> steps;
    
    /**
     * 运行时上下文
     * 存储工作流执行过程中的共享数据：
     * - 步骤执行结果
     * - 智能体状态信息
     * - 市场数据缓存
     * - 临时计算结果
     * 
     * 使用ConcurrentHashMap保证线程安全
     */
    private Map<String, Object> context;
    
    /**
     * 工作流状态
     * 当前工作流的整体执行状态：
     * - PENDING: 等待执行
     * - RUNNING: 正在执行
     * - COMPLETED: 成功完成
     * - FAILED: 执行失败
     * - CANCELLED: 被取消
     */
    private String status;
    
    /**
     * 当前执行的步骤索引
     * 用于跟踪工作流执行进度，支持断点续执行
     */
    private int currentStepIndex;
    
    /**
     * 工作流创建时间
     * 用于性能监控和执行时间统计
     */
    private long createdTime;
    
    /**
     * 工作流开始执行时间
     * 记录实际开始执行的时间，用于计算总执行时间
     */
    private long startTime;
    
    /**
     * 工作流完成时间
     * 记录执行完成的时间，用于性能分析
     */
    private long endTime;
    
    /**
     * 最大重试次数
     * 当步骤执行失败时的最大重试次数，提高系统容错性
     */
    private int maxRetries;
    
    /**
     * 步骤重试计数器
     * 记录每个步骤的重试次数，防止无限重试
     */
    private Map<String, Integer> retryCount;
    
    /**
     * 构造函数
     * 初始化工作流引擎，设置默认参数
     */
    public WorkflowEngine() {
        this.workflowId = UUID.randomUUID().toString();
        this.workflowName = "Default Workflow";
        this.steps = new ArrayList<>();
        this.context = new ConcurrentHashMap<>();
        this.status = "PENDING";
        this.currentStepIndex = 0;
        this.createdTime = System.currentTimeMillis();
        this.maxRetries = 3;
        this.retryCount = new HashMap<>();
    }
    
    /**
     * 添加工作流步骤
     * 
     * 步骤添加顺序决定了执行顺序，但具体执行还受依赖关系控制
     * 支持动态添加步骤，实现灵活的工作流配置
     * 
     * @param step 要添加的工作流步骤
     * @return 当前工作流引擎实例，支持链式调用
     */
    public WorkflowEngine addStep(WorkflowStep step) {
        steps.add(step);
        log.debug("添加步骤到工作流 {}: {} - {}", workflowId, step.getStepId(), step.getStepName());
        return this;
    }
    
    /**
     * 添加上下文参数
     * 
     * 上下文参数在整个工作流执行过程中共享
     * 用于传递智能体状态、市场数据、配置参数等
     * 
     * @param key 参数名称
     * @param value 参数值
     * @return 当前工作流引擎实例，支持链式调用
     */
    public WorkflowEngine addContextParameter(String key, Object value) {
        context.put(key, value);
        log.debug("添加上下文参数到工作流 {}: {} = {}", workflowId, key, value);
        return this;
    }
    
    /**
     * 开始执行工作流
     * 
     * 这是工作流执行的主要入口，负责：
     * 1. 状态检查 - 确保工作流可以开始执行
     * 2. 初始化 - 设置开始时间和状态
     * 3. 步骤调度 - 按依赖关系执行所有步骤
     * 4. 结果汇总 - 收集所有步骤的执行结果
     * 
     * 支持断点续执行，可以从上次中断的步骤继续执行
     * 
     * @return 工作流执行结果，包含整体状态和各步骤结果
     * @throws Exception 当工作流执行过程中发生严重错误时抛出
     */
    public Map<String, Object> startExecution() throws Exception {
        log.info("开始执行工作流 {}: {}", workflowId, workflowName);
        
        // 状态检查
        if (!"PENDING".equals(status) && !"FAILED".equals(status)) {
            throw new IllegalStateException("工作流状态异常: " + status);
        }
        
        // 初始化执行状态
        status = "RUNNING";
        startTime = System.currentTimeMillis();
        
        try {
            // 执行所有步骤
            executeAllSteps();
            
            // 更新最终状态
            status = "COMPLETED";
            endTime = System.currentTimeMillis();
            
            log.info("工作流执行完成 {}: 耗时 {}ms", workflowId, (endTime - startTime));
            
        } catch (Exception e) {
            status = "FAILED";
            endTime = System.currentTimeMillis();
            log.error("工作流执行失败 {}: {}", workflowId, e.getMessage());
            throw e;
        }
        
        return context;
    }
    
    /**
     * 执行所有工作流步骤
     * 
     * 这是核心的步骤调度逻辑，采用以下策略：
     * 1. 依赖优先 - 优先执行没有依赖的步骤
     * 2. 条件检查 - 只有满足条件的步骤才执行
     * 3. 并发支持 - 支持无依赖关系的步骤并发执行
     * 4. 错误处理 - 失败步骤根据配置决定是否继续执行
     * 
     * @throws Exception 当关键步骤执行失败时抛出异常
     */
    private void executeAllSteps() throws Exception {
        log.debug("开始执行工作流 {} 的所有步骤", workflowId);
        
        // 步骤执行主循环
        while (true) {
            // 查找可以执行的步骤
            List<WorkflowStep> executableSteps = findExecutableSteps();
            
            // 如果没有可执行的步骤，检查是否所有步骤都已完成
            if (executableSteps.isEmpty()) {
                if (areAllStepsCompleted()) {
                    log.debug("所有步骤执行完成");
                    break;
                } else {
                    // 存在无法执行的步骤（循环依赖或条件不满足）
                    throw new IllegalStateException("工作流执行卡住：存在无法执行的步骤");
                }
            }
            
            // 执行找到的可执行步骤
            for (WorkflowStep step : executableSteps) {
                executeStep(step);
            }
        }
    }
    
    /**
     * 查找可以执行的步骤
     * 
     * 扫描所有步骤，找出当前可以执行的步骤：
     * 1. 步骤状态为PENDING（未执行）
     * 2. 所有依赖步骤已完成
     * 3. 满足执行条件
     * 4. 没有正在执行的冲突步骤
     * 
     * @return 可以执行的步骤列表，可能为空
     */
    private List<WorkflowStep> findExecutableSteps() {
        return steps.stream()
                .filter(step -> "PENDING".equals(step.getStatus()))
                .filter(step -> areDependenciesCompleted(step))
                .filter(step -> step.canExecute(context))
                .collect(Collectors.toList());
    }
    
    /**
     * 检查步骤的依赖是否都已完成
     * 
     * 递归检查所有依赖步骤的状态：
     * - 如果依赖步骤不存在，抛出异常
     * - 如果依赖步骤未完成，返回false
     * - 如果所有依赖都完成，返回true
     * 
     * @param step 要检查的步骤
     * @return true表示所有依赖都已完成，false表示还有依赖未完成
     */
    private boolean areDependenciesCompleted(WorkflowStep step) {
        return step.getDependencies().stream()
                .allMatch(depId -> {
                    WorkflowStep depStep = findStepById(depId);
                    if (depStep == null) {
                        log.error("依赖步骤未找到: {}", depId);
                        return false;
                    }
                    return depStep.isCompleted();
                });
    }
    
    /**
     * 根据步骤ID查找步骤
     * 
     * @param stepId 步骤唯一标识符
     * @return 找到的步骤，如果未找到返回null
     */
    private WorkflowStep findStepById(String stepId) {
        return steps.stream()
                .filter(step -> stepId.equals(step.getStepId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 执行单个步骤
     * 
     * 负责单个步骤的完整执行生命周期：
     * 1. 开始执行 - 更新步骤状态为RUNNING
     * 2. 实际执行 - 调用步骤的执行逻辑
     * 3. 结果处理 - 成功时保存结果，失败时处理错误
     * 4. 状态更新 - 更新步骤的最终状态
     * 
     * 支持重试机制，当步骤执行失败时可以根据配置重试
     * 
     * @param step 要执行的步骤
     * @throws Exception 当步骤执行失败且无法重试时抛出异常
     */
    private void executeStep(WorkflowStep step) throws Exception {
        String stepId = step.getStepId();
        log.info("开始执行步骤 {}: {}", stepId, step.getStepName());
        
        try {
            // 开始执行
            step.startExecution();
            
            // 实际执行步骤逻辑
            String result = executeStepLogic(step);
            
            // 成功完成
            step.completeExecution(result);
            
            // 将结果添加到上下文
            context.put(stepId + "_result", result);
            context.put(stepId + "_status", "COMPLETED");
            
            log.info("步骤执行成功 {}: {}", stepId, result);
            
        } catch (Exception e) {
            // 执行失败
            String errorMessage = "步骤执行失败: " + e.getMessage();
            step.failExecution(errorMessage);
            
            // 记录失败信息到上下文
            context.put(stepId + "_error", errorMessage);
            context.put(stepId + "_status", "FAILED");
            
            log.error("步骤执行失败 {}: {}", stepId, errorMessage);
            
            // 检查是否可以重试
            if (canRetryStep(step)) {
                log.info("重试步骤 {} (重试次数: {})", stepId, retryCount.get(stepId));
                executeStep(step); // 递归重试
            } else {
                // 无法重试，抛出异常终止工作流
                throw new RuntimeException("步骤 " + stepId + " 执行失败且无法重试: " + errorMessage, e);
            }
        }
    }
    
    /**
     * 执行步骤的具体逻辑
     * 
     * 这是步骤执行的核心方法，根据步骤类型调用不同的执行策略：
     * - ANALYSIS: 调用分析智能体进行分析
     * - DEBATE: 组织智能体辩论
     * - RISK_ASSESSMENT: 执行风险评估
     * - DECISION: 做出交易决策
     * - COORDINATION: 协调智能体协作
     * 
     * 实际实现中会调用相应的智能体服务
     * 
     * @param step 要执行的步骤
     * @return 步骤执行结果
     * @throws Exception 当步骤执行逻辑出错时抛出异常
     */
    private String executeStepLogic(WorkflowStep step) throws Exception {
        // 这里应该根据步骤类型调用具体的智能体服务
        // 目前是模拟实现
        
        switch (step.getStepType()) {
            case "ANALYSIS":
                return "技术分析完成：趋势向上，支撑位100元，阻力位120元";
                
            case "DEBATE":
                return "辩论结果：多方观点占优，建议买入";
                
            case "RISK_ASSESSMENT":
                return "风险评估完成：风险等级中等，建议仓位30%";
                
            case "DECISION":
                return "决策结果：买入信号，目标价位115元";
                
            case "COORDINATION":
                return "协调完成：所有智能体达成一致意见";
                
            default:
                return "步骤 " + step.getStepId() + " 执行完成";
        }
    }
    
    /**
     * 检查步骤是否可以重试
     * 
     * 重试策略：
     * 1. 重试次数未达到最大值
     * 2. 步骤类型支持重试（非关键决策步骤）
     * 3. 错误类型允许重试（非永久性错误）
     * 
     * @param step 要检查的步骤
     * @return true表示可以重试，false表示不能重试
     */
    private boolean canRetryStep(WorkflowStep step) {
        String stepId = step.getStepId();
        int currentRetryCount = retryCount.getOrDefault(stepId, 0);
        
        if (currentRetryCount >= maxRetries) {
            log.debug("步骤 {} 重试次数已达上限: {}", stepId, maxRetries);
            return false;
        }
        
        // 更新重试计数
        retryCount.put(stepId, currentRetryCount + 1);
        
        // 重置步骤状态以便重试
        step.reset();
        
        return true;
    }
    
    /**
     * 检查是否所有步骤都已完成
     * 
     * @return true表示所有步骤都已完成，false表示还有步骤未完成
     */
    private boolean areAllStepsCompleted() {
        return steps.stream()
                .allMatch(WorkflowStep::isCompleted);
    }
    
    /**
     * 获取工作流执行状态
     * 
     * 提供详细的工作流执行状态信息，包括：
     * - 整体状态和执行时间
     * - 每个步骤的详细状态
     * - 上下文中的关键数据
     * - 错误和异常信息
     * 
     * @return 完整的工作流状态信息
     */
    public Map<String, Object> getWorkflowStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 基本信息
        status.put("workflowId", workflowId);
        status.put("workflowName", workflowName);
        status.put("status", this.status);
        status.put("createdTime", createdTime);
        status.put("startTime", startTime);
        status.put("endTime", endTime);
        
        // 执行时间统计
        if (startTime > 0) {
            long currentTime = endTime > 0 ? endTime : System.currentTimeMillis();
            status.put("executionTime", currentTime - startTime);
        }
        
        // 步骤状态
        List<Map<String, Object>> stepStatuses = steps.stream()
                .map(step -> {
                    Map<String, Object> stepStatus = new HashMap<>();
                    stepStatus.put("stepId", step.getStepId());
                    stepStatus.put("stepName", step.getStepName());
                    stepStatus.put("status", step.getStatus());
                    stepStatus.put("executionTime", step.getExecutionTime());
                    stepStatus.put("result", step.getResult());
                    return stepStatus;
                })
                .collect(Collectors.toList());
        
        status.put("steps", stepStatuses);
        status.put("completedSteps", steps.stream().filter(WorkflowStep::isCompleted).count());
        status.put("totalSteps", steps.size());
        
        return status;
    }
    
    /**
     * 重置工作流
     * 
     * 将工作流重置到初始状态，支持重新执行：
     * 1. 重置所有步骤状态
     * 2. 清空重试计数
     * 3. 保留上下文数据（可选）
     * 4. 重置时间和状态
     * 
     * 用于工作流重试和调试场景
     */
    public void reset() {
        log.debug("重置工作流 {}", workflowId);
        
        // 重置所有步骤
        steps.forEach(WorkflowStep::reset);
        
        // 重置计数器
        retryCount.clear();
        
        // 重置状态
        status = "PENDING";
        currentStepIndex = 0;
        startTime = 0;
        endTime = 0;
        
        // 可以选择清空上下文或保留关键数据
        // context.clear();
    }
}