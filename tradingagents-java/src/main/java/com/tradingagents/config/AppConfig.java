package com.tradingagents.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 配置导入类
 * 集中导入所有配置类
 */
@Configuration
@EnableConfigurationProperties({
    TradingAgentsConfig.class
})
@Import({
    WebConfig.class,
    AsyncConfig.class,
    CacheConfig.class,
    SecurityConfig.class
})
public class AppConfig {
}