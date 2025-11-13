package com.tradingagents.api;

import com.tradingagents.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

/**
 * StockDataController测试类
 */
@DisplayName("股票数据API控制器测试")
public class StockDataControllerTest {

    @Mock
    private StockDataService stockDataService;

    @Mock
    private MarketDataProvider marketDataProvider;

    @Mock
    private FinancialDataUtils financialDataUtils;

    private StockDataController stockDataController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stockDataController = new StockDataController(stockDataService, marketDataProvider, financialDataUtils);
    }

    @Test
    @DisplayName("测试获取股票数据")
    void testGetStockData() {
        StockData mockStockData = StockData.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .currentPrice(150.0)
                .build();
        
        when(stockDataService.getStockData("AAPL")).thenReturn(mockStockData);
        
        ResponseEntity<?> response = stockDataController.getStockData("AAPL");
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("success", result.get("status"));
        assertEquals("AAPL", ((StockData) result.get("data")).getSymbol());
    }

    @Test
    @DisplayName("测试获取技术指标")
    void testGetTechnicalIndicators() {
        Map<String, Double> mockIndicators = Map.of(
                "sma20", 145.0,
                "sma50", 140.0,
                "rsi", 65.0,
                "macd", 2.5
        );
        
        when(stockDataService.getTechnicalIndicators("AAPL")).thenReturn(mockIndicators);
        
        ResponseEntity<?> response = stockDataController.getTechnicalIndicators("AAPL");
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("success", result.get("status"));
        
        Map<String, Double> indicators = (Map<String, Double>) result.get("data");
        assertEquals(145.0, indicators.get("sma20"));
        assertEquals(65.0, indicators.get("rsi"));
    }

    @Test
    @DisplayName("测试获取基本面数据")
    void testGetFundamentalData() {
        StockData mockStockData = StockData.builder()
                .symbol("AAPL")
                .pe(25.0)
                .eps(6.0)
                .marketCap(2500000000000L)
                .build();
        
        when(stockDataService.getStockData("AAPL")).thenReturn(mockStockData);
        
        ResponseEntity<?> response = stockDataController.getFundamentalData("AAPL");
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("success", result.get("status"));
        
        Map<String, Object> fundamentalData = (Map<String, Object>) result.get("data");
        assertEquals(25.0, fundamentalData.get("pe"));
        assertEquals(6.0, fundamentalData.get("eps"));
        assertEquals("2.5万亿", fundamentalData.get("marketCap"));
    }

    @Test
    @DisplayName("测试获取风险指标")
    void testGetRiskMetrics() {
        Map<String, Object> mockRiskMetrics = Map.of(
                "volatility", 0.15,
                "var_95", 0.05,
                "sharpeRatio", 1.2,
                "beta", 1.1
        );
        
        when(financialDataUtils.calculateRiskMetrics("AAPL")).thenReturn(mockRiskMetrics);
        
        ResponseEntity<?> response = stockDataController.getRiskMetrics("AAPL");
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("success", result.get("status"));
        
        Map<String, Object> riskMetrics = (Map<String, Object>) result.get("data");
        assertEquals(0.15, riskMetrics.get("volatility"));
        assertEquals(1.2, riskMetrics.get("sharpeRatio"));
    }

    @Test
    @DisplayName("测试获取批量股票数据")
    void testGetBatchStockData() {
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        
        StockData appleStock = StockData.builder().symbol("AAPL").currentPrice(150.0).build();
        StockData msftStock = StockData.builder().symbol("MSFT").currentPrice(300.0).build();
        StockData googleStock = StockData.builder().symbol("GOOGL").currentPrice(2500.0).build();
        
        when(stockDataService.getStockData("AAPL")).thenReturn(appleStock);
        when(stockDataService.getStockData("MSFT")).thenReturn(msftStock);
        when(stockDataService.getStockData("GOOGL")).thenReturn(googleStock);
        
        ResponseEntity<?> response = stockDataController.getBatchStockData(symbols);
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("success", result.get("status"));
        
        Map<String, StockData> stockDataMap = (Map<String, StockData>) result.get("data");
        assertEquals(3, stockDataMap.size());
        assertTrue(stockDataMap.containsKey("AAPL"));
        assertTrue(stockDataMap.containsKey("MSFT"));
        assertTrue(stockDataMap.containsKey("GOOGL"));
    }

    @Test
    @DisplayName("测试搜索股票")
    void testSearchStocks() {
        List<StockData> mockSearchResults = Arrays.asList(
                StockData.builder().symbol("AAPL").name("Apple Inc.").build(),
                StockData.builder().symbol("AAP").name("Advance Auto Parts").build()
        );
        
        when(stockDataService.searchStocks("AA")).thenReturn(mockSearchResults);
        
        ResponseEntity<?> response = stockDataController.searchStocks("AA");
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("success", result.get("status"));
        
        List<StockData> searchResults = (List<StockData>) result.get("data");
        assertEquals(2, searchResults.size());
    }

    @Test
    @DisplayName("测试获取市场概览")
    void testGetMarketOverview() {
        List<StockData> mockMarketIndices = Arrays.asList(
                StockData.builder().symbol("SPY").currentPrice(400.0).build(),
                StockData.builder().symbol("QQQ").currentPrice(350.0).build()
        );
        
        List<StockData> mockHotStocks = Arrays.asList(
                StockData.builder().symbol("AAPL").currentPrice(150.0).build(),
                StockData.builder().symbol("TSLA").currentPrice(200.0).build()
        );
        
        when(marketDataProvider.getMarketIndices()).thenReturn(mockMarketIndices);
        when(marketDataProvider.getHotStocks()).thenReturn(mockHotStocks);
        
        ResponseEntity<?> response = stockDataController.getMarketOverview();
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("success", result.get("status"));
        
        Map<String, Object> overviewData = (Map<String, Object>) result.get("data");
        assertNotNull(overviewData.get("indices"));
        assertNotNull(overviewData.get("hotStocks"));
    }

    @Test
    @DisplayName("测试获取缓存统计")
    void testGetCacheStatistics() {
        Map<String, Object> mockCacheStats = Map.of(
                "size", 10,
                "hitRate", 0.8,
                "missRate", 0.2
        );
        
        when(stockDataService.getCacheStatistics()).thenReturn(mockCacheStats);
        
        ResponseEntity<?> response = stockDataController.getCacheStatistics();
        
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("success", result.get("status"));
        
        Map<String, Object> cacheStats = (Map<String, Object>) result.get("data");
        assertEquals(10, cacheStats.get("size"));
        assertEquals(0.8, cacheStats.get("hitRate"));
    }

    @Test
    @DisplayName("测试错误处理")
    void testErrorHandling() {
        when(stockDataService.getStockData("INVALID")).thenThrow(new RuntimeException("Stock not found"));
        
        ResponseEntity<?> response = stockDataController.getStockData("INVALID");
        
        assertEquals(500, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals("error", result.get("status"));
        assertEquals("Stock not found", result.get("message"));
    }
}