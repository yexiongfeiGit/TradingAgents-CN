//package com.tradingagents.api;
//
//import com.tradingagents.data.*;
//import lombok.extern.slf4j.Slf4j;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
///**
// * 股票数据API控制器
// * 提供股票数据的REST API接口
// */
//@RestController
//@RequestMapping("/api/stocks")
//@CrossOrigin(origins = "*")
//@Slf4j
//public class StockDataController {
//
//    private static final Logger log = LoggerFactory.getLogger(StockDataController.class);
//
//    @Autowired
//    private StockDataService stockDataService;
//
//    @Autowired
//    private MarketDataProvider marketDataProvider;
//
//    @Autowired
//    private FinancialDataUtils financialDataUtils;
//
//    /**
//     * 获取单只股票数据
//     */
//    @GetMapping("/{symbol}")
//    public ResponseEntity<?> getStockData(@PathVariable String symbol) {
//        try {
//            log.info("获取股票数据: {}", symbol);
//
//            StockData stockData = stockDataService.getStockData(symbol.toUpperCase());
//
//            if (stockData == null) {
//                return ResponseEntity.notFound().build();
//            }
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("symbol", stockData.getSymbol());
//            response.put("name", stockData.getName());
//            response.put("current_price", stockData.getCurrentPrice());
//            response.put("previous_close", stockData.getPreviousClose());
//            response.put("price_change", stockData.getPriceChange());
//            response.put("price_change_percent", stockData.getPriceChangePercent());
//            response.put("volume", stockData.getVolume());
//            response.put("market_cap", stockData.getMarketCap());
//            response.put("pe_ratio", stockData.getPeRatio());
//            response.put("pb_ratio", stockData.getPbRatio());
//            response.put("dividend_yield", stockData.getDividendYield());
//            response.put("open_price", stockData.getOpenPrice());
//            response.put("high_price", stockData.getHighPrice());
//            response.put("low_price", stockData.getLowPrice());
//            response.put("last_updated", stockData.getLastUpdated());
//            response.put("additional_data", stockData.getAdditionalData());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("获取股票数据失败: {}", symbol, e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取股票数据失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取实时股票数据（强制刷新）
//     */
//    @GetMapping("/{symbol}/real-time")
//    public ResponseEntity<?> getRealTimeStockData(@PathVariable String symbol) {
//        try {
//            log.info("获取实时股票数据: {}", symbol);
//
//            StockData stockData = stockDataService.getRealTimeStockData(symbol.toUpperCase());
//
//            if (stockData == null) {
//                return ResponseEntity.notFound().build();
//            }
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("symbol", stockData.getSymbol());
//            response.put("name", stockData.getName());
//            response.put("current_price", stockData.getCurrentPrice());
//            response.put("price_change", stockData.getPriceChange());
//            response.put("price_change_percent", stockData.getPriceChangePercent());
//            response.put("volume", stockData.getVolume());
//            response.put("last_updated", stockData.getLastUpdated());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("获取实时股票数据失败: {}", symbol, e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取实时股票数据失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取股票历史数据
//     */
//    @GetMapping("/{symbol}/history")
//    public ResponseEntity<?> getHistoricalData(
//            @PathVariable String symbol,
//            @RequestParam(defaultValue = "30") int days) {
//        try {
//            log.info("获取股票历史数据: {} ({}天)", symbol, days);
//
//            List<StockData> historicalData = stockDataService.getHistoricalData(symbol.toUpperCase(), days);
//
//            if (historicalData == null || historicalData.isEmpty()) {
//                return ResponseEntity.notFound().build();
//            }
//
//            List<Map<String, Object>> priceHistory = new ArrayList<>();
//            for (StockData data : historicalData) {
//                Map<String, Object> priceData = new HashMap<>();
//                priceData.put("date", data.getLastUpdated());
//                priceData.put("price", data.getCurrentPrice());
//                priceData.put("volume", data.getVolume());
//                priceData.put("high", data.getHighPrice());
//                priceData.put("low", data.getLowPrice());
//                priceData.put("open", data.getOpenPrice());
//                priceData.put("close", data.getCurrentPrice());
//                priceHistory.add(priceData);
//            }
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("symbol", symbol.toUpperCase());
//            response.put("period", days);
//            response.put("data_points", priceHistory.size());
//            response.put("price_history", priceHistory);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("获取历史数据失败: {}", symbol, e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取历史数据失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取技术指标
//     */
//    @GetMapping("/{symbol}/technical-indicators")
//    public ResponseEntity<?> getTechnicalIndicators(
//            @PathVariable String symbol,
//            @RequestParam(defaultValue = "30") int period) {
//        try {
//            log.info("获取技术指标: {} ({}天)", symbol, period);
//
//            Map<String, Double> indicators = stockDataService.getTechnicalIndicators(symbol.toUpperCase(), period);
//
//            if (indicators.isEmpty()) {
//                return ResponseEntity.notFound().build();
//            }
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("symbol", symbol.toUpperCase());
//            response.put("period", period);
//            response.put("indicators", indicators);
//            response.put("last_updated", LocalDateTime.now());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("获取技术指标失败: {}", symbol, e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取技术指标失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取基本面数据
//     */
//    @GetMapping("/{symbol}/fundamentals")
//    public ResponseEntity<?> getFundamentals(@PathVariable String symbol) {
//        try {
//            log.info("获取基本面数据: {}", symbol);
//
//            Map<String, Object> fundamentals = financialDataUtils.calculateFundamentalMetrics(symbol.toUpperCase());
//
//            if (fundamentals.containsKey("error")) {
//                return ResponseEntity.internalServerError()
//                        .body(createErrorResponse("获取基本面数据失败", (String) fundamentals.get("error")));
//            }
//
//            return ResponseEntity.ok(fundamentals);
//
//        } catch (Exception e) {
//            log.error("获取基本面数据失败: {}", symbol, e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取基本面数据失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取风险指标
//     */
//    @GetMapping("/{symbol}/risk-metrics")
//    public ResponseEntity<?> getRiskMetrics(
//            @PathVariable String symbol,
//            @RequestParam(defaultValue = "30") int period) {
//        try {
//            log.info("获取风险指标: {} ({}天)", symbol, period);
//
//            Map<String, Object> riskMetrics = financialDataUtils.calculateRiskMetrics(symbol.toUpperCase(), period);
//
//            if (riskMetrics.containsKey("error")) {
//                return ResponseEntity.internalServerError()
//                        .body(createErrorResponse("获取风险指标失败", (String) riskMetrics.get("error")));
//            }
//
//            return ResponseEntity.ok(riskMetrics);
//
//        } catch (Exception e) {
//            log.error("获取风险指标失败: {}", symbol, e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取风险指标失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取批量股票数据
//     */
//    @PostMapping("/batch")
//    public ResponseEntity<?> getBatchStockData(@RequestBody List<String> symbols) {
//        try {
//            log.info("获取批量股票数据: {}只股票", symbols.size());
//
//            List<String> upperCaseSymbols = symbols.stream()
//                    .map(String::toUpperCase)
//                    .toList();
//
//            List<StockData> stockDataList = stockDataService.getBatchStockData(upperCaseSymbols);
//
//            List<Map<String, Object>> response = new ArrayList<>();
//            for (StockData stockData : stockDataList) {
//                Map<String, Object> stockInfo = new HashMap<>();
//                stockInfo.put("symbol", stockData.getSymbol());
//                stockInfo.put("name", stockData.getName());
//                stockInfo.put("current_price", stockData.getCurrentPrice());
//                stockInfo.put("price_change", stockData.getPriceChange());
//                stockInfo.put("price_change_percent", stockData.getPriceChangePercent());
//                stockInfo.put("volume", stockData.getVolume());
//                stockInfo.put("market_cap", stockData.getMarketCap());
//                response.add(stockInfo);
//            }
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("count", response.size());
//            result.put("stocks", response);
//            result.put("last_updated", LocalDateTime.now());
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            log.error("获取批量股票数据失败", e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取批量股票数据失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 搜索股票
//     */
//    @GetMapping("/search")
//    public ResponseEntity<?> searchStocks(@RequestParam String query) {
//        try {
//            log.info("搜索股票: {}", query);
//
//            List<Map<String, Object>> results = stockDataService.searchStocks(query);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("query", query);
//            response.put("results", results);
//            response.put("count", results.size());
//            response.put("last_updated", LocalDateTime.now());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("搜索股票失败: {}", query, e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("搜索股票失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取市场概览
//     */
//    @GetMapping("/market-overview")
//    public ResponseEntity<?> getMarketOverview() {
//        try {
//            log.info("获取市场概览");
//
//            Map<String, Object> overview = stockDataService.getMarketOverview();
//
//            return ResponseEntity.ok(overview);
//
//        } catch (Exception e) {
//            log.error("获取市场概览失败", e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取市场概览失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取投资组合分析
//     */
//    @PostMapping("/portfolio-analysis")
//    public ResponseEntity<?> getPortfolioAnalysis(@RequestBody PortfolioRequest request) {
//        try {
//            log.info("获取投资组合分析: {}只股票", request.getSymbols().size());
//
//            // 验证输入
//            if (request.getSymbols().size() != request.getWeights().size()) {
//                return ResponseEntity.badRequest()
//                        .body(createErrorResponse("输入错误", "股票数量和权重数量不匹配"));
//            }
//
//            double totalWeight = request.getWeights().stream().mapToDouble(Double::doubleValue).sum();
//            if (Math.abs(totalWeight - 1.0) > 0.01) {
//                return ResponseEntity.badRequest()
//                        .body(createErrorResponse("输入错误", "权重总和必须等于1.0"));
//            }
//
//            Map<String, Object> analysis = financialDataUtils.calculatePortfolioMetrics(
//                    request.getSymbols(),
//                    request.getWeights()
//            );
//
//            if (analysis.containsKey("error")) {
//                return ResponseEntity.internalServerError()
//                        .body(createErrorResponse("投资组合分析失败", (String) analysis.get("error")));
//            }
//
//            return ResponseEntity.ok(analysis);
//
//        } catch (Exception e) {
//            log.error("投资组合分析失败", e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("投资组合分析失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取股票比较
//     */
//    @PostMapping("/compare")
//    public ResponseEntity<?> compareStocks(@RequestBody List<String> symbols) {
//        try {
//            log.info("股票比较: {}只股票", symbols.size());
//
//            List<String> upperCaseSymbols = symbols.stream()
//                    .map(String::toUpperCase)
//                    .toList();
//
//            Map<String, Object> comparison = stockDataService.getStockComparison(upperCaseSymbols);
//
//            return ResponseEntity.ok(comparison);
//
//        } catch (Exception e) {
//            log.error("股票比较失败", e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("股票比较失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取相关性分析
//     */
//    @PostMapping("/correlation")
//    public ResponseEntity<?> getCorrelation(@RequestBody CorrelationRequest request) {
//        try {
//            log.info("获取相关性分析: {}只股票 ({}天)", request.getSymbols().size(), request.getPeriod());
//
//            Map<String, Object> correlation = financialDataUtils.calculateCorrelation(
//                    request.getSymbols(),
//                    request.getPeriod()
//            );
//
//            if (correlation.containsKey("error")) {
//                return ResponseEntity.internalServerError()
//                        .body(createErrorResponse("相关性分析失败", (String) correlation.get("error")));
//            }
//
//            return ResponseEntity.ok(correlation);
//
//        } catch (Exception e) {
//            log.error("相关性分析失败", e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("相关性分析失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 获取缓存统计
//     */
//    @GetMapping("/cache-stats")
//    public ResponseEntity<?> getCacheStats() {
//        try {
//            Map<String, Object> stats = stockDataService.getCacheStats();
//            return ResponseEntity.ok(stats);
//
//        } catch (Exception e) {
//            log.error("获取缓存统计失败", e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("获取缓存统计失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 清除缓存
//     */
//    @DeleteMapping("/cache")
//    public ResponseEntity<?> clearCache() {
//        try {
//            stockDataService.clearCache();
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("message", "缓存已清除");
//            response.put("timestamp", LocalDateTime.now());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("清除缓存失败", e);
//            return ResponseEntity.internalServerError()
//                    .body(createErrorResponse("清除缓存失败", e.getMessage()));
//        }
//    }
//
//    /**
//     * 创建错误响应
//     */
//    private Map<String, Object> createErrorResponse(String error, String details) {
//        Map<String, Object> errorResponse = new HashMap<>();
//        errorResponse.put("error", error);
//        errorResponse.put("details", details);
//        errorResponse.put("timestamp", LocalDateTime.now());
//        return errorResponse;
//    }
//
//    /**
//     * 投资组合请求
//     */
//    public static class PortfolioRequest {
//        private List<String> symbols;
//        private List<Double> weights;
//
//        public List<String> getSymbols() { return symbols; }
//        public void setSymbols(List<String> symbols) { this.symbols = symbols; }
//        public List<Double> getWeights() { return weights; }
//        public void setWeights(List<Double> weights) { this.weights = weights; }
//    }
//
//    /**
//     * 相关性分析请求
//     */
//    public static class CorrelationRequest {
//        private List<String> symbols;
//        private int period = 30;
//
//        public List<String> getSymbols() { return symbols; }
//        public void setSymbols(List<String> symbols) { this.symbols = symbols; }
//        public int getPeriod() { return period; }
//        public void setPeriod(int period) { this.period = period; }
//    }
//}