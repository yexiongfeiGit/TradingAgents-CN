# AI调用流程图 - 输入输出详解

## 🎯 概述

本文档详细阐述了TradingAgents-CN项目中AI接口调用的完整流程，包括输入参数、处理逻辑和输出结果。

## 📊 系统架构图

```mermaid
graph TB
    subgraph "输入层"
        A1["用户输入"]
        A2["股票代码"]
        A3["分析日期"]
        A4["分析类型"]
    end

    subgraph "AI调用核心流程"
        B1["LLM适配器层"]
        B2["提示词构建"]
        B3["工具调用管理"]
        B4["AI接口调用"]
        B5["响应处理"]
    end

    subgraph "输出层"
        C1["分析报告"]
        C2["投资建议"]
        C3["Token使用统计"]
        C4["成本计算"]
    end

    A1 --> B1
    A2 --> B2
    A3 --> B2
    A4 --> B2
    B1 --> B3
    B2 --> B3
    B3 --> B4
    B4 --> B5
    B5 --> C1
    B5 --> C2
    B5 --> C3
    B5 --> C4
```

## 🔧 AI调用详细流程

### 1. 输入参数定义

```mermaid
graph LR
    subgraph "必需输入"
        I1["messages: List[BaseMessage]"]
        I2["model: str"]
        I3["api_key: str"]
    end

    subgraph "可选输入"
        O1["temperature: float"]
        O2["max_tokens: int"]
        O3["base_url: str"]
        O4["timeout: int"]
        O5["stop: List[str]"]
    end

    subgraph "工具相关"
        T1["tools: List[Tool]"]
        T2["tool_choice: str"]
    end
```

### 2. LLM适配器输入输出

```mermaid
sequenceDiagram
    participant Client as "客户端"
    participant Adapter as "LLM适配器"
    participant Provider as "AI服务商"
    participant Logger as "日志系统"

    Client->>Adapter: create_llm_by_provider()
    Note over Adapter: 输入参数:
    Note over Adapter: - provider: str
    Note over Adapter: - model: str
    Note over Adapter: - api_key: str
    Note over Adapter: - temperature: float
    Note over Adapter: - max_tokens: int

    Adapter->>Adapter: API密钥验证
    Adapter->>Provider: 初始化LLM实例
    Provider-->>Adapter: LLM实例
    Adapter-->>Client: 返回配置好的LLM

    Client->>Adapter: llm.invoke(messages)
    Note over Adapter: 输入消息列表
    Note over Adapter: 包含系统提示、用户输入

    Adapter->>Provider: API调用
    Provider-->>Adapter: AI响应结果
    
    Adapter->>Logger: 记录Token使用
    Note over Logger: 输出统计:
    Note over Logger: - total_tokens
    Note over Logger: - prompt_tokens  
    Note over Logger: - completion_tokens
    Note over Logger: - 用时统计
    
    Adapter-->>Client: 返回处理后的响应
```

### 3. 单个分析师AI调用流程

```mermaid
graph TD
    Start(["开始分析"]) --> InputCheck{"检查输入"}
    InputCheck -->|"有效输入"| StatePrep["准备状态对象"]
    StatePrep --> ToolCheck{"工具调用计数检查"}
    
    ToolCheck -->|"< 3次"| PromptBuild["构建提示词"]
    PromptBuild --> ToolBind["绑定工具"]
    ToolBind --> LLMCall["调用LLM"]
    
    LLMCall -->|"需要工具"| ToolExec["执行工具"]
    ToolExec --> ToolCheck
    
    LLMCall -->|"生成报告"| ReportGen["生成分析报告"]
    ReportGen --> TokenTrack["记录Token使用"]
    TokenTrack --> End(["结束分析"])
    
    ToolCheck -->|"≥ 3次"| ErrorHandle["错误处理"]
    ErrorHandle --> End
    
    InputCheck -->|"无效输入"| ErrorHandle
```

### 4. 输入输出详细说明

#### 4.1 输入参数详解

| 参数类别 | 参数名称 | 类型 | 必需 | 说明 |
|---------|---------|------|------|------|
| **基础参数** | messages | List[BaseMessage] | ✅ | 消息列表，包含系统提示和用户输入 |
| | model | string | ✅ | 模型名称，如"qwen-turbo" |
| | api_key | string | ✅ | API密钥，支持环境变量读取 |
| **生成参数** | temperature | float | ❌ | 温度参数，控制随机性 (0.0-1.0) |
| | max_tokens | integer | ❌ | 最大生成token数 |
| | timeout | integer | ❌ | 超时时间（秒） |
| **连接参数** | base_url | string | ❌ | 自定义API端点 |
| | stop | List[string] | ❌ | 停止词列表 |
| **工具参数** | tools | List[Tool] | ❌ | 可用工具列表 |
| | tool_choice | string | ❌ | 工具选择策略 |

#### 4.2 输出结果详解

```mermaid
graph TD
    subgraph "主要输出"
        M1["ChatResult对象"]
        M2["content: str"]
        M3["usage_metadata: dict"]
    end

    subgraph "使用统计"
        S1["total_tokens: int"]
        S2["prompt_tokens: int"]
        S3["completion_tokens: int"]
        S4["elapsed_time: float"]
    end

    subgraph "成本计算"
        C1["input_cost: float"]
        C2["output_cost: float"]
        C3["total_cost: float"]
    end

    subgraph "日志输出"
        L1["Provider信息"]
        L2["Model信息"]
        L3["Token统计"]
        L4["用时统计"]
    end

    M1 --> M2
    M1 --> M3
    M3 --> S1
    M3 --> S2
    M3 --> S3
    S1 --> C1
    S2 --> C2
    S3 --> C3
    M1 --> L1
    M1 --> L2
    M1 --> L3
    M1 --> L4
```

### 5. 具体代码示例

#### 5.1 基础AI调用

```python
# 输入参数
messages = [
    SystemMessage(content="你是一个专业的股票分析师"),
    HumanMessage(content="请分析AAPL股票的技术指标")
]

# 调用参数
model = "qwen-turbo"
api_key = "your-api-key"
temperature = 0.7
max_tokens = 2000

# 执行调用
llm = create_llm_by_provider(
    provider="dashscope",
    model=model,
    api_key=api_key,
    temperature=temperature,
    max_tokens=max_tokens
)

# 输出结果
result = llm.invoke(messages)
print(f"分析结果: {result.content}")
print(f"Token使用: {result.usage_metadata}")
```

#### 5.2 带工具调用的AI调用

```python
# 输入参数（包含工具）
messages = [
    SystemMessage(content="使用工具获取股票数据"),
    HumanMessage(content="获取TSLA的市场数据")
]

# 工具定义
tools = [get_stock_market_data_unified]

# 绑定工具并调用
llm_with_tools = llm.bind_tools(tools)
result = llm_with_tools.invoke(messages)

# 输出包含工具调用
if result.tool_calls:
    print(f"工具调用: {result.tool_calls}")
print(f"最终响应: {result.content}")
```

### 6. Token使用和成本跟踪

```mermaid
graph LR
    subgraph "输入跟踪"
        I1["计算输入token数"]
        I2["记录提示词token"]
        I3["估算输入成本"]
    end

    subgraph "输出跟踪"
        O1["提取输出token数"]
        O2["记录补全token"]
        O3["计算输出成本"]
    end

    subgraph "统计输出"
        S1["总token数"]
        S2["总成本"]
        S3["用时统计"]
        S4["效率指标"]
    end

    I1 --> I2
    I2 --> I3
    O1 --> O2
    O2 --> O3
    I3 --> S2
    O3 --> S2
    I1 --> S1
    O1 --> S1
    I1 --> S3
    O1 --> S4
```

### 7. 错误处理机制

#### 7.1 API密钥验证

```python
def validate_api_key(api_key: str) -> bool:
    """验证API密钥有效性"""
    if not api_key or len(api_key) <= 10:
        return False
    if api_key.startswith('your_') or api_key.startswith('your-'):
        return False
    if '...' in api_key:
        return False
    return True
```

#### 7.2 异常处理

```python
try:
    result = llm.invoke(messages)
except ValueError as e:
    logger.error(f"API密钥错误: {e}")
    raise ValueError("请检查API密钥配置")
except TimeoutError as e:
    logger.error(f"请求超时: {e}")
    raise TimeoutError("AI服务响应超时")
except Exception as e:
    logger.error(f"AI调用失败: {e}")
    raise RuntimeError(f"AI调用失败: {str(e)}")
```

## 📝 总结

AI调用流程的核心特点：

1. **统一接口**：通过适配器模式支持多种AI服务商
2. **完整跟踪**：详细记录Token使用和成本统计
3. **工具集成**：支持函数调用和工具使用
4. **错误处理**：完善的异常处理和验证机制
5. **性能优化**：防死循环机制和超时控制

该流程确保了AI调用的可靠性、可追踪性和成本可控性。