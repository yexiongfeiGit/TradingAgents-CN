package com.tradingagents.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 安全配置类
 * 配置安全策略和权限控制
 */
@Configuration
@Profile("!test") // 测试环境不启用安全控制
public class SecurityConfig {

    // 暂时禁用安全配置，简化启动
    @Bean
    public Object securityFilterChain() {
        return new Object();
    }
}