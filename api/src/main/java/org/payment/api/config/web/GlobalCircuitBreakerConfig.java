package org.payment.api.config.web;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GlobalCircuitBreakerConfig {

    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 실패율이 50%를 넘으면 Circuit Breaker 열림
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Circuit Breaker가 열린 후 재시도하기까지 대기 시간
                .slidingWindowSize(10) // 슬라이딩 윈도우 크기
                .build();
    }

    @Bean
    public CircuitBreaker defaultCircuitBreaker(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreaker.of("defaultCircuitBreaker", circuitBreakerConfig);
    }
}
