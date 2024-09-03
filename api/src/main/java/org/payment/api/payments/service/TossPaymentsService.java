package org.payment.api.payments.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.payment.api.payments.service.model.TransactionGetRequestVO;
import org.payment.api.payments.service.model.TransactionVO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.scheduler.Schedulers;
import reactor.netty.resources.ConnectionProvider;
import org.payment.api.config.service.TossPaymentsConfig;
import org.payment.api.payments.service.model.PaymentServiceConfirmRequestVO;
import org.payment.api.payments.service.model.PaymentServiceConfirmResponseVO;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
public class TossPaymentsService {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentsService.class);

    private final WebClient webClient;
    private final TossPaymentsConfig tossPaymentsConfig;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;


    @Autowired
    public TossPaymentsService(WebClient.Builder webClientBuilder, TossPaymentsConfig tossPaymentsConfig,
                               CircuitBreaker circuitBreaker, Retry retry) {
        this.tossPaymentsConfig = tossPaymentsConfig;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;

        // Connection Provider 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(500) // 최대 연결 수 설정
                .pendingAcquireTimeout(Duration.ofSeconds(60)) // 모든 연결 사용 중 시스템 자체 응답 대기 시간 설정
                .maxIdleTime(Duration.ofSeconds(20)) // 고객 비활동 상태의 최대 유휴 시간 설정
                .build();

        // HttpClient 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 서버 연결 타임아웃 설정
                .responseTimeout(Duration.ofSeconds(5)) // 서버 응답 타임아웃 설정
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5)) // 클라이언트가 응답받은 데이터 read
                                .addHandlerLast(new WriteTimeoutHandler(5))) // 클라이언드 주는 데이터 write
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL); // 요청/응답 로그 출력 (디버깅용)

        // WebClient 빌더에 HttpClient 적용
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(tossPaymentsConfig.getBaseUrl())
                .build();
    }

    public Mono<PaymentServiceConfirmResponseVO> sendPaymentConfirmRequest(PaymentServiceConfirmRequestVO requestVO) {
        String credentials = tossPaymentsConfig.getSecretKey() + ":"; // Secret key에 ":"를 추가
        String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return Mono.defer(() -> webClient.post() //Mono.defer()을 통해 호출시마다 새로운 mono생성해 독립적 요청 처리 보장
                        .uri(tossPaymentsConfig.getBaseUrl() + "/payments/confirm")
                        .header("Authorization", tossPaymentsConfig.getAuthorizationType() + " " + encodedAuth)
                        .header("Content-Type", tossPaymentsConfig.getContentType())
                        .bodyValue(requestVO)
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), this::handleError)
                        .bodyToMono(PaymentServiceConfirmResponseVO.class))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)) // 전역 Circuit Breaker 적용
                .transformDeferred(RetryOperator.of(retry)) // Retry 적용
                .subscribeOn(Schedulers.boundedElastic())  // mono의 비동기 작업을 boundedElastic 스케줄러에서 처리
                .doOnError(throwable -> {
                    if (!(throwable instanceof RuntimeException)) {
                        log.error("confirm API 에러 발생", throwable); // handleError에서 잡지않은 RuntimeException만 잡기
                    }
                });
    }

    public Mono<List<TransactionVO>> getTransaction(TransactionGetRequestVO requestVO) {
        String credentials = tossPaymentsConfig.getSecretKey() + ":"; // Secret key에 ":"를 추가
        String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return Mono.defer(() -> webClient.get()
                        .uri(uriBuilder -> {
                            uriBuilder
                                    .path(tossPaymentsConfig.getBaseUrl() + "/v1/transactions")
                                    .queryParam("startDate", requestVO.getStartDate())
                                    .queryParam("endDate", requestVO.getEndDate());
                            // Optional parameters 처리
                            if (requestVO.getStartingAfter() != null) {
                                uriBuilder.queryParam("startingAfter", requestVO.getStartingAfter());
                            }
                            if (requestVO.getLimit() > 0) { // 기본값을 0으로 가정하고 양수인 경우에만 설정
                                uriBuilder.queryParam("limit", requestVO.getLimit());
                            }
                            return uriBuilder.build();
                        })
                        .header("Authorization", tossPaymentsConfig.getAuthorizationType() + " " + encodedAuth)
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), this::handleError)
                        .bodyToMono(new ParameterizedTypeReference<List<TransactionVO>>() {})
                )
                .transformDeferred(RetryOperator.of(retry)) // Retry 적용 (먼저)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)) // Circuit Breaker 적용 (나중)
                .subscribeOn(Schedulers.boundedElastic()) // 비동기 작업을 boundedElastic 스케줄러에서 처리
                .doOnError(throwable -> {
                    if (!(throwable instanceof RuntimeException)) {
                        log.error("Transaction API 에러 발생", throwable); // handleError에서 처리하지 않은 에러 처리
                    }
                });
    }


    // 응답 에러 발생 시 동작
    private Mono<? extends Throwable> handleError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
                .flatMap(errorBody -> {
                    logErrorResponse(clientResponse.statusCode().value(), errorBody);
                    HttpStatus status = HttpStatus.valueOf(clientResponse.statusCode().value());
                    return Mono.error(WebClientResponseException.create(
                            status.value(),
                            status.getReasonPhrase(), // HTTP 상태 코드의 설명을 가져옴
                            clientResponse.headers().asHttpHeaders(),
                            errorBody.getBytes(),
                            null));
                });
    }

    private void logErrorResponse(int statusCode, String errorBody) {
        if (statusCode >= 500) {
            log.error("서버 에러 발생: {} - Response: {}", statusCode, errorBody);
        } else if (statusCode >= 400) {
            log.warn("클라이언트 에러 발생: {} - Response: {}", statusCode, errorBody);
        } else {
            log.info("Unexpected 에러 발생: {} - Response: {}", statusCode, errorBody);
        }
    }
}
