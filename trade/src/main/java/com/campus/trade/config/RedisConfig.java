package com.campus.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
    // 使用 Spring Boot 自动配置的 StringRedisTemplate，无需手动注入同名 Bean
}