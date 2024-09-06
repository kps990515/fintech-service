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

### 4. 로깅적용
1. Gradle
```yaml
implementation 'org.slf4j:slf4j-api:1.7.36'
implementation 'ch.qos.logback:logback-classic:1.4.12' 
```

2. logback.xml
- AsyncAppender : 로깅을 비동기로 처리하는 클래스
- discardingThreshold : 큐가 채워졌을 때 로그를 버리기 시작하는 기준을 설정하는 값
- neverBlock : 큐가 가득 찼을 때 새로운 로그 이벤트를 대기시키거나(false) 버리는 기준 값(true)
```xml
<configuration>
    <!-- 비동기 로깅을 위한 AsyncAppender 설정 -->
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender"> <!-- 로깅을 비동기로 처리 -->
        <appender-ref ref="FILE"/>
        <appender-ref ref="CONSOLE"/>
        <queueSize>5000</queueSize> <!-- 로깅 보관할 큐 사이즈 -->
        <discardingThreshold>90</discardingThreshold> <!-- 큐가 90%이면 버리기 시작함  -->
        <neverBlock>false</neverBlock> <!-- 큐 full일때 true이면 로그 버림, false이면 대기 -->
    </appender>

    <!-- 파일로 로그를 출력하는 FileAppender 설정 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file> <!-- 로그 저장될 위치 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 콘솔 출력용 콘솔 Appender 설정 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 루트 로거 설정 -->
    <root level="INFO">
        <appender-ref ref="ASYNC"/>
    </root>

    <!-- 특정 패키지에 대한 로깅 레벨 설정 -->
    <logger name="org.payment.api" level="DEBUG"/>
</configuration>
```
3. API
- API 요청에 대한 에러처리
```java
.doOnError(throwable -> {
    if (!(throwable instanceof RuntimeException)) {
        log.error("confirm API 에러 발생", throwable); // handleError에서 잡지않은 RuntimeException만 잡기
    }
})
```
- API 응답에 대한 에러처리
```java
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
// 콘솔, 파일적재 용
private void logErrorResponse(int statusCode, String errorBody) {
    if (statusCode >= 500) {
        log.error("서버 에러 발생: {} - Response: {}", statusCode, errorBody);
    } else if (statusCode >= 400) {
        log.warn("클라이언트 에러 발생: {} - Response: {}", statusCode, errorBody);
    } else {
        log.info("Unexpected 에러 발생: {} - Response: {}", statusCode, errorBody);
    }
}
```

### 4. 백프레셔(참고만)
- 정의 : 스트림 데이터의 생산자와 소비자 사이에 속도 차이를 조절하는 메커니즘
- 필요성 : 생산자의 생산 속도를 소비자가 못따라갈 수 있음
- 처리방법
  - 주된 방법 : 소비자가 데이터양을 조절
  - 생산자가 buffer(), drop(), sample()로 생산량 조절
- 함수
  - buffer : 데이터를 버퍼에 저장하여 일정량이 쌓였을 때 한꺼번에 소비자에게 전달
  - drop : 소비자가 소비할수없는 양일 경우 버림
  - sample : 중요 데이터만 전달

#### 지식
1. subscribeOn vs. publishOn
- subscribeOn : 스트림의 어디에 위치하든지 간에 스트림의 맨 처음 작업부터 스케줄러를 적용
- publishOn : 스트림에서 해당 메서드가 호출된 이후의 연산자들에 대해서만 스케줄러를 지정
  - 백프레셔를 관리하기 위해 publishOn후에 커스터마이징 가능
```java
.subscribeOn(Schedulers.boundedElastic())  // mono의 비동기 작업을 boundedElastic 스케줄러에서 처리

// 예시
.publishOn(Schedulers.boundedElastic()) // 이 지점 이후의 연산자들은 boundedElastic 스케줄러에서 실행됨
.limitRate(10) // flux에서 요청 처리 속도 제한
.onBackpressureBuffer(100, // 100개까지 버퍼 저장
        dropped -> log.warn("Dropped request: {}", dropped)) // 버퍼 초과 이후 로그로 남김

```

### 5. 부하테스트
- Jmeter사용하면 좋을듯
- 예시
  1. JMeter를 설치하고 실행합니다.
  2. 테스트 플랜을 작성하여 HTTP 요청을 정의합니다.
  3. Thread Group에서 사용자 수와 Ramp-up 시간(사용자가 늘어나는 시간), 루프 카운트(각 사용자가 요청을 반복하는 횟수)를 설정합니다.
  4. 결과를 수집하고 분석할 수 있도록 Listener를 추가합니다.
  5. 테스트를 실행하고 결과를 분석합니다.
