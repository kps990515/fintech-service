package org.payment.api.config.web;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GlobalRetryConfig {

    @Bean
    public RetryConfig defaultRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3) // 최대 3번 재시도
                .waitDuration(Duration.ofSeconds(2)) // 각 재시도 사이에 2초 대기
                .build();
    }

    @Bean
    // 여러 retry를 관리하기 위한 역할
    public RetryRegistry retryRegistry(RetryConfig retryConfig) {
        return RetryRegistry.of(retryConfig);
    }

    @Bean
    public Retry retry(RetryConfig retryConfig) {
        return Retry.of("defaultRetry", retryConfig);
    }
}
