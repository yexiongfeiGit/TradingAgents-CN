package com.tradingagents.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

import java.util.Map;

/**
 * 分析请求模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    
    @NotBlank(message = "股票代码不能为空")
    @Pattern(regexp = "^[A-Z0-9]{1,10}(\\.[A-Z]{2})?$", message = "股票代码格式无效")
    private String stockCode;
    
    @NotBlank(message = "股票名称不能为空")
    private String stockName;
    
    @Min(value = 1, message = "研究深度必须在1-5之间")
    @Max(value = 5, message = "研究深度必须在1-5之间")
    private int researchDepth = 2;
    
    private String analysisType = "comprehensive";
    
    private boolean enableMemory = false;
    
    private boolean enableDebug = false;
    
    private String language = "zh";
    
    private String llmProvider = "openai";
    
    private String llmModel;
    
    private Map<String, Object> additionalParameters;
    
    /**
     * 获取完整股票代码（包含市场后缀）
     */
    public String getFullStockCode() {
        if (stockCode == null) {
            return null;
        }
        
        // 如果已经包含后缀，直接返回
        if (stockCode.contains(".")) {
            return stockCode;
        }
        
        // 根据股票代码长度判断市场
        if (stockCode.length() == 6) {
            // 中国A股
            if (stockCode.startsWith("6")) {
                return stockCode + ".SS";  // 上证
            } else {
                return stockCode + ".SZ";  // 深证
            }
        } else if (stockCode.length() <= 5) {
            // 美股
            return stockCode;
        }
        
        return stockCode;
    }
    
    /**
     * 获取市场类型
     */
    public String getMarketType() {
        String fullCode = getFullStockCode();
        if (fullCode == null) {
            return "unknown";
        }
        
        if (fullCode.contains(".SS") || fullCode.contains(".SZ")) {
            return "china";
        } else if (fullCode.contains(".HK")) {
            return "hk";
        } else {
            return "us";
        }
    }
    
    /**
     * 获取研究深度描述
     */
    public String getResearchDepthDescription() {
        switch (researchDepth) {
            case 1: return "快速分析";
            case 2: return "标准分析";
            case 3: return "深度分析";
            case 4: return "全面分析";
            case 5: return "专家分析";
            default: return "标准分析";
        }
    }
    
    /**
     * 获取股票代码
     */
    public String getStockCode() {
        return stockCode;
    }
    
    /**
     * 设置股票代码
     */
    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }
    
    /**
     * 获取股票名称
     */
    public String getStockName() {
        return stockName;
    }
    
    /**
     * 设置股票名称
     */
    public void setStockName(String stockName) {
        this.stockName = stockName;
    }
    
    /**
     * 获取研究深度
     */
    public int getResearchDepth() {
        return researchDepth;
    }
    
    /**
     * 设置研究深度
     */
    public void setResearchDepth(int researchDepth) {
        this.researchDepth = researchDepth;
    }
    
    /**
     * 获取分析类型
     */
    public String getAnalysisType() {
        return analysisType;
    }
    
    /**
     * 设置分析类型
     */
    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }
    
    /**
     * 是否启用内存
     */
    public boolean isEnableMemory() {
        return enableMemory;
    }
    
    /**
     * 设置是否启用内存
     */
    public void setEnableMemory(boolean enableMemory) {
        this.enableMemory = enableMemory;
    }
    
    /**
     * 是否启用调试
     */
    public boolean isEnableDebug() {
        return enableDebug;
    }
    
    /**
     * 设置是否启用调试
     */
    public void setEnableDebug(boolean enableDebug) {
        this.enableDebug = enableDebug;
    }
    
    /**
     * 获取语言
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * 设置语言
     */
    public void setLanguage(String language) {
        this.language = language;
    }
    
    /**
     * 获取LLM提供商
     */
    public String getLlmProvider() {
        return llmProvider;
    }
    
    /**
     * 设置LLM提供商
     */
    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }
    
    /**
     * 创建默认请求
     */
    public static AnalysisRequest createDefault(String stockCode, String stockName) {
        AnalysisRequest request = new AnalysisRequest();
        request.setStockCode(stockCode);
        request.setStockName(stockName);
        request.setResearchDepth(2);
        request.setAnalysisType("comprehensive");
        request.setEnableMemory(false);
        request.setEnableDebug(false);
        request.setLanguage("zh");
        request.setLlmProvider("openai");
        return request;
    }
}