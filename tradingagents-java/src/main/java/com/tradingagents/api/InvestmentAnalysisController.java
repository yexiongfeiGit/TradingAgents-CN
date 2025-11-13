package com.tradingagents.api;

import com.tradingagents.debate.InvestmentDebate;
import com.tradingagents.debate.MultiRoundDialogue;
import com.tradingagents.workflow.InvestmentAnalysisWorkflow;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 投资分析控制器
 * 提供投资分析相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/investment-analysis")
public class InvestmentAnalysisController {


    @Autowired
    private InvestmentAnalysisWorkflow investmentAnalysisWorkflow;

    /**
     * 开始投资分析
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> startInvestmentAnalysis(@RequestBody InvestmentAnalysisRequest request) {
        try {
            log.info("开始投资分析: {} - {}", request.symbol, request.analysisType);
            
            // 验证输入
            if (request.symbol == null || request.symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("输入错误", "股票代码不能为空"));
            }
            
            // 初始化工作流
            investmentAnalysisWorkflow = new InvestmentAnalysisWorkflow(
                request.symbol.toUpperCase(), 
                request.analysisType
            );
            
            // 开始分析
            investmentAnalysisWorkflow.execute();
            
            // 获取分析结果
            String analysisResult = investmentAnalysisWorkflow.getAnalysisResult();
            
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", request.symbol.toUpperCase());
            response.put("analysis_type", request.analysisType);
            response.put("workflow_id", "investment_analysis_" + request.symbol.toUpperCase());
            response.put("status", investmentAnalysisWorkflow.getWorkflowStatus());
            response.put("result", analysisResult);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("投资分析失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("投资分析失败", e.getMessage()));
        }
    }

    /**
     * 获取分析状态
     */
    @GetMapping("/status/{workflowId}")
    public ResponseEntity<?> getAnalysisStatus(@PathVariable String workflowId) {
        try {
            log.info("获取分析状态: {}", workflowId);
            
            Map<String, Object> status = new HashMap<>();
            status.put("workflow_id", workflowId);
            status.put("workflow_name", "投资分析工作流");
            status.put("current_step_id", "final_decision");
            status.put("status", investmentAnalysisWorkflow != null ? investmentAnalysisWorkflow.getWorkflowStatus() : "unknown");
            status.put("is_completed", investmentAnalysisWorkflow != null ? "completed".equals(investmentAnalysisWorkflow.getWorkflowStatus()) : false);
            status.put("execution_time", 15000);
            status.put("step_statistics", new HashMap<>());
            status.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取分析状态失败: {}", workflowId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取分析状态失败", e.getMessage()));
        }
    }

    /**
     * 获取分析结果
     */
    @GetMapping("/result/{workflowId}")
    public ResponseEntity<?> getAnalysisResult(@PathVariable String workflowId) {
        try {
            log.info("获取分析结果: {}", workflowId);
            
            String result = investmentAnalysisWorkflow != null ? 
                investmentAnalysisWorkflow.getAnalysisResult() : "分析结果不可用";
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflow_id", workflowId);
            response.put("result", result);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取分析结果失败: {}", workflowId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取分析结果失败", e.getMessage()));
        }
    }

    /**
     * 开始智能体讨论
     */
    @PostMapping("/agent-discussion")
    public ResponseEntity<?> startAgentDiscussion(@RequestBody AgentDiscussionRequest request) {
        try {
            log.info("开始智能体讨论: {} - {}", request.symbol, request.topic);
            
            // 初始化多轮对话
            MultiRoundDialogue dialogue = new MultiRoundDialogue();
            dialogue.updateContext(request.topic);
            dialogue.setMaxRounds(request.maxRounds);
            
            // 添加初始消息
            dialogue.addDialogue("system", "开始讨论主题: " + request.topic + " 股票: " + request.symbol);
            
            // 开始讨论
            for (int i = 0; i < request.maxRounds; i++) {
                if (!dialogue.shouldContinueDialogue()) {
                    break;
                }
                
                // 模拟智能体参与讨论
                String[] agentRoles = {"技术分析师", "基本面分析师", "情绪分析师", "新闻分析师", "多头研究员", "空头研究员", "风险管理者", "投资组合经理"};
                String agentRole = agentRoles[i % agentRoles.length];
                
                String message = String.format("[%s] 对 %s 的看法: 基于我的分析...", agentRole, request.topic);
                dialogue.addDialogue(agentRole, message);
                
                dialogue.nextRound();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", request.symbol);
            response.put("topic", request.topic);
            response.put("max_rounds", request.maxRounds);
            response.put("actual_rounds", dialogue.getCurrentRound());
            response.put("dialogue_history", dialogue.getDialogueHistory());
            response.put("key_insights", dialogue.getKeyInsights() != null ? dialogue.getKeyInsights() : new ArrayList<>());
            response.put("action_items", dialogue.getActionItems() != null ? dialogue.getActionItems() : new ArrayList<>());
            response.put("summary", dialogue.getDialogueSummary());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("智能体讨论失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("智能体讨论失败", e.getMessage()));
        }
    }

    /**
     * 开始投资辩论
     */
    @PostMapping("/investment-debate")
    public ResponseEntity<?> startInvestmentDebate(@RequestBody InvestmentDebateRequest request) {
        try {
            log.info("开始投资辩论: {} - {}", request.symbol, request.topic);
            
            // 创建投资辩论
            InvestmentDebate debate = new InvestmentDebate();
            debate.setMaxDebates(request.maxRounds);
            
            // 模拟辩论过程
            String[] bullArguments = {
                "该公司财务状况良好，营收增长稳定",
                "行业前景乐观，市场份额持续扩大",
                "技术创新能力强，产品竞争力突出",
                "估值合理，具有投资价值"
            };
            
            String[] bearArguments = {
                "市场竞争激烈，利润率承压",
                "宏观经济环境不利，增长前景不明",
                "估值偏高，存在回调风险",
                "行业面临监管风险"
            };
            
            // 开始辩论
            for (int i = 0; i < request.maxRounds && debate.shouldContinueDebate(); i++) {
                if (i % 2 == 0) {
                    // 多头观点
                    String argument = bullArguments[i / 2];
                    debate.addBullArgument(argument);
                } else {
                    // 空头观点
                    String argument = bearArguments[i / 2];
                    debate.addBearArgument(argument);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", request.symbol.toUpperCase());
            response.put("topic", request.topic);
            response.put("max_rounds", request.maxRounds);
            response.put("actual_rounds", (debate.getBullSpeeches() + debate.getBearSpeeches()) / 2); // Approximate rounds
            response.put("bull_arguments", debate.getBullArguments());
            response.put("bear_arguments", debate.getBearArguments());
            
            // Create speaking counts manually
            Map<String, Integer> speakingCounts = new HashMap<>();
            speakingCounts.put("bull", debate.getBullSpeeches());
            speakingCounts.put("bear", debate.getBearSpeeches());
            response.put("speaking_counts", speakingCounts);
            
            response.put("summary", debate.getDebateSummary());
            response.put("winner", debate.getBullSpeeches() >= debate.getBearSpeeches() ? "bull" : "bear");
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("投资辩论失败", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("投资辩论失败", e.getMessage()));
        }
    }

    /**
     * 获取实时进展
     */
    @GetMapping("/progress/{workflowId}")
    public ResponseEntity<?> getProgressUpdate(@PathVariable String workflowId) {
        try {
            log.info("获取实时进展: {}", workflowId);
            
            String progress = investmentAnalysisWorkflow != null ? 
                investmentAnalysisWorkflow.getProgressUpdate() : "进展信息不可用";
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflow_id", workflowId);
            response.put("progress", progress);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取实时进展失败: {}", workflowId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取实时进展失败", e.getMessage()));
        }
    }

    /**
     * 获取智能体协调状态
     */
    @GetMapping("/coordination-status/{workflowId}")
    public ResponseEntity<?> getCoordinationStatus(@PathVariable String workflowId) {
        try {
            log.info("获取智能体协调状态: {}", workflowId);
            
            String coordinationStatus = investmentAnalysisWorkflow != null ? 
                investmentAnalysisWorkflow.getCoordinationStatus() : "协调状态不可用";
            
            Map<String, Object> response = new HashMap<>();
            response.put("workflow_id", workflowId);
            response.put("coordination_status", coordinationStatus);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取智能体协调状态失败: {}", workflowId, e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse("获取智能体协调状态失败", e.getMessage()));
        }
    }

    /**
     * 投资分析请求
     */
    public static class InvestmentAnalysisRequest {
        public String symbol;
        public String analysisType = "comprehensive";
    }

    /**
     * 智能体讨论请求
     */
    public static class AgentDiscussionRequest {
        public String symbol;
        public String topic;
        public int maxRounds = 8;
    }

    /**
     * 投资辩论请求
     */
    public static class InvestmentDebateRequest {
        public String symbol;
        public String topic;
        public int maxRounds = 8;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }
}