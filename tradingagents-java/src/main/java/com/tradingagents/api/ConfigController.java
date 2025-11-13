package com.tradingagents.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 配置管理API控制器
 * 提供系统配置管理的REST API接口
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {
    
    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);
    
    /**
     * 获取系统配置
     */
    @GetMapping
    public ResponseEntity<?> getSystemConfig() {
        try {
            log.info("获取系统配置");
            
            Map<String, Object> config = new HashMap<>();
            config.put("app_name", "TradingAgents-CN");
            config.put("version", "1.0.0");
            config.put("environment", "production");
            config.put("debug_mode", false);
            config.put("timestamp", LocalDateTime.now());
            
            // API配置
            Map<String, Object> api = new HashMap<>();
            api.put("base_url", "http://localhost:8080");
            api.put("timeout_seconds", 30);
            api.put("rate_limit_per_minute", 100);
            api.put("cors_enabled", true);
            config.put("api", api);
            
            // 智能体配置
            Map<String, Object> agents = new HashMap<>();
            agents.put("max_concurrent_analyses", 10);
            agents.put("debate_rounds", 3);
            agents.put("max_tokens", 4096);
            agents.put("temperature", 0.7);
            agents.put("timeout_seconds", 60);
            config.put("agents", agents);
            
            // 数据配置
            Map<String, Object> data = new HashMap<>();
            data.put("cache_ttl_minutes", 15);
            data.put("max_cache_size", 1000);
            data.put("auto_refresh_enabled", true);
            data.put("refresh_interval_minutes", 5);
            config.put("data", data);
            
            // 工作流配置
            Map<String, Object> workflow = new HashMap<>();
            workflow.put("max_steps", 20);
            workflow.put("timeout_minutes", 30);
            workflow.put("retry_attempts", 3);
            workflow.put("retry_delay_seconds", 5);
            config.put("workflow", workflow);
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取系统配置失败", e.getMessage()));
        }
    }
    
    /**
     * 获取API配置
     */
    @GetMapping("/api")
    public ResponseEntity<?> getApiConfig() {
        try {
            log.info("获取API配置");
            
            Map<String, Object> api = new HashMap<>();
            api.put("base_url", "http://localhost:8080");
            api.put("timeout_seconds", 30);
            api.put("rate_limit_per_minute", 100);
            api.put("cors_enabled", true);
            api.put("documentation_url", "http://localhost:8080/swagger-ui.html");
            api.put("version", "v1");
            api.put("endpoints", getApiEndpoints());
            api.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(api);
            
        } catch (Exception e) {
            log.error("获取API配置失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取API配置失败", e.getMessage()));
        }
    }
    
    /**
     * 获取智能体配置
     */
    @GetMapping("/agents")
    public ResponseEntity<?> getAgentConfig() {
        try {
            log.info("获取智能体配置");
            
            Map<String, Object> agents = new HashMap<>();
            agents.put("max_concurrent_analyses", 10);
            agents.put("debate_rounds", 3);
            agents.put("max_tokens", 4096);
            agents.put("temperature", 0.7);
            agents.put("timeout_seconds", 60);
            agents.put("retry_attempts", 2);
            agents.put("retry_delay_seconds", 3);
            agents.put("model_types", Arrays.asList("gpt-4", "gpt-3.5-turbo", "claude", "ernie"));
            agents.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(agents);
            
        } catch (Exception e) {
            log.error("获取智能体配置失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取智能体配置失败", e.getMessage()));
        }
    }
    
    /**
     * 获取数据配置
     */
    @GetMapping("/data")
    public ResponseEntity<?> getDataConfig() {
        try {
            log.info("获取数据配置");
            
            Map<String, Object> data = new HashMap<>();
            data.put("cache_ttl_minutes", 15);
            data.put("max_cache_size", 1000);
            data.put("auto_refresh_enabled", true);
            data.put("refresh_interval_minutes", 5);
            data.put("data_sources", Arrays.asList("Yahoo Finance", "Alpha Vantage", "新浪财经", "腾讯财经"));
            data.put("supported_exchanges", Arrays.asList("NYSE", "NASDAQ", "HKEX", "SSE", "SZSE"));
            data.put("market_hours", getMarketHours());
            data.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(data);
            
        } catch (Exception e) {
            log.error("获取数据配置失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取数据配置失败", e.getMessage()));
        }
    }
    
    /**
     * 获取工作流配置
     */
    @GetMapping("/workflow")
    public ResponseEntity<?> getWorkflowConfig() {
        try {
            log.info("获取工作流配置");
            
            Map<String, Object> workflow = new HashMap<>();
            workflow.put("max_steps", 20);
            workflow.put("timeout_minutes", 30);
            workflow.put("retry_attempts", 3);
            workflow.put("retry_delay_seconds", 5);
            workflow.put("parallel_execution", true);
            workflow.put("step_timeout_seconds", 300);
            workflow.put("workflow_types", Arrays.asList("investment_analysis", "risk_assessment", "portfolio_optimization"));
            workflow.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(workflow);
            
        } catch (Exception e) {
            log.error("获取工作流配置失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取工作流配置失败", e.getMessage()));
        }
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/{configType}")
    public ResponseEntity<?> updateConfig(@PathVariable String configType, @RequestBody Map<String, Object> newConfig) {
        try {
            log.info("更新配置: {}", configType);
            
            // 这里应该实现实际的配置更新逻辑
            // 目前只是返回模拟的更新结果
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "配置已更新");
            response.put("config_type", configType);
            response.put("updated_fields", newConfig.keySet());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("更新配置失败: {}", configType, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("更新配置失败", e.getMessage()));
        }
    }
    
    /**
     * 获取API端点列表
     */
    private List<Map<String, Object>> getApiEndpoints() {
        List<Map<String, Object>> endpoints = new ArrayList<>();
        
        // 股票数据端点
        endpoints.add(createEndpoint("GET", "/api/stocks/{symbol}", "获取股票数据"));
        endpoints.add(createEndpoint("GET", "/api/stocks/{symbol}/realtime", "获取实时股票数据"));
        endpoints.add(createEndpoint("GET", "/api/stocks/{symbol}/history", "获取股票历史数据"));
        endpoints.add(createEndpoint("GET", "/api/stocks/{symbol}/technical", "获取技术指标"));
        endpoints.add(createEndpoint("GET", "/api/stocks/{symbol}/fundamental", "获取基本面数据"));
        endpoints.add(createEndpoint("GET", "/api/stocks/{symbol}/risk", "获取风险指标"));
        endpoints.add(createEndpoint("POST", "/api/stocks/batch", "批量获取股票数据"));
        endpoints.add(createEndpoint("GET", "/api/stocks/search", "搜索股票"));
        endpoints.add(createEndpoint("GET", "/api/stocks/market-overview", "获取市场概览"));
        endpoints.add(createEndpoint("POST", "/api/stocks/portfolio", "投资组合分析"));
        endpoints.add(createEndpoint("POST", "/api/stocks/compare", "股票比较"));
        endpoints.add(createEndpoint("GET", "/api/stocks/correlation", "相关性分析"));
        endpoints.add(createEndpoint("GET", "/api/stocks/cache/stats", "缓存统计"));
        endpoints.add(createEndpoint("DELETE", "/api/stocks/cache", "清除缓存"));
        
        // 投资分析端点
        endpoints.add(createEndpoint("POST", "/api/investment/analyze", "开始投资分析"));
        endpoints.add(createEndpoint("GET", "/api/investment/status/{workflowId}", "获取分析状态"));
        endpoints.add(createEndpoint("GET", "/api/investment/result/{workflowId}", "获取分析结果"));
        endpoints.add(createEndpoint("POST", "/api/investment/discuss", "开始智能体讨论"));
        endpoints.add(createEndpoint("POST", "/api/investment/debate", "投资辩论"));
        endpoints.add(createEndpoint("POST", "/api/investment/risk-assessment", "风险评估讨论"));
        endpoints.add(createEndpoint("GET", "/api/investment/coordinator/status", "获取协调器状态"));
        endpoints.add(createEndpoint("GET", "/api/investment/types", "获取可用分析类型"));
        
        // 智能体管理端点
        endpoints.add(createEndpoint("GET", "/api/agents/status", "获取所有智能体状态"));
        endpoints.add(createEndpoint("GET", "/api/agents/{agentId}/status", "获取特定智能体状态"));
        endpoints.add(createEndpoint("GET", "/api/agents/{agentId}/config", "获取智能体配置"));
        endpoints.add(createEndpoint("GET", "/api/agents/{agentId}/messages", "获取智能体消息历史"));
        endpoints.add(createEndpoint("DELETE", "/api/agents/{agentId}/messages", "清除智能体消息历史"));
        endpoints.add(createEndpoint("POST", "/api/agents/{agentId}/test", "测试智能体"));
        endpoints.add(createEndpoint("GET", "/api/agents/statistics", "获取智能体统计信息"));
        
        // 健康检查端点
        endpoints.add(createEndpoint("GET", "/api/health", "健康检查"));
        endpoints.add(createEndpoint("GET", "/api/health/detailed", "详细健康检查"));
        endpoints.add(createEndpoint("GET", "/api/health/status", "服务状态"));
        
        // 配置管理端点
        endpoints.add(createEndpoint("GET", "/api/config", "获取系统配置"));
        endpoints.add(createEndpoint("GET", "/api/config/api", "获取API配置"));
        endpoints.add(createEndpoint("GET", "/api/config/agents", "获取智能体配置"));
        endpoints.add(createEndpoint("GET", "/api/config/data", "获取数据配置"));
        endpoints.add(createEndpoint("GET", "/api/config/workflow", "获取工作流配置"));
        endpoints.add(createEndpoint("PUT", "/api/config/{configType}", "更新配置"));
        
        return endpoints;
    }
    
    /**
     * 创建端点描述
     */
    private Map<String, Object> createEndpoint(String method, String path, String description) {
        Map<String, Object> endpoint = new HashMap<>();
        endpoint.put("method", method);
        endpoint.put("path", path);
        endpoint.put("description", description);
        return endpoint;
    }
    
    /**
     * 获取市场交易时间
     */
    private Map<String, Object> getMarketHours() {
        Map<String, Object> marketHours = new HashMap<>();
        marketHours.put("NYSE", Map.of("open", "09:30", "close", "16:00", "timezone", "EST"));
        marketHours.put("NASDAQ", Map.of("open", "09:30", "close", "16:00", "timezone", "EST"));
        marketHours.put("HKEX", Map.of("open", "09:30", "close", "16:00", "timezone", "HKT"));
        marketHours.put("SSE", Map.of("open", "09:30", "close", "15:00", "timezone", "CST"));
        marketHours.put("SZSE", Map.of("open", "09:30", "close", "15:00", "timezone", "CST"));
        return marketHours;
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
}