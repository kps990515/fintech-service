package org.payment.api.payments.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.payment.api.config.service.TossPaymentsConfig;
import org.payment.api.payments.service.model.PaymentServiceConfirmRequestVO;
import org.payment.api.payments.service.model.PaymentServiceConfirmResponseVO;
import org.payment.api.payments.service.model.TransactionGetRequestVO;
import org.payment.api.payments.service.model.TransactionVO;
import org.payment.api.payments.service.pg.DefaultPGService;
import org.payment.api.payments.service.pg.PGAdapter;
import org.redisson.api.RedissonReactiveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class TossPaymentsService extends DefaultPGService implements PGAdapter {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentsService.class);

    private final WebClient webClient;
    private final TossPaymentsConfig tossPaymentsConfig;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RedissonReactiveClient redissonReactiveClient;

    @Autowired
    public TossPaymentsService(WebClient webClient, TossPaymentsConfig tossPaymentsConfig,
                               CircuitBreaker circuitBreaker, Retry retry, RedissonReactiveClient redissonReactiveClient) {
        this.webClient = webClient;  // 의존성 주입된 WebClient 사용
        this.tossPaymentsConfig = tossPaymentsConfig;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.redissonReactiveClient = redissonReactiveClient;
    }

    @Override
    public Mono<PaymentServiceConfirmResponseVO> sendPaymentConfirmRequest(PaymentServiceConfirmRequestVO requestVO) {
        String lockName = "paymentConfirmLock:" + requestVO.getPaymentKey();
        return acquireLock(redissonReactiveClient, lockName)
                .flatMap(isLocked -> isLocked ? executePaymentConfirm(requestVO) : handleLockFailure())
                .doFinally(signalType -> releaseLock(redissonReactiveClient, lockName))
                .doOnError(throwable -> log.error("confirm API 에러 발생", throwable));
    }

    private Mono<PaymentServiceConfirmResponseVO> executePaymentConfirm(PaymentServiceConfirmRequestVO requestVO) {
        return applyCircuitBreakerAndRetry(
                webClient.post()
                        .uri(tossPaymentsConfig.getBaseUrl() + "/payments/confirm")
                        .headers(headers -> headers.addAll(createAuthHeaders(
                                tossPaymentsConfig.getSecretKey(), tossPaymentsConfig.getAuthorizationType())))
                        .bodyValue(requestVO)
                        .retrieve()
                        .bodyToMono(PaymentServiceConfirmResponseVO.class),
                circuitBreaker, retry
        );
    }

    private Mono<PaymentServiceConfirmResponseVO> handleLockFailure() {
        log.warn("Lock 획득 실패, 대체 처리 진행");
        return Mono.just(new PaymentServiceConfirmResponseVO());
    }

    @Override
    public Mono<List<TransactionVO>> getTransaction(TransactionGetRequestVO requestVO) {
        String lockName = "transactionLock:" + requestVO.getStartDate();
        return acquireLock(redissonReactiveClient, lockName)
                .flatMap(ignored -> webClient.get()
                        .uri(uriBuilder -> {
                            uriBuilder.path(tossPaymentsConfig.getBaseUrl() + "/v1/transactions")
                                    .queryParam("startDate", requestVO.getStartDate())
                                    .queryParam("endDate", requestVO.getEndDate());
                            if (requestVO.getStartingAfter() != null) {
                                uriBuilder.queryParam("startingAfter", requestVO.getStartingAfter());
                            }
                            if (requestVO.getLimit() > 0) {
                                uriBuilder.queryParam("limit", requestVO.getLimit());
                            }
                            return uriBuilder.build();
                        })
                        .headers(headers -> headers.addAll(createAuthHeaders(
                                tossPaymentsConfig.getSecretKey(), tossPaymentsConfig.getAuthorizationType())))
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<TransactionVO>>() {})
                )
                .transformDeferred(mono -> applyCircuitBreakerAndRetry(mono, circuitBreaker, retry))
                .doFinally(signalType -> releaseLock(redissonReactiveClient, lockName))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(throwable -> {
                    if (!(throwable instanceof RuntimeException)) {
                        log.error("Transaction API 에러 발생", throwable);
                    }
                });
    }
}
