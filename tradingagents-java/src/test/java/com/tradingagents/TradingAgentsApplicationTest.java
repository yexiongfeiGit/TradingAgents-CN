package com.tradingagents;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 主应用测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TradingAgents应用测试")
public class TradingAgentsApplicationTest {

    @Test
    @DisplayName("测试应用上下文加载")
    void contextLoads() {
        // 测试Spring Boot应用上下文是否正确加载
        assertTrue(true);
    }

    @Test
    @DisplayName("测试主类启动")
    void testMainMethod() {
        // 测试主类的main方法是否可以正常执行
        assertDoesNotThrow(() -> {
            TradingAgentsApplication.main(new String[]{});
        });
    }
}