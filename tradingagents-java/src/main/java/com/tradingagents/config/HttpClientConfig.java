package com.tradingagents.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * HTTP客户端配置类
 * 配置RestTemplate等HTTP客户端
 */
@Configuration
public class HttpClientConfig {

    /**
     * 标准RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 设置请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10秒连接超时
        factory.setReadTimeout(30000);    // 30秒读取超时
        restTemplate.setRequestFactory(factory);
        
        // 设置消息转换器
        restTemplate.setMessageConverters(Arrays.asList(
            new MappingJackson2HttpMessageConverter(),
            new StringHttpMessageConverter(StandardCharsets.UTF_8)
        ));
        
        return restTemplate;
    }

    /**
     * 市场数据专用RestTemplate
     */
    @Bean(name = "marketDataRestTemplate")
    public RestTemplate marketDataRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 设置请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5秒连接超时
        factory.setReadTimeout(15000);    // 15秒读取超时
        restTemplate.setRequestFactory(factory);
        
        // 设置消息转换器
        restTemplate.setMessageConverters(Arrays.asList(
            new MappingJackson2HttpMessageConverter(),
            new StringHttpMessageConverter(StandardCharsets.UTF_8)
        ));
        
        return restTemplate;
    }

    /**
     * AI服务专用RestTemplate
     */
    @Bean(name = "aiServiceRestTemplate")
    public RestTemplate aiServiceRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 设置请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30秒连接超时
        factory.setReadTimeout(120000);   // 120秒读取超时
        restTemplate.setRequestFactory(factory);
        
        // 设置消息转换器
        restTemplate.setMessageConverters(Arrays.asList(
            new MappingJackson2HttpMessageConverter(),
            new StringHttpMessageConverter(StandardCharsets.UTF_8)
        ));
        
        return restTemplate;
    }
}