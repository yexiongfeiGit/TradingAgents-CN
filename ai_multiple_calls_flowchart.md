# TradingAgents AI多次调用流程图

## 🔄 系统架构概览

```mermaid
graph TB
    Start([开始分析]) --> Init[初始化分析参数]
    Init --> Depth{选择研究深度}
    
    Depth -->|1级(最快)| QuickPath[快速分析路径]
    Depth -->|2级(快速)| FastPath[快速辩论路径] 
    Depth -->|3级(标准)| StandardPath[标准分析路径]
    Depth -->|4级(深度)| DeepPath[深度分析路径]
    Depth -->|5级(最深度)| UltraPath[超深度分析路径]
    
    QuickPath --> End([结束])
    FastPath --> End
    StandardPath --> End
    DeepPath --> End
    UltraPath --> End
```

## 📊 AI多次调用详细流程

### 基础分析师阶段（所有级别）

```mermaid
graph LR
    subgraph "分析师团队并行分析"
        Market[市场分析师<br/>最多3次AI调用] --> MarketReport[市场报告]
        Fundamentals[基本面分析师<br/>最多1次AI调用] --> FundamentalsReport[基本面报告]
        News[新闻分析师<br/>最多3次AI调用] --> NewsReport[新闻报告]
        Social[社交媒体分析师<br/>最多3次AI调用] --> SocialReport[情绪报告]
    end
    
    MarketReport --> ResearchPhase[研究阶段]
    FundamentalsReport --> ResearchPhase
    NewsReport --> ResearchPhase
    SocialReport --> ResearchPhase
```

### 研究辩论阶段（2-5级）

```mermaid
graph TD
    ResearchPhase[研究阶段] --> Bull1[🐂 看涨研究员<br/>第1轮发言]
    Bull1 --> Bear1[🐻 看跌研究员<br/>第1轮反驳]
    Bear1 --> Judge1[⚖️ 研究经理<br/>第1轮裁决]
    
    Judge1 --> Bull2[🐂 看涨研究员<br/>第2轮发言]
    Bull2 --> Bear2[🐻 看跌研究员<br/>第2轮反驳]
    Bear2 --> Judge2[⚖️ 研究经理<br/>第2轮裁决]
    
    Judge2 --> ConfigCheck{检查配置<br/>max_debate_rounds}
    ConfigCheck -->|轮次=3| Bull3[🐂 看涨研究员<br/>第3轮发言]
    Bull3 --> Bear3[🐻 看跌研究员<br/>第3轮反驳]
    Bear3 --> Judge3[⚖️ 研究经理<br/>第3轮裁决]
    Judge3 --> RiskPhase[风险管理阶段]
    
    ConfigCheck -->|轮次=2| RiskPhase
```

### 风险管理阶段（3-5级）

```mermaid
graph TD
    RiskPhase[风险管理阶段] --> Risky1[🔥 激进分析师<br/>第1轮发言]
    Risky1 --> Safe1[🛡️ 保守分析师<br/>第1轮反驳]
    Safe1 --> Neutral1[⚖️ 中性分析师<br/>第1轮平衡]
    
    Neutral1 --> Risky2[🔥 激进分析师<br/>第2轮发言]
    Risky2 --> Safe2[🛡️ 保守分析师<br/>第2轮反驳]
    Safe2 --> Neutral2[⚖️ 中性分析师<br/>第2轮平衡]
    
    Neutral2 --> Risky3[🔥 激进分析师<br/>第3轮发言]
    Risky3 --> Safe3[🛡️ 保守分析师<br/>第3轮反驳]
    Safe3 --> Neutral3[⚖️ 中性分析师<br/>第3轮平衡]
    
    Neutral3 --> RiskJudge[🎯 投资组合经理<br/>最终决策]
```

## 🔢 AI调用次数统计

### 各级别AI调用总数

| 研究深度 | 分析师团队 | 投资辩论 | 风险讨论 | **总计** |
|---------|------------|----------|----------|----------|
| 1级(最快) | 4-10次 | 0次 | 0次 | **4-10次** |
| 2级(快速) | 4-10次 | 4次 | 0次 | **8-14次** |
| 3级(标准) | 4-10次 | 6次 | 9次 | **19-25次** |
| 4级(深度) | 4-10次 | 6次 | 9次 | **19-25次** |
| 5级(最深度) | 4-10次 | 9次 | 9次 | **22-28次** |

### 详细调用分解

```mermaid
graph LR
    subgraph "单次分析最大调用数"
        A[市场分析师<br/>3次] --> B[基本面分析师<br/>1次]
        B --> C[新闻分析师<br/>3次]
        C --> D[社交媒体分析师<br/>3次]
        D --> E[总计:10次<br/>分析师阶段]
    end
    
    subgraph "投资辩论调用数"
        E --> F[看涨研究员<br/>每轮1次]
        F --> G[看跌研究员<br/>每轮1次]
        G --> H[研究经理<br/>每轮1次]
        H --> I[2轮:6次<br/>3轮:9次]
    end
    
    subgraph "风险讨论调用数"
        I --> J[激进分析师<br/>每轮1次]
        J --> K[保守分析师<br/>每轮1次]
        K --> L[中性分析师<br/>每轮1次]
        L --> M[固定:9次<br/>风险阶段]
    end
```

## ⚙️ 循环控制机制

### 防死循环保护

```mermaid
graph TD
    StartCall[开始AI调用] --> CheckCount{检查调用次数}
    CheckCount -->|超过限制| ForceEnd[强制结束<br/>防止死循环]
    CheckCount -->|未超限制| NormalCall[正常调用AI]
    
    NormalCall --> CheckResult{检查结果}
    CheckResult -->|需要更多数据| Increment[计数器+1<br/>重新调用]
    CheckResult -->|结果完整| SaveResult[保存结果<br/>结束调用]
    
    Increment --> CheckCount
    ForceEnd --> SaveResult
```

### 条件判断逻辑

```mermaid
graph TD
    CheckMessage{检查消息类型} -->|AIMessage| HasToolCalls{是否有工具调用?}
    CheckMessage -->|其他类型| CheckReport{检查报告完整性}
    
    HasToolCalls -->|有| ExecuteTool[执行工具调用]
    HasToolCalls -->|无| CheckReport
    
    ExecuteTool --> ToolCount{工具调用次数}
    ToolCount -->|超过max_tool_calls| EndAnalysis[结束分析]
    ToolCount -->|未超过| ContinueAnalysis[继续分析]
    
    CheckReport -->|报告完整| EndAnalysis
    CheckReport -->|报告不完整| ContinueAnalysis
```

## 🎯 实际调用序列示例

### 4级深度分析完整流程

```mermaid
sequenceDiagram
    participant User
    participant System
    participant AI
    
    User->>System: 请求4级深度分析
    System->>AI: 1. 市场分析师调用（可能多次）
    System->>AI: 2. 基本面分析师调用
    System->>AI: 3. 新闻分析师调用（可能多次）
    System->>AI: 4. 社交媒体分析师调用（可能多次）
    
    Note over System,AI: 研究辩论阶段开始
    
    System->>AI: 5. 看涨研究员第1轮
    AI->>System: 看涨分析报告
    System->>AI: 6. 看跌研究员第1轮
    AI->>System: 看跌分析报告
    System->>AI: 7. 研究经理第1轮裁决
    AI->>System: 第1轮投资决策
    
    System->>AI: 8. 看涨研究员第2轮
    System->>AI: 9. 看跌研究员第2轮
    System->>AI: 10. 研究经理第2轮裁决
    
    Note over System,AI: 风险管理阶段开始
    
    System->>AI: 11. 激进分析师第1轮
    System->>AI: 12. 保守分析师第1轮
    System->>AI: 13. 中性分析师第1轮
    System->>AI: 14. 激进分析师第2轮
    System->>AI: 15. 保守分析师第2轮
    System->>AI: 16. 中性分析师第2轮
    System->>AI: 17. 激进分析师第3轮
    System->>AI: 18. 保守分析师第3轮
    System->>AI: 19. 中性分析师第3轮
    System->>AI: 20. 投资组合经理最终决策
    
    System->>User: 返回完整分析报告
```

## 🔍 关键控制参数

### 核心配置项

```python
# 工具调用次数限制
MAX_TOOL_CALLS = {
    "market": 3,        # 市场分析师
    "news": 3,          # 新闻分析师
    "social": 3,        # 社交媒体分析师
    "fundamentals": 1   # 基本面分析师
}

# 辩论轮次配置
MAX_DEBATE_ROUNDS = 2           # 投资辩论轮次
MAX_RISK_DISCUSS_ROUNDS = 3     # 风险讨论轮次

# 研究深度映射
RESEARCH_DEPTH_LEVELS = {
    1: {"debate": 0, "risk": 0},    # 最快
    2: {"debate": 1, "risk": 0},    # 快速  
    3: {"debate": 2, "risk": 3},    # 标准
    4: {"debate": 2, "risk": 3},    # 深度
    5: {"debate": 3, "risk": 3}     # 最深度
}
```

## 💡 总结

TradingAgents的AI多次调用机制体现了以下特点：

1. **层次化调用**：从基础分析到深度辩论的渐进式调用
2. **并行处理**：多个分析师可以同时进行分析
3. **循环控制**：严格的次数限制防止死循环
4. **质量保障**：多轮辩论确保分析结果的全面性
5. **灵活配置**：可根据需求调整研究深度和调用次数

这种设计既保证了分析质量，又通过合理的调用控制确保了系统性能和成本效益。