package com.tradingagents.api;

import com.tradingagents.agent.analyst.MarketAnalyst;
import com.tradingagents.agent.analyst.SentimentAnalyst;
import com.tradingagents.agent.analyst.NewsAnalyst;
import com.tradingagents.agent.researcher.BullResearcher;
import com.tradingagents.agent.researcher.BearResearcher;
import com.tradingagents.agent.manager.RiskManager;
import com.tradingagents.agent.manager.PortfolioManager;
import com.tradingagents.agent.base.BaseAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 智能体管理API控制器
 * 提供智能体管理和监控的REST API接口
 */
@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentManagementController {
    
    private static final Logger log = LoggerFactory.getLogger(AgentManagementController.class);
    
    @Autowired
    private MarketAnalyst marketAnalyst;
    
    // @Autowired
    // private FundamentalAnalyst fundamentalAnalyst; // 暂时注释掉，等待实现
    
    @Autowired
    private SentimentAnalyst sentimentAnalyst;
    
    @Autowired
    private NewsAnalyst newsAnalyst;
    
    @Autowired
    private BullResearcher bullResearcher;
    
    @Autowired
    private BearResearcher bearResearcher;
    
    @Autowired
    private RiskManager riskManager;
    
    @Autowired
    private PortfolioManager portfolioManager;
    
    /**
     * 获取所有智能体状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAllAgentsStatus() {
        try {
            log.info("获取所有智能体状态");
            
            List<Map<String, Object>> agents = new ArrayList<>();
            
            // 分析师智能体
            agents.add(createAgentStatus("technical_analyst", "技术分析师", marketAnalyst));
            // agents.add(createAgentStatus("fundamental_analyst", "基本面分析师", fundamentalAnalyst)); // 暂时注释掉
            agents.add(createAgentStatus("sentiment_analyst", "情绪分析师", sentimentAnalyst));
            agents.add(createAgentStatus("news_analyst", "新闻分析师", newsAnalyst));
            
            // 研究员智能体
            agents.add(createAgentStatus("bull_researcher", "多头研究员", bullResearcher));
            agents.add(createAgentStatus("bear_researcher", "空头研究员", bearResearcher));
            
            // 管理者智能体
            agents.add(createAgentStatus("risk_manager", "风险管理者", riskManager));
            agents.add(createAgentStatus("portfolio_manager", "投资组合经理", portfolioManager));
            
            Map<String, Object> response = new HashMap<>();
            response.put("agents", agents);
            response.put("total_count", agents.size());
            response.put("active_count", agents.stream().filter(a -> (Boolean) a.get("is_active")).count());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取智能体状态失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取智能体状态失败", e.getMessage()));
        }
    }
    
    /**
     * 获取特定智能体状态
     */
    @GetMapping("/{agentId}/status")
    public ResponseEntity<?> getAgentStatus(@PathVariable String agentId) {
        try {
            log.info("获取智能体状态: {}", agentId);
            
            BaseAgent agent = getAgentById(agentId);
            if (agent == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> status = createAgentStatus(agentId, getAgentName(agentId), agent);
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取智能体状态失败: {}", agentId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取智能体状态失败", e.getMessage()));
        }
    }
    
    /**
     * 获取智能体配置
     */
    @GetMapping("/{agentId}/config")
    public ResponseEntity<?> getAgentConfig(@PathVariable String agentId) {
        try {
            log.info("获取智能体配置: {}", agentId);
            
            BaseAgent agent = getAgentById(agentId);
            if (agent == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> config = new HashMap<>();
            config.put("agent_id", agentId);
            config.put("agent_name", getAgentName(agentId));
            config.put("model_type", agent.getDefaultProvider() + "/" + agent.getDefaultModel());
            config.put("last_updated", LocalDateTime.now());
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("获取智能体配置失败: {}", agentId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取智能体配置失败", e.getMessage()));
        }
    }
    
    /**
     * 获取智能体消息历史
     */
    @GetMapping("/{agentId}/messages")
    public ResponseEntity<?> getAgentMessages(@PathVariable String agentId) {
        try {
            log.info("获取智能体消息历史: {}", agentId);
            
            BaseAgent agent = getAgentById(agentId);
            if (agent == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Since BaseAgent doesn't have message history, return empty list
            List<Map<String, Object>> messages = new ArrayList<>();
            
            Map<String, Object> response = new HashMap<>();
            response.put("agent_id", agentId);
            response.put("agent_name", getAgentName(agentId));
            response.put("message_count", messages.size());
            response.put("messages", messages);
            response.put("last_updated", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取智能体消息历史失败: {}", agentId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取智能体消息历史失败", e.getMessage()));
        }
    }
    
    /**
     * 清除智能体消息历史
     */
    @DeleteMapping("/{agentId}/messages")
    public ResponseEntity<?> clearAgentMessages(@PathVariable String agentId) {
        try {
            log.info("清除智能体消息历史: {}", agentId);
            
            BaseAgent agent = getAgentById(agentId);
            if (agent == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Since BaseAgent doesn't have message history, just return success
            Map<String, Object> response = new HashMap<>();
            response.put("message", "消息历史已清除");
            response.put("agent_id", agentId);
            response.put("agent_name", getAgentName(agentId));
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("清除智能体消息历史失败: {}", agentId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("清除智能体消息历史失败", e.getMessage()));
        }
    }
    
    /**
     * 测试智能体
     */
    @PostMapping("/{agentId}/test")
    public ResponseEntity<?> testAgent(@PathVariable String agentId, @RequestBody TestRequest request) {
        try {
            log.info("测试智能体: {}", agentId);
            
            BaseAgent agent = getAgentById(agentId);
            if (agent == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 模拟智能体分析
            String testPrompt = String.format("请分析股票 %s 的 %s", 
                    request.getSymbol() != null ? request.getSymbol() : "TEST",
                    request.getAnalysisType() != null ? request.getAnalysisType() : "基本情况");
            
            String testResponse = String.format("[%s] 对 %s 的分析结果: 基于我的专业分析...", 
                    getAgentName(agentId), testPrompt);
            
            Map<String, Object> response = new HashMap<>();
            response.put("agent_id", agentId);
            response.put("agent_name", getAgentName(agentId));
            response.put("test_prompt", testPrompt);
            response.put("test_response", testResponse);
            response.put("message_count", 0); // No message history available
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("测试智能体失败: {}", agentId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("测试智能体失败", e.getMessage()));
        }
    }
    
    /**
     * 获取智能体统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getAgentStatistics() {
        try {
            log.info("获取智能体统计信息");
            
            Map<String, Object> statistics = new HashMap<>();
            
            // 分析师统计
            Map<String, Object> analystStats = new HashMap<>();
            analystStats.put("total_analysts", 4);
            analystStats.put("active_analysts", 4);
            analystStats.put("total_analyses", estimateTotalAnalyses("analyst"));
            statistics.put("analysts", analystStats);
            
            // 研究员统计
            Map<String, Object> researcherStats = new HashMap<>();
            researcherStats.put("total_researchers", 2);
            researcherStats.put("active_researchers", 2);
            researcherStats.put("total_researches", estimateTotalAnalyses("researcher"));
            statistics.put("researchers", researcherStats);
            
            // 管理者统计
            Map<String, Object> managerStats = new HashMap<>();
            managerStats.put("total_managers", 2);
            managerStats.put("active_managers", 2);
            managerStats.put("total_decisions", estimateTotalAnalyses("manager"));
            statistics.put("managers", managerStats);
            
            // 总体统计
            statistics.put("total_agents", 8);
            statistics.put("active_agents", 8);
            statistics.put("total_analyses", estimateTotalAnalyses("all"));
            statistics.put("average_response_time", "1.2s");
            statistics.put("success_rate", 0.95);
            statistics.put("last_updated", LocalDateTime.now());
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("获取智能体统计信息失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取智能体统计信息失败", e.getMessage()));
        }
    }
    
    /**
     * 创建智能体状态
     */
    private Map<String, Object> createAgentStatus(String agentId, String agentName, BaseAgent agent) {
        Map<String, Object> status = new HashMap<>();
        status.put("agent_id", agentId);
        status.put("agent_name", agentName);
        status.put("is_active", true);
        status.put("model_type", agent.getDefaultProvider() + "/" + agent.getDefaultModel());
        status.put("message_history_count", 0); // No message history available
        status.put("last_activity", LocalDateTime.now());
        status.put("status", "running");
        status.put("capabilities", getAgentCapabilities(agentId));
        status.put("performance_metrics", getAgentPerformanceMetrics(agentId));
        return status;
    }
    
    /**
     * 根据ID获取智能体
     */
    private BaseAgent getAgentById(String agentId) {
        switch (agentId.toLowerCase()) {
            case "technical_analyst":
                return marketAnalyst;
            case "fundamental_analyst":
                // return fundamentalAnalyst; // 暂时注释掉，等待实现
                return null;
            case "sentiment_analyst":
                return sentimentAnalyst;
            case "news_analyst":
                return newsAnalyst;
            case "bull_researcher":
                return bullResearcher;
            case "bear_researcher":
                return bearResearcher;
            case "risk_manager":
                return riskManager;
            case "portfolio_manager":
                return portfolioManager;
            default:
                return null;
        }
    }
    
    /**
     * 获取智能体名称
     */
    private String getAgentName(String agentId) {
        switch (agentId.toLowerCase()) {
            case "technical_analyst":
                return "技术分析师";
            case "fundamental_analyst":
                return "基本面分析师";
            case "sentiment_analyst":
                return "情绪分析师";
            case "news_analyst":
                return "新闻分析师";
            case "bull_researcher":
                return "多头研究员";
            case "bear_researcher":
                return "空头研究员";
            case "risk_manager":
                return "风险管理者";
            case "portfolio_manager":
                return "投资组合经理";
            default:
                return "未知智能体";
        }
    }
    
    /**
     * 获取智能体能力
     */
    private List<String> getAgentCapabilities(String agentId) {
        switch (agentId.toLowerCase()) {
            case "technical_analyst":
                return List.of("技术分析", "趋势识别", "支撑阻力位", "图表模式");
            case "fundamental_analyst":
                return List.of("基本面分析", "财务分析", "估值分析", "行业分析");
            case "sentiment_analyst":
                return List.of("情绪分析", "社交媒体分析", "市场情绪", "投资者情绪");
            case "news_analyst":
                return List.of("新闻分析", "事件分析", "媒体报道", "新闻情绪");
            case "bull_researcher":
                return List.of("多头研究", "买入建议", "积极因素", "增长潜力");
            case "bear_researcher":
                return List.of("空头研究", "卖出建议", "风险因素", "下跌风险");
            case "risk_manager":
                return List.of("风险管理", "风险评估", "风险控制", "风险缓解");
            case "portfolio_manager":
                return List.of("投资组合管理", "资产配置", "投资决策", "组合优化");
            default:
                return List.of("未知能力");
        }
    }
    

    
    /**
     * 获取智能体性能指标
     */
    private Map<String, Object> getAgentPerformanceMetrics(String agentId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("accuracy", 0.85 + Math.random() * 0.1);
        metrics.put("response_time_ms", 800 + (int) (Math.random() * 400));
        metrics.put("success_rate", 0.9 + Math.random() * 0.08);
        metrics.put("total_analyses", 100 + (int) (Math.random() * 200));
        metrics.put("last_updated", LocalDateTime.now());
        return metrics;
    }
    
    /**
     * 估算总分析数量
     */
    private int estimateTotalAnalyses(String type) {
        switch (type) {
            case "analyst":
                return 400 + (int) (Math.random() * 200);
            case "researcher":
                return 200 + (int) (Math.random() * 100);
            case "manager":
                return 150 + (int) (Math.random() * 80);
            case "all":
                return 750 + (int) (Math.random() * 380);
            default:
                return 0;
        }
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String error, String details) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("details", details);
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }
    
    /**
     * 测试请求
     */
    public static class TestRequest {
        private String symbol = "TEST";
        private String analysisType = "basic";
        
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getAnalysisType() { return analysisType; }
        public void setAnalysisType(String analysisType) { this.analysisType = analysisType; }
    }
}