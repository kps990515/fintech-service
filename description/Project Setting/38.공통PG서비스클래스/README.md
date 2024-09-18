## 공통 PG 서비스 사용 클래스
1. PGConfig : PG설정(URL, 비밀키등) 인터페이스 생성 후 PG사에 implement
2. WebClientConfig 
   - PGConfig를 파라미터로 받아 해당 PG에 맞는 WebClient 생성
   - Webclient(ConnectionPool, httpClient) 세팅을 공통으로 분리
3. DefaultPGService : API통신에 사용하는 공통함수 분리
    - 인증헤더 생성
    - 비동기 락 획득/해제
    - 공통 에러 처리 / 로그 출력
    - CircuitBreaker / Retry함수
4. CustomWebClientException 생성
    - client, Server에러 분리할 Exception 생성

### PGConfig
```java
public interface PGConfig {
    String getBaseUrl();
    String getSecretKey();
    String getAuthorizationType();
    String getContentType();
}
```
```java
public class TossPaymentsConfig implements PGConfig {
```

### WebClientConfig
- PGConfig를 파라미터로 받아 WebClient생성

```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(PGConfig pgConfig) {
        // Connection Provider 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(500) // 최대 연결 수 설정
                .pendingAcquireTimeout(Duration.ofSeconds(60)) // 응답 대기 시간 설정
                .maxIdleTime(Duration.ofSeconds(20)) // 유휴 시간 설정
                .build();

        // HttpClient 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 서버 연결 타임아웃 설정
                .responseTimeout(Duration.ofSeconds(5)) // 서버 응답 타임아웃 설정
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5)) // 데이터 read
                                .addHandlerLast(new WriteTimeoutHandler(5))) // 데이터 write
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL); // 요청/응답 로그 출력 (디버깅용)

        // WebClient 빌더에 HttpClient 적용
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(pgConfig.getBaseUrl())
                .build();
    }
}
```

### DefaultPGService
- 인증헤더 생성
- 비동기 락 획득/해제
- 공통 에러 처리 / 로그 출력
- CircuitBreaker / Retry함수
```java
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
         redissonReactiveClient.getLock(lockName).unlock().subscribe();
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
                    // 새로운 CustomWebClientException생성
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
```

### CustomWebClientException
```java
@Getter
public class CustomWebClientException extends RuntimeException {
    private final HttpStatus status;
    private final String responseBody;

    public CustomWebClientException(HttpStatus status, String responseBody) {
        super("WebClient 에러 발생: " + status);
        this.status = status;
        this.responseBody = responseBody;
    }
}
```
