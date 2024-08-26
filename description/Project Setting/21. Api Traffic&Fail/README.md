## 대용량 트래픽 대비 & 실패 대비

## 지식
- WebClient : 비동기/논블로킹 방식, 비동기 요청으로 높은 동시성 지원
- Reactor Netty : WebClient에서 서버와 통신할떄 사용하는 비동기 네트워크 라이브러리 
  - 네트워크 비동기 I/O처리 과정에서 **이벤트루프**라는 스레드 그룹 사용
      - 스레드 관리 스케쥴러
          - Elastic : 필요한 만큼 무제한 생성
          - BoundedElastic : 최대 스레드 수 제한
      - 문제점 : 대량요청으로 이벤트루프가 사용하는 스레드풀이 꽉차면 병목현상 발생
  

- Reactor : 비동기/논블로킹 스트림, 이벤트 처리
  - Mono : 최대 하나의 값을 비동기적으로 처리하는 클래스
  - Flux : 0개 이상의 값을 비동기 스트림으로 처리하는 클래스

## 1. 대용량 트래픽 대비

### 1. Webclient Connection Pooling설정
1. Connectin Provider
    - max Connection(최대 연결 풀 개수)
    - pendingAcquireTimeout 설정(풀 다 찼을때 유휴시간)
    - maxIdleTime(고객 비활동 상태 한계 시간 설정)
2. Http Client 설정
    - CONNECT_TIMEOUT(서버와 연결 최대시간)
    - responseTimeout(서버응답 최대시간)
    - ReadTimeout(고객이 서버응답 받는 최대시간)
    - WriteTimeout(고객이 서버에 전달하는 최대시간)
```java
@Autowired
public TossPaymentsService(WebClient.Builder webClientBuilder, TossPaymentsConfig tossPaymentsConfig) {
    this.tossPaymentsConfig = tossPaymentsConfig;

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
```

### 2. 비동기 처리 및 Thread Pool 관리
- mono.defer을 통해 각 구독자만의 새로운 비동기 작업 생성/관리
- boundedElastic을 통해 비동기 작업을 비동기에 특화된 스테르풀에서 관리

```java
return Mono.defer(() -> webClient.post() //Mono.defer()을 통해 호출시마다 새로운 mono생성해 독립적 요청 처리 보장
                .uri(tossPaymentsConfig.getBaseUrl() + "/v1/payments/confirm")
                .header("Authorization", tossPaymentsConfig.getAuthorizationType() + " " + encodedAuth)
                .header("Content-Type", tossPaymentsConfig.getContentType())
                .bodyValue(requestVO)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), this::handleError)
                .bodyToMono(PaymentServiceConfirmResponseVO.class))
        .subscribeOn(Schedulers.boundedElastic());  // mono의 비동기 작업을 boundedElastic 스케줄러에서 처리
```

### 3. CircuitBreaker & Retry 적용
- CircuitBreaker가 먼저 적용된 이유
  - CircuitBreaker 작동 후 : retry방지
  - CircuitBreaker 작동 전 : retry를 통해 재시도

1. build.gradle
```yaml
implementation 'io.github.resilience4j:resilience4j-spring-boot2:1.7.0'
implementation 'io.github.resilience4j:resilience4j-circuitbreaker:1.7.1'
implementation 'io.github.resilience4j:resilience4j-reactor:2.1.0'
implementation 'io.github.resilience4j:resilience4j-retry:2.1.0'
```

2. 전역 config설정
- GlobalCircuitBreakerConfig
```java
@Configuration
public class GlobalCircuitBreakerConfig {

    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 실패율이 50%를 넘으면 Circuit Breaker 열림
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Circuit Breaker가 열린 후 재시도하기까지 대기 시간
                .slidingWindowSize(10) // 슬라이딩 윈도우 크기(몇개의 요청을 기반으로 실패율 판별할건지)
                .build();
    }

    @Bean
    public CircuitBreaker defaultCircuitBreaker(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreaker.of("defaultCircuitBreaker", circuitBreakerConfig);
    }
} 
```
- GlobalRetryConfig
```java
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
```

3. API 적용하기
- Class 생성 시 적용
```java
@Autowired
public TossPaymentsService(WebClient.Builder webClientBuilder, TossPaymentsConfig tossPaymentsConfig,
        CircuitBreaker circuitBreaker, Retry retry) {
        this.tossPaymentsConfig = tossPaymentsConfig;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
```
- API 호출시 적용
```java
.transformDeferred(CircuitBreakerOperator.of(circuitBreaker)) // 전역 Circuit Breaker 적용
.transformDeferred(RetryOperator.of(retry)) // Retry 적용
```

### 4. Retry적용
1. build.gradle
```yaml
implementation 'io.github.resilience4j:resilience4j-retry:2.1.0'
```




