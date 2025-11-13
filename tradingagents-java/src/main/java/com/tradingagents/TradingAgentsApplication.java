package com.tradingagents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * TradingAgents Java版本主应用类
 * 
 * AI多智能体股票分析系统
 * 支持多轮对话、辩论机制和条件逻辑控制
 */
@SpringBootApplication
@EnableAsync
@EnableRedisRepositories
public class TradingAgentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingAgentsApplication.class, args);
    }
}