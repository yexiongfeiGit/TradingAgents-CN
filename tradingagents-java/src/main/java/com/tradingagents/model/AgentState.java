/**
 * 智能体状态模型 - 多智能体系统的核心数据结构
 * 
 * 核心职责：
 * 1. 状态管理 - 维护智能体的运行时状态信息
 * 2. 消息传递 - 支持智能体间的异步通信
 * 3. 数据共享 - 提供统一的数据访问接口
 * 4. 上下文维护 - 保存对话历史和执行上下文
 * 5. 配置管理 - 存储智能体配置参数
 * 
 * 设计思想：
 * - 不可变模式：核心状态对象采用不可变设计，确保线程安全
 * - 建造者模式：使用Builder模式简化复杂对象的创建
 * - 观察者模式：状态变化通知相关组件
 * - 策略模式：不同类型的智能体采用不同的状态管理策略
 * 
 * 使用场景：
 * - 多智能体对话系统
 * - 协作式股票分析
 * - 智能体辩论和决策
 * - 状态持久化和恢复
 * 
 * 线程安全：
 * - 使用不可变对象保证线程安全
 * - 状态变更通过副本机制实现
 * - 支持并发访问和修改
 */
package com.tradingagents.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体状态主类
 * 
 * 包含智能体的完整状态信息：
 * - 基本信息：ID、名称、类型、角色
 * - 状态信息：当前状态、可用性、置信度
 * - 能力信息：支持的功能、专长领域
 * - 上下文信息：消息历史、共享数据、配置参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {
    
    /**
     * 智能体唯一标识符
     * 全局唯一，用于区分不同的智能体实例
     */
    private String agentId;
    
    /**
     * 智能体名称
     * 人类可读的名称，用于显示和日志记录
     */
    private String name;
    
    /**
     * 智能体类型
     * 定义智能体的基本类型：
     * - TECHNICAL_ANALYST: 技术分析师
     * - FUNDAMENTAL_ANALYST: 基本面分析师
     * - MARKET_ANAYLYST: 市场分析师（修正了拼写错误）
     * - RISK_ASSESSOR: 风险评估师
     * - TRADER: 交易员
     * - COORDINATOR: 协调员
     */
    private String agentType;
    
    /**
     * 智能体角色描述
     * 详细描述智能体的职责和专长
     */
    private String role;
    
    /**
     * 智能体当前状态
     * 表示智能体的运行状态：
     * - IDLE: 空闲状态，等待任务
     * - ANALYZING: 分析中
     * - DEBATING: 辩论中
     * - DECIDING: 决策中
     * - TRADING: 交易中
     * - ERROR: 错误状态
     */
    private String status;
    
    /**
     * 智能体是否可用
     * 表示智能体是否可以接收新任务
     */
    private boolean isAvailable;
    
    /**
     * 置信度水平
     * 智能体对其分析结果的置信程度，范围0.0-1.0
     */
    private double confidenceLevel;
    
    /**
     * 当前分析的股票代码
     * 智能体当前正在分析的股票
     */
    private String currentStock;
    
    /**
     * 消息历史列表
     * 存储智能体的所有消息记录，支持对话历史追踪
     */
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    
    /**
     * 共享上下文数据
     * 智能体间共享的数据，如市场数据、分析结果等
     * 使用ConcurrentHashMap保证线程安全
     */
    @Builder.Default
    private Map<String, Object> sharedContext = new ConcurrentHashMap<>();
    
    /**
     * 智能体配置参数
     * 特定于智能体类型的配置参数
     */
    @Builder.Default
    private Map<String, Object> configuration = new HashMap<>();
    
    /**
     * 智能体能力列表
     * 定义智能体具备的能力和功能
     */
    @Builder.Default
    private List<String> capabilities = new ArrayList<>();
    
    /**
     * 最后活跃时间
     * 用于监控智能体的活跃状态
     */
    private long lastActiveTime;
    
    /**
     * 创建时间
     * 智能体实例的创建时间
     */
    private long createdTime;
    
    /**
     * 错误信息
     * 当智能体处于ERROR状态时，记录错误详情
     */
    private String errorInfo;
    
    /**
     * 内部类：消息模型
     * 
     * 表示智能体间的通信消息，包含：
     * - 发送者和接收者信息
     * - 消息内容和类型
     * - 时间戳和状态
     * - 工具调用信息（支持函数调用）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        
        /**
         * 消息ID
         * 消息的唯一标识符
         */
        private String messageId;
        
        /**
         * 发送者ID
         * 发送此消息的智能体ID
         */
        private String senderId;
        
        /**
         * 接收者ID
         * 接收此消息的智能体ID，可为空（广播消息）
         */
        private String receiverId;
        
        /**
         * 消息内容
         * 消息的主要内容
         */
        private String content;
        
        /**
         * 消息类型
         * 定义消息的类型：
         * - REQUEST: 请求消息
         * - RESPONSE: 响应消息
         * - NOTIFICATION: 通知消息
         * - DEBATE: 辩论消息
         * - ERROR: 错误消息
         */
        private String messageType;
        
        /**
         * 时间戳
         * 消息创建的时间戳
         */
        private long timestamp;
        
        /**
         * 消息状态
         * 消息的当前状态：
         * - PENDING: 待发送
         * - SENT: 已发送
         * - DELIVERED: 已送达
         * - READ: 已读
         */
        private String status;
        
        /**
         * 工具调用列表
         * 消息中包含的工具调用请求
         */
        @Builder.Default
        private List<ToolCall> toolCalls = new ArrayList<>();
        
        /**
         * 元数据
         * 消息的附加信息
         */
        @Builder.Default
        private Map<String, Object> metadata = new HashMap<>();
        
        /**
         * 内部类：工具调用
         * 
         * 表示智能体可以调用的工具/函数
         * 支持外部API调用、数据库查询等操作
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ToolCall {
            
            /**
             * 工具ID
             * 工具的唯一标识符
             */
            private String toolId;
            
            /**
             * 工具名称
             * 要调用的工具/函数名称
             */
            private String toolName;
            
            /**
             * 工具参数
             * 调用工具时传递的参数
             */
            @Builder.Default
            private Map<String, Object> parameters = new HashMap<>();
            
            /**
             * 调用结果
             * 工具执行的结果
             */
            private Object result;
            
            /**
             * 调用状态
             * 工具调用的状态：
             * - PENDING: 待调用
             * - EXECUTING: 执行中
             * - COMPLETED: 已完成
             * - FAILED: 调用失败
             */
            private String status;
            
            /**
             * 错误信息
             * 当调用失败时记录的错误信息
             */
            private String error;
            
            /**
             * 开始时间
             * 工具调用开始的时间戳
             */
            private long startTime;
            
            /**
             * 结束时间
             * 工具调用结束的时间戳
             */
            private long endTime;
        }
    }
    
    /**
     * 内部类：市场信息
     * 
     * 包含股票的市场数据：
     * - 基本市场信息
     * - 价格和交易量数据
     * - 技术指标和信号
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketInfo {
        
        /**
         * 市场类型
         * 股票所在的市场：
         * - SH: 上海交易所
         * - SZ: 深圳交易所
         * - HK: 香港交易所
         * - US: 美国市场
         */
        private String marketType;
        
        /**
         * 当前价格
         * 股票的当前市场价格
         */
        private double currentPrice;
        
        /**
         * 价格变化
         * 相比前一交易日的价格变化
         */
        private double priceChange;
        
        /**
         * 价格变化百分比
         * 相比前一交易日的价格变化百分比
         */
        private double priceChangePercent;
        
        /**
         * 交易量
         * 当日交易量
         */
        private long volume;
        
        /**
         * 市值
         * 股票的总市值
         */
        private double marketCap;
        
        /**
         * 技术指标
         * 技术分析指标数据
         */
        @Builder.Default
        private Map<String, Object> technicalIndicators = new HashMap<>();
        
        /**
         * 市场信号
         * 市场分析信号和建议
         */
        @Builder.Default
        private List<String> marketSignals = new ArrayList<>();
    }
    
    /**
     * 内部类：公司信息
     * 
     * 包含股票对应公司的基本信息：
     * - 公司基本资料
     * - 财务数据
     * - 业务信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfo {
        
        /**
         * 股票代码
         * 股票的唯一代码
         */
        private String stockCode;
        
        /**
         * 公司名称
         * 公司的正式名称
         */
        private String companyName;
        
        /**
         * 市场信息
         * 股票的市场数据
         */
        private MarketInfo marketInfo;
        
        /**
         * 行业分类
         * 公司所属的行业
         */
        private String industry;
        
        /**
         * 成立时间
         * 公司成立日期
         */
        private String foundedDate;
        
        /**
         * 员工数量
         * 公司员工总数
         */
        private int employeeCount;
        
        /**
         * 主营业务
         * 公司的主要业务范围
         */
        @Builder.Default
        private List<String> mainBusiness = new ArrayList<>();
        
        /**
         * 财务指标
         * 关键财务数据和比率
         */
        @Builder.Default
        private Map<String, Double> financialMetrics = new HashMap<>();
        
        /**
         * 公司新闻
         * 相关的公司新闻和公告
         */
        @Builder.Default
        private List<String> news = new ArrayList<>();
        
        /**
         * 获取股票名称
         * 与companyName相同，用于兼容性
         * 
         * @return 股票名称
         */
        public String getStockName() {
            return companyName;
        }
        
        /**
         * 获取市场类型
         * 从marketInfo中获取市场类型
         * 
         * @return 市场类型，如果marketInfo为null则返回null
         */
        public String getMarketType() {
            return marketInfo != null ? marketInfo.getMarketType() : null;
        }
    }
    
    /**
     * 内部类：分析结果
     * 
     * 包含智能体的分析结果：
     * - 分析结论
     * - 置信度评分
     * - 建议和风险提示
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResult {
        
        /**
         * 分析类型
         * 分析的具体类型：
         * - TECHNICAL: 技术分析
         * - FUNDAMENTAL: 基本面分析
         * - SENTIMENT: 情绪分析
         * - RISK: 风险分析
         * - COMPREHENSIVE: 综合分析
         */
        private String analysisType;
        
        /**
         * 分析结论
         * 主要的分析结论和观点
         */
        private String conclusion;
        
        /**
         * 置信度评分
         * 对分析结论的置信程度，0.0-1.0
         */
        private double confidenceScore;
        
        /**
         * 建议操作
         * 基于分析结果的建议操作：
         * - BUY: 买入
         * - SELL: 卖出
         * - HOLD: 持有
         * - WATCH: 观望
         */
        private String recommendation;
        
        /**
         * 目标价格
         * 预期的目标价格
         */
        private double targetPrice;
        
        /**
         * 时间框架
         * 分析适用的时间框架：
         * - SHORT_TERM: 短期（1-7天）
         * - MEDIUM_TERM: 中期（1-4周）
         * - LONG_TERM: 长期（1-12个月）
         */
        private String timeFrame;
        
        /**
         * 风险等级
         * 投资的风险等级：
         * - LOW: 低风险
         * - MEDIUM: 中等风险
         * - HIGH: 高风险
         */
        private String riskLevel;
        
        /**
         * 关键指标
         * 支撑分析的关键数据指标
         */
        @Builder.Default
        private Map<String, Object> keyMetrics = new HashMap<>();
        
        /**
         * 风险提示
         * 需要注意的风险因素
         */
        @Builder.Default
        private List<String> riskWarnings = new ArrayList<>();
        
        /**
         * 支撑论据
         * 支撑分析结论的主要论据
         */
        @Builder.Default
        private List<String> supportingArguments = new ArrayList<>();
    }
    
    /**
     * 添加消息到历史记录
     * 
     * @param message 要添加的消息
     */
    public void addMessage(Message message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 获取指定类型的消息
     * 
     * @param messageType 消息类型
     * @return 该类型的消息列表
     */
    public List<Message> getMessagesByType(String messageType) {
        if (messages == null) {
            return new ArrayList<>();
        }
        return messages.stream()
                .filter(msg -> messageType.equals(msg.getMessageType()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 获取与指定智能体的对话
     * 
     * @param agentId 智能体ID
     * @return 与该智能体的消息列表
     */
    public List<Message> getConversationWithAgent(String agentId) {
        if (messages == null) {
            return new ArrayList<>();
        }
        return messages.stream()
                .filter(msg -> agentId.equals(msg.getSenderId()) || agentId.equals(msg.getReceiverId()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 更新共享上下文
     * 
     * @param key 上下文键
     * @param value 上下文值
     */
    public void updateSharedContext(String key, Object value) {
        if (sharedContext == null) {
            sharedContext = new ConcurrentHashMap<>();
        }
        sharedContext.put(key, value);
        lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 获取共享上下文中的值
     * 
     * @param key 上下文键
     * @return 上下文值，如果不存在返回null
     */
    public Object getSharedContextValue(String key) {
        return sharedContext != null ? sharedContext.get(key) : null;
    }
    
    /**
     * 更新配置参数
     * 
     * @param key 配置键
     * @param value 配置值
     */
    public void updateConfiguration(String key, Object value) {
        if (configuration == null) {
            configuration = new HashMap<>();
        }
        configuration.put(key, value);
    }
    
    /**
     * 获取配置参数
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在返回默认值
     */
    public Object getConfigurationValue(String key, Object defaultValue) {
        return configuration != null ? configuration.getOrDefault(key, defaultValue) : defaultValue;
    }
    
    /**
     * 添加能力
     * 
     * @param capability 能力描述
     */
    public void addCapability(String capability) {
        if (capabilities == null) {
            capabilities = new ArrayList<>();
        }
        if (!capabilities.contains(capability)) {
            capabilities.add(capability);
        }
    }
    
    /**
     * 检查是否具备指定能力
     * 
     * @param capability 能力描述
     * @return 是否具备该能力
     */
    public boolean hasCapability(String capability) {
        return capabilities != null && capabilities.contains(capability);
    }
    
    /**
     * 设置错误状态
     * 
     * @param errorInfo 错误信息
     */
    public void setError(String errorInfo) {
        this.status = "ERROR";
        this.errorInfo = errorInfo;
        this.isAvailable = false;
        lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 清除错误状态
     */
    public void clearError() {
        if ("ERROR".equals(this.status)) {
            this.status = "IDLE";
            this.errorInfo = null;
            this.isAvailable = true;
        }
    }
    
    /**
     * 获取状态摘要
     * 
     * @return 状态摘要信息
     */
    public String getStatusSummary() {
        return String.format("Agent[%s] %s (%s) - %s - Confidence: %.2f - Available: %s",
                agentId, name, agentType, status, confidenceLevel, isAvailable);
    }
    
    /**
     * 获取公司信息
     * 
     * @return 公司信息对象，如果不存在则返回null
     */
    public CompanyInfo getCompanyInfo() {
        return (CompanyInfo) getSharedContextValue("companyInfo");
    }
    
    /**
     * 设置公司信息
     * 
     * @param companyInfo 公司信息对象
     */
    public void setCompanyInfo(CompanyInfo companyInfo) {
        updateSharedContext("companyInfo", companyInfo);
    }
    
    /**
     * 获取市场分析
     * 
     * @return 市场分析报告，如果不存在则返回null
     */
    public String getMarketAnalysis() {
        return (String) getSharedContextValue("marketAnalysis");
    }
    
    /**
     * 设置市场分析
     * 
     * @param marketAnalysis 市场分析报告
     */
    public void setMarketAnalysis(String marketAnalysis) {
        updateSharedContext("marketAnalysis", marketAnalysis);
    }
    
    /**
     * 获取情绪报告
     * 
     * @return 情绪分析报告，如果不存在则返回null
     */
    public String getSentimentReport() {
        return (String) getSharedContextValue("sentimentReport");
    }
    
    /**
     * 设置情绪报告
     * 
     * @param sentimentReport 情绪分析报告
     */
    public void setSentimentReport(String sentimentReport) {
        updateSharedContext("sentimentReport", sentimentReport);
    }
    
    /**
     * 获取新闻报告
     * 
     * @return 新闻分析报告，如果不存在则返回null
     */
    public String getNewsReport() {
        return (String) getSharedContextValue("newsReport");
    }
    
    /**
     * 设置新闻报告
     * 
     * @param newsReport 新闻分析报告
     */
    public void setNewsReport(String newsReport) {
        updateSharedContext("newsReport", newsReport);
    }
    
    /**
     * 获取基本面报告
     * 
     * @return 基本面分析报告，如果不存在则返回null
     */
    public String getFundamentalsReport() {
        return (String) getSharedContextValue("fundamentalsReport");
    }
    
    /**
     * 设置基本面报告
     * 
     * @param fundamentalsReport 基本面分析报告
     */
    public void setFundamentalsReport(String fundamentalsReport) {
        updateSharedContext("fundamentalsReport", fundamentalsReport);
    }
    
    /**
     * 获取投资辩论状态
     * 
     * @return 投资辩论状态，如果不存在则返回null
     */
    public InvestmentDebateState getInvestmentDebateState() {
        return (InvestmentDebateState) getSharedContextValue("investmentDebateState");
    }
    
    /**
     * 设置投资辩论状态
     * 
     * @param investmentDebateState 投资辩论状态
     */
    public void setInvestmentDebateState(InvestmentDebateState investmentDebateState) {
        updateSharedContext("investmentDebateState", investmentDebateState);
    }
    
    /**
     * 获取风险讨论状态
     * 
     * @return 风险讨论状态，如果不存在则返回null
     */
    public RiskDebateState getRiskDiscussionState() {
        return (RiskDebateState) getSharedContextValue("riskDiscussionState");
    }
    
    /**
     * 设置风险讨论状态
     * 
     * @param riskDiscussionState 风险讨论状态
     */
    public void setRiskDiscussionState(RiskDebateState riskDiscussionState) {
        updateSharedContext("riskDiscussionState", riskDiscussionState);
    }
    
    /**
     * 获取投资组合分析
     * 
     * @return 投资组合分析报告，如果不存在则返回null
     */
    public String getPortfolioAnalysis() {
        return (String) getSharedContextValue("portfolioAnalysis");
    }
    
    /**
     * 设置投资组合分析
     * 
     * @param portfolioAnalysis 投资组合分析报告
     */
    public void setPortfolioAnalysis(String portfolioAnalysis) {
        updateSharedContext("portfolioAnalysis", portfolioAnalysis);
    }
    
    /**
     * 增加工具调用计数
     * 
     * @param toolName 工具名称
     */
    public void incrementToolCallCount(String toolName) {
        Map<String, Integer> toolCallCounts = (Map<String, Integer>) getSharedContextValue("toolCallCounts");
        if (toolCallCounts == null) {
            toolCallCounts = new HashMap<>();
            updateSharedContext("toolCallCounts", toolCallCounts);
        }
        toolCallCounts.merge(toolName, 1, Integer::sum);
    }
    
    /**
     * 获取最后一条消息
     * 
     * @return 最后一条消息，如果不存在则返回null
     */
    public Message getLastMessage() {
        return messages != null && !messages.isEmpty() ? messages.get(messages.size() - 1) : null;
    }
    
    /**
     * 获取消息内容列表
     * 
     * @return 消息内容列表
     */
    public List<String> getMessages() {
        if (messages == null) {
            return new ArrayList<>();
        }
        List<String> messageContents = new ArrayList<>();
        for (Message message : messages) {
            if (message.getContent() != null) {
                messageContents.add(message.getContent());
            }
        }
        return messageContents;
    }
    
    /**
     * 内部类：投资辩论状态
     * 
     * 管理投资辩论的状态信息：
     * - 多方观点和空方观点
     * - 辩论历史和当前响应
     * - 法官决策和计数器
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentDebateState {
        
        /**
         * 综合讨论历史
         * 存储所有辩论参与者的综合讨论内容
         */
        private String history;
        
        /**
         * 多头观点历史
         * 存储看涨方的观点和论据
         */
        private String bullHistory;
        
        /**
         * 空头观点历史
         * 存储看跌方的观点和论据
         */
        private String bearHistory;
        
        /**
         * 当前响应
         * 当前辩论轮次的响应内容
         */
        private String currentResponse;
        
        /**
         * 发言计数
         * 记录辩论发言的总次数
         */
        private int count;
        
        /**
         * 法官决策
         * 最终的投资决策结果
         */
        private String judgeDecision;
        
        /**
         * 附加属性
         * 存储其他自定义属性
         */
        @Builder.Default
        private Map<String, Object> additionalProperties = new HashMap<>();
        
        /**
         * 获取辩论历史
         * 
         * @return 格式化的辩论历史列表
         */
        public List<String> getDebateHistory() {
            List<String> history = new ArrayList<>();
            if (bullHistory != null && !bullHistory.isEmpty()) {
                history.add("多头观点: " + bullHistory);
            }
            if (bearHistory != null && !bearHistory.isEmpty()) {
                history.add("空头观点: " + bearHistory);
            }
            if (this.history != null && !this.history.isEmpty()) {
                history.add("综合讨论: " + this.history);
            }
            return history;
        }
        
        /**
         * 添加多头观点
         * 
         * @param argument 多头论据
         */
        public void addBullArgument(String argument) {
            if (bullHistory == null) {
                bullHistory = argument;
            } else {
                bullHistory += "\n" + argument;
            }
        }
        
        /**
         * 添加空头观点
         * 
         * @param argument 空头论据
         */
        public void addBearArgument(String argument) {
            if (bearHistory == null) {
                bearHistory = argument;
            } else {
                bearHistory += "\n" + argument;
            }
        }
        
        /**
         * 增加多头发言计数
         */
        public void incrementBullSpeeches() {
            count++;
        }
        
        /**
         * 增加空头发言计数
         */
        public void incrementBearSpeeches() {
            count++;
        }
        
        /**
         * 获取多头发言次数
         * 
         * @return 多头发言次数
         */
        public int getBullSpeeches() {
            return count;
        }
        
        /**
         * 获取空头发言次数
         * 
         * @return 空头发言次数
         */
        public int getBearSpeeches() {
            return count;
        }
    }
    
    /**
     * 内部类：风险讨论状态
     * 
     * 管理风险讨论的状态信息：
     * - 不同风险偏好的观点
     * - 讨论历史和最终决策
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskDebateState {
        
        /**
         * 综合讨论历史
         * 存储所有风险讨论的综合内容
         */
        private String history;
        
        /**
         * 激进观点历史
         * 存储激进投资策略的观点
         */
        private String aggressiveHistory;
        
        /**
         * 保守观点历史
         * 存储保守投资策略的观点
         */
        private String conservativeHistory;
        
        /**
         * 中性观点历史
         * 存储中性投资策略的观点
         */
        private String neutralHistory;
        
        /**
         * 当前响应
         * 当前讨论轮次的响应内容
         */
        private String currentResponse;
        
        /**
         * 发言计数
         * 记录风险讨论发言的总次数
         */
        private int count;
        
        /**
         * 最终决策
         * 风险讨论的最终决策结果
         */
        private String finalDecision;
        
        /**
         * 附加属性
         * 存储其他自定义属性
         */
        @Builder.Default
        private Map<String, Object> additionalProperties = new HashMap<>();
        
        /**
         * 添加风险分析
         * 
         * @param analysis 风险分析内容
         */
        public void addRiskAnalysis(String analysis) {
            if (history == null) {
                history = analysis;
            } else {
                history += "\n" + analysis;
            }
        }
        
        /**
         * 增加风险讨论发言计数
         */
        public void incrementRiskSpeeches() {
            count++;
        }
        
        /**
         * 获取风险分析列表
         * 
         * @return 格式化的风险分析列表
         */
        public List<String> getRiskAnalyses() {
            List<String> analyses = new ArrayList<>();
            if (aggressiveHistory != null && !aggressiveHistory.isEmpty()) {
                analyses.add("激进观点: " + aggressiveHistory);
            }
            if (conservativeHistory != null && !conservativeHistory.isEmpty()) {
                analyses.add("保守观点: " + conservativeHistory);
            }
            if (neutralHistory != null && !neutralHistory.isEmpty()) {
                analyses.add("中性观点: " + neutralHistory);
            }
            if (history != null && !history.isEmpty()) {
                analyses.add("综合讨论: " + history);
            }
            return analyses;
        }
        
        /**
         * 设置最终决策
         * 
         * @param finalDecision 最终决策结果
         */
        public void setFinalDecision(String finalDecision) {
            this.finalDecision = finalDecision;
        }
        
        /**
         * 获取最终决策
         * 
         * @return 最终决策结果
         */
        public String getFinalDecision() {
            return finalDecision;
        }
    }
    
    /**
     * 创建Builder实例的便捷方法
     * 
     * @return 新的Builder实例
     */
    public static AgentStateBuilder builder() {
        return new AgentStateBuilder()
                .status("IDLE")
                .isAvailable(true)
                .confidenceLevel(0.5)
                .lastActiveTime(System.currentTimeMillis())
                .createdTime(System.currentTimeMillis());
    }
}