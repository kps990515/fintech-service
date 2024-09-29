package org.payment.api.payments.service.pg;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.payment.api.common.exception.CustomWebClientException;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class DefaultPGService implements PGAdapter{
    // 인증헤더 생성
    protected HttpHeaders createAuthHeaders(String secretKey, String authorizationType) {
        HttpHeaders headers = new HttpHeaders();
        String credentials = secretKey + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", authorizationType + " " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // 비동기 락 획득
    protected Mono<Boolean> acquireLock(RedissonReactiveClient redissonReactiveClient, String lockName) {
        RLockReactive lock = redissonReactiveClient.getLock(lockName);
        return lock.tryLock(0, 5, TimeUnit.SECONDS);
    }

    // 비동기 락 해제
    protected void releaseLock(RedissonReactiveClient redissonReactiveClient, String lockName) {
        try {
            redissonReactiveClient.getLock(lockName).unlock().subscribe(
                    null,
                    error -> log.error("Failed to release lock: {}", error.getMessage())
            );
        } catch (Exception e) {
            log.error("Failed to release lock: {}", lockName, e);
        }
    }

    // 공통 에러 처리 메서드
    protected Mono<? extends Throwable> handleError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
                .flatMap(errorBody -> {
                    HttpStatus status = (HttpStatus) clientResponse.statusCode();
                    logErrorResponse(status, errorBody);
                    return Mono.error(new CustomWebClientException(status, errorBody));
                });
    }

    // 에러 로그 출력
    protected void logErrorResponse(HttpStatus status, String errorBody) {
        if (status.is5xxServerError()) {
            log.error("서버 에러 발생: {} - Response: {}", status, errorBody);
        } else if (status.is4xxClientError()) {
            log.warn("클라이언트 에러 발생: {} - Response: {}", status, errorBody);
        } else {
            log.info("예기치 못한 에러 발생: {} - Response: {}", status, errorBody);
        }
    }

    // 공통 CircuitBreaker와 Retry 적용 메서드
    protected <T> Mono<T> applyCircuitBreakerAndRetry(Mono<T> mono, CircuitBreaker circuitBreaker, Retry retry) {
        return mono.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }
}
