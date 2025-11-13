# TradingAgents-CN

多智能体AI股票交易分析系统

## 项目简介

TradingAgents-CN 是一个基于多智能体AI的股票交易分析系统，集成了技术分析、基本面分析、情绪分析和风险管理等功能。

## 核心功能

### 智能体系统
- **技术分析师**: 基于技术指标进行股票分析
- **基本面分析师**: 基于财务数据进行分析
- **情绪分析师**: 分析市场情绪
- **新闻分析师**: 分析新闻影响
- **多头研究员**: 寻找看涨机会
- **空头研究员**: 寻找看跌机会
- **风险管理者**: 风险评估和控制
- **投资组合经理**: 组合管理和优化

### 数据服务
- **股票数据服务**: 实时和历史股票数据
- **市场数据提供者**: 多源市场数据集成
- **金融数据工具**: 金融计算和分析工具

### API接口
- **股票数据API**: 股票信息查询
- **智能体API**: 智能体管理和调用
- **分析API**: 综合分析结果

## 技术架构

### 后端技术栈
- **框架**: Spring Boot 2.7.18
- **AI框架**: Spring AI 1.1.0-M3
- **AI模型**: Anthropic Claude
- **缓存**: Redis + Spring Cache
- **构建工具**: Maven

### 配置管理
- **异步配置**: 多线程池管理
- **缓存配置**: 多级缓存策略
- **安全配置**: API安全控制
- **服务配置**: 数据服务配置
- **智能体配置**: AI智能体配置
- **HTTP客户端配置**: 多场景HTTP客户端

## 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- Redis 6.0+

### 安装步骤

1. 克隆项目
```bash
git clone https://github.com/your-repo/TradingAgents-CN.git
cd TradingAgents-CN
```

2. 配置环境变量
```bash
export ANTHROPIC_API_KEY=your_anthropic_api_key
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

3. 构建项目
```bash
mvn clean install
```

4. 运行应用
```bash
mvn spring-boot:run
```

### API文档

启动应用后，访问 Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

## 配置说明

### 应用配置
应用配置位于 `application.yml`，包含：
- 智能体配置（模型、提示词等）
- 数据源配置（API密钥、URL等）
- 缓存配置（TTL、最大大小等）
- 分析参数配置（辩论轮数、风险讨论轮数等）

### 缓存策略
- **股票数据缓存**: 按市场和类型分别缓存
- **智能体缓存**: 智能体状态和配置缓存
- **分析结果缓存**: 分析结果缓存

## 测试

### 运行测试
```bash
mvn test
```

### 测试覆盖
- 数据模型测试
- 服务层测试
- API控制器测试
- 配置测试
- 主应用测试

## 开发指南

### 添加新的智能体
1. 创建智能体类实现 `Agent` 接口
2. 在 `AgentConfig` 中配置Bean
3. 在 `TradingAgentsConfig` 中添加配置

### 添加新的数据源
1. 创建数据提供者实现相应接口
2. 在 `ServiceConfig` 中配置Bean
3. 更新配置文件

### 添加新的API端点
1. 在控制器中添加方法
2. 使用 Swagger 注解文档化
3. 添加对应的测试用例

## 性能优化

### 缓存优化
- 合理设置缓存TTL
- 使用多级缓存策略
- 监控缓存命中率

### 异步处理
- 使用线程池处理耗时操作
- 合理配置线程池参数
- 监控线程池状态

### 数据库优化
- 使用连接池
- 优化查询语句
- 添加适当索引

## 监控和日志

### 健康检查
访问健康检查端点：
```
http://localhost:8080/actuator/health
```

### 指标监控
访问指标端点：
```
http://localhost:8080/actuator/metrics
```

### 日志配置
日志配置位于 `logback-spring.xml`，支持：
- 控制台输出
- 文件输出
- 日志级别控制
- 日志轮转

## 贡献指南

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

- 项目维护者: [Your Name]
- 邮箱: [your.email@example.com]
- 项目主页: [https://github.com/your-repo/TradingAgents-CN](https://github.com/your-repo/TradingAgents-CN)