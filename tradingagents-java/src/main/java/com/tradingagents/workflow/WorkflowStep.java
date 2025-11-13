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
 * 工作流步骤 - 智能体协作的基本执行单元
 * 
 * 设计思想：
 * 1. 状态机模式：每个步骤都有明确的生命周期状态（PENDING → RUNNING → COMPLETED/FAILED）
 * 2. 条件驱动：通过WorkflowCondition实现灵活的条件判断机制
 * 3. 依赖管理：支持步骤间的依赖关系，确保执行顺序的正确性
 * 4. 超时控制：内置超时机制，防止步骤执行时间过长
 * 5. 工厂模式：提供静态工厂方法，简化特定类型步骤的创建
 * 
 * 核心职责：
 * - 定义步骤的基本属性（ID、名称、类型）
 * - 管理步骤的执行状态
 * - 处理步骤间的依赖关系
 * - 提供条件执行机制
 * - 记录执行结果和性能指标
 * 
 * 使用场景：
 * - 技术分析步骤：调用TechnicalAnalyst进行指标分析
 * - 基本面分析步骤：调用FundamentalAnalyst进行财务分析
 * - 风险评估步骤：调用RiskAnalyst评估投资风险
 * - 交易决策步骤：调用TradingAgent制定交易策略
 * - 协调步骤：管理多个智能体的协作过程
 * 
 * 线程安全：
 * 使用ConcurrentHashMap存储参数，支持多线程环境下的安全访问
 * 状态变更通过原子操作保证一致性
 */
@Data
@Slf4j
public class WorkflowStep {
    
    /**
     * 步骤唯一标识符
     * 用于在工作流引擎中唯一标识此步骤
     */
    private String stepId;
    
    /**
     * 步骤显示名称
     * 用于日志记录和用户界面显示
     */
    private String stepName;
    
    /**
     * 步骤类型
     * 定义步骤的业务类型，支持以下值：
     * - ANALYSIS: 分析类步骤（如技术分析、基本面分析）
     * - DEBATE: 辩论类步骤（如投资辩论、风险讨论）
     * - RISK_ASSESSMENT: 风险评估步骤
     * - DECISION: 决策类步骤（如交易决策）
     * - COORDINATION: 协调类步骤（管理智能体协作）
     */
    private String stepType;
    
    /**
     * 执行条件列表
     * 定义步骤执行前必须满足的条件
     * 所有条件都必须满足，步骤才能执行
     */
    private List<WorkflowCondition> conditions;
    
    /**
     * 步骤参数映射
     * 存储步骤执行所需的配置参数
     * 使用ConcurrentHashMap保证线程安全
     */
    private Map<String, Object> parameters;
    
    /**
     * 依赖步骤ID列表
     * 定义此步骤依赖的其他步骤
     * 所有依赖步骤完成后，此步骤才能执行
     */
    private List<String> dependencies;
    
    /**
     * 步骤完成标志
     * true表示步骤已成功完成，false表示未完成或失败
     */
    private boolean isCompleted;
    
    /**
     * 步骤活跃标志
     * true表示步骤正在执行中，false表示未在执行
     * 用于防止步骤的重复执行
     */
    private boolean isActive;
    
    /**
     * 步骤当前状态
     * 标准状态流转：PENDING → RUNNING → COMPLETED/FAILED
     */
    private String status;
    
    /**
     * 步骤执行结果
     * 存储步骤的输出结果或错误信息
     */
    private String result;
    
    /**
     * 步骤开始执行时间戳（毫秒）
     * 用于计算执行耗时
     */
    private long startTime;
    
    /**
     * 步骤结束执行时间戳（毫秒）
     * 用于计算执行耗时
     */
    private long endTime;
    
    /**
     * 步骤超时时间（毫秒）
     * 默认5分钟（300000毫秒）
     * 超过此时间未完成的步骤将被标记为超时
     */
    private long timeout;
    
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
     * 添加执行条件
     * 
     * 条件机制允许步骤根据运行时上下文动态决定是否执行
     * 例如：只有在市场数据可用时才执行技术分析步骤
     * 
     * @param condition 要添加的条件对象，包含参数名、期望值和比较操作符
     */
    public void addCondition(WorkflowCondition condition) {
        conditions.add(condition);
        log.debug("添加条件到步骤 {}: {}", stepId, condition.getDescription());
    }
    
    /**
     * 添加执行参数
     * 
     * 参数用于配置步骤的具体行为，例如：
     * - 分析周期（日线、周线、月线）
     * - 技术指标参数（MA周期、RSI参数等）
     * - 风险偏好设置
     * - 阈值配置
     * 
     * @param key 参数名称
     * @param value 参数值，可以是任意类型（字符串、数字、列表、映射等）
     */
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
        log.debug("添加参数到步骤 {}: {} = {}", stepId, key, value);
    }
    
    /**
     * 添加步骤依赖
     * 
     * 依赖机制确保步骤按正确的顺序执行：
     * - 风险评估步骤必须在技术分析和基本面分析完成后执行
     * - 交易决策步骤必须在所有分析步骤完成后执行
     * - 协调步骤可能依赖于多个前置步骤的结果
     * 
     * @param dependencyStepId 依赖的步骤ID，此步骤必须完成后当前步骤才能执行
     */
    public void addDependency(String dependencyStepId) {
        dependencies.add(dependencyStepId);
        log.debug("添加依赖到步骤 {}: {}", stepId, dependencyStepId);
    }
    
    /**
     * 检查步骤是否可以执行
     * 
     * 这是步骤调度的核心逻辑，检查以下所有条件：
     * 1. 步骤不能已经执行完成（避免重复执行）
     * 2. 步骤不能正在执行中（避免并发执行）
     * 3. 所有依赖步骤必须已完成（通过外部检查）
     * 4. 所有执行条件必须满足（通过条件评估）
     * 
     * 注意：依赖检查由WorkflowEngine负责，此方法只检查步骤内部状态
     * 
     * @param context 运行时上下文，包含所有步骤的执行结果和共享数据
     * @return true表示步骤可以执行，false表示步骤不能执行
     */
    public boolean canExecute(Map<String, Object> context) {
        // 检查是否已完成 - 已完成的步骤不需要重复执行
        if (isCompleted) {
            log.debug("步骤 {} 已完成，跳过执行", stepId);
            return false;
        }
        
        // 检查是否正在执行 - 防止步骤的并发执行
        if (isActive) {
            log.debug("步骤 {} 正在执行中，跳过重复执行", stepId);
            return false;
        }
        
        // 检查所有执行条件
        // 条件机制允许步骤根据运行时数据动态决定是否执行
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
     * 开始执行步骤
     * 
     * 状态转换：PENDING → RUNNING
     * 记录开始时间用于性能监控
     * 设置活跃标志防止重复执行
     * 
     * 注意：此方法必须在canExecute()返回true后调用
     */
    public void startExecution() {
        this.isActive = true;
        this.status = "RUNNING";
        this.startTime = System.currentTimeMillis();
        log.debug("开始执行步骤 {}: {}", stepId, stepName);
    }
    
    /**
     * 成功完成步骤执行
     * 
     * 状态转换：RUNNING → COMPLETED
     * 计算执行耗时用于性能分析
     * 存储执行结果供后续步骤使用
     * 
     * @param result 步骤执行结果，可以是分析结果、决策建议、错误信息等
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
     * 步骤执行失败
     * 
     * 状态转换：RUNNING → FAILED
     * 保持完成标志为false，表示步骤未成功完成
     * 错误信息将传递给工作流引擎进行错误处理
     * 
     * @param errorMessage 错误描述信息，用于问题诊断和日志记录
     */
    public void failExecution(String errorMessage) {
        this.isActive = false;
        this.status = "FAILED";
        this.result = errorMessage;
        this.endTime = System.currentTimeMillis();
        log.error("步骤 {} 执行失败: {}", stepId, errorMessage);
    }
    
    /**
     * 检查步骤是否超时
     * 
     * 超时机制防止步骤执行时间过长导致整个工作流阻塞
     * 超时的步骤应该被中断或标记为失败
     * 
     * @return true表示步骤已超时，false表示未超时或不在执行中
     */
    public boolean isTimeout() {
        if (!isActive || startTime == 0) {
            return false;
        }
        return (System.currentTimeMillis() - startTime) > timeout;
    }
    
    /**
     * 获取步骤执行时间
     * 
     * @return 执行耗时（毫秒），如果步骤未开始返回0，如果正在执行返回已用时间
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
     * 获取步骤状态描述
     * 
     * 提供人类可读的状态信息，包含：
     * - 步骤基本信息（ID、名称）
     * - 当前状态
     * - 执行耗时
     * - 执行结果
     * 
     * @return 格式化的状态描述字符串
     */
    public String getStatusDescription() {
        return String.format("步骤 %s (%s): %s, 耗时: %dms, 结果: %s",
                stepId, stepName, status, getExecutionTime(), result);
    }
    
    /**
     * 重置步骤状态
     * 
     * 重置机制支持：
     * 1. 工作流重试 - 当整个工作流需要重新执行时
     * 2. 步骤重试 - 当单个步骤失败但需要重试时
     * 3. 工作流重启 - 当工作流状态需要完全重置时
     * 
     * 重置后步骤回到初始状态，可以重新执行
     */
    public void reset() {
        this.isActive = false;
        this.isCompleted = false;
        this.status = "PENDING";
        this.result = null;
        this.startTime = 0;
        this.endTime = 0;
        log.debug("重置步骤 {}: {}", stepId, stepName);
    }
    
    /**
     * 创建分析类型步骤
     * 
     * 分析步骤负责处理数据并生成分析结果：
     * - 技术分析：计算技术指标、识别趋势
     * - 基本面分析：评估公司财务状况、行业地位
     * - 情绪分析：分析市场情绪、新闻影响
     * 
     * @param stepId 步骤唯一标识符
     * @param stepName 步骤显示名称
     * @param analysisType 分析类型，如"technical", "fundamental", "sentiment"
     * @return 配置好的分析步骤实例
     */
    public static WorkflowStep createAnalysisStep(String stepId, String stepName, String analysisType) {
        WorkflowStep step = new WorkflowStep(stepId, stepName, "ANALYSIS");
        step.addParameter("analysisType", analysisType);
        step.addParameter("timeout", 30000); // 30秒超时
        return step;
    }
    
    /**
     * 创建辩论类型步骤
     * 
     * 辩论步骤模拟多个智能体之间的讨论：
     * - 多方观点碰撞，生成更全面的分析
     * - 通过辩论识别潜在风险和机会
     * - 达成共识或保留不同意见
     * 
     * @param stepId 步骤唯一标识符
     * @param stepName 步骤显示名称
     * @param debateTopic 辩论主题
     * @param participants 参与辩论的智能体列表
     * @return 配置好的辩论步骤实例
     */
    public static WorkflowStep createDebateStep(String stepId, String stepName, String debateTopic, List<String> participants) {
        WorkflowStep step = new WorkflowStep(stepId, stepName, "DEBATE");
        step.addParameter("debateTopic", debateTopic);
        step.addParameter("participants", participants);
        step.addParameter("timeout", 60000); // 60秒超时，辩论需要更多时间
        return step;
    }
    
    /**
     * 创建风险评估类型步骤
     * 
     * 风险评估步骤综合分析结果，识别潜在风险：
     * - 市场风险：价格波动、流动性风险
     * - 信用风险：交易对手违约风险
     * - 操作风险：系统故障、人为错误
     * - 合规风险：监管政策变化
     * 
     * @param stepId 步骤唯一标识符
     * @param stepName 步骤显示名称
     * @param riskFactors 风险因子列表，如["market", "credit", "operational"]
     * @return 配置好的风险评估步骤实例
     */
    public static WorkflowStep createRiskAssessmentStep(String stepId, String stepName, List<String> riskFactors) {
        WorkflowStep step = new WorkflowStep(stepId, stepName, "RISK_ASSESSMENT");
        step.addParameter("riskFactors", riskFactors);
        step.addParameter("timeout", 45000); // 45秒超时
        return step;
    }
    
    /**
     * 创建决策类型步骤
     * 
     * 决策步骤基于前面的分析结果做出最终决策：
     * - 买入/卖出/持有决策
     * - 仓位大小决策
     * - 时机选择决策
     * - 止损止盈设置
     * 
     * @param stepId 步骤唯一标识符
     * @param stepName 步骤显示名称
     * @param decisionType 决策类型，如"trade", "position", "timing"
     * @return 配置好的决策步骤实例
     */
    public static WorkflowStep createDecisionStep(String stepId, String stepName, String decisionType) {
        WorkflowStep step = new WorkflowStep(stepId, stepName, "DECISION");
        step.addParameter("decisionType", decisionType);
        step.addParameter("timeout", 30000); // 30秒超时
        return step;
    }
    
    /**
     * 创建协调类型步骤
     * 
     * 协调步骤负责管理多个智能体的协作：
     * - 分配任务给不同的智能体
     * - 协调智能体之间的通信
     * - 解决智能体之间的冲突
     * - 整合多个智能体的输出
     * 
     * @param stepId 步骤唯一标识符
     * @param stepName 步骤显示名称
     * @param coordinationType 协调类型，如"task_allocation", "conflict_resolution", "output_integration"
     * @return 配置好的协调步骤实例
     */
    public static WorkflowStep createCoordinationStep(String stepId, String stepName, String coordinationType) {
        WorkflowStep step = new WorkflowStep(stepId, stepName, "COORDINATION");
        step.addParameter("coordinationType", coordinationType);
        step.addParameter("timeout", 40000); // 40秒超时
        return step;
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