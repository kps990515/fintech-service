## 토스페이먼츠 연동
1. 시크릿키
```yml
toss:
  payments:
    base-url: "https://api.tosspayments.com/v1"
    secret-key: "test_sk_EP59LybZ8BploGpKdzqJV6GYo7pR"
    authorization-type: "Basic"
    content-type: "application/json"
```
2. 토스페이먼츠 config
```java
@Getter
@ConfigurationProperties(prefix = "toss.payments")
@RequiredArgsConstructor
public class TossPaymentsConfig {

    private final String baseUrl;
    private final String secretKey;
    private final String authorizationType;
    private final String contentType;
}
```

3. 메인 java세팅
```java
@SpringBootApplication
@ConfigurationPropertiesScan // @ConfigurationProperties로 주석이 달린 클래스를 자동으로 스캔하고, 빈으로 등록하는 역할
public class ApiApplication {
    public static void main(String[] args){
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

4. TossPaymentsService

- webClient
  - 비동기적이고 논블로킹 방식의 HTTP 클라이언트로
  - 주로 REST API 호출이나 외부 서비스와의 통신을 비동기적으로 처리하기 위해 사용

- .flatMap()
  - 하나의 값을 처리하고, 그 결과로 새로운 Mono나 Flux를 반환하는 비동기 작업을 수행

```java
@Service
public class TossPaymentsService {

    private final WebClient webClient;
    private final TossPaymentsConfig tossPaymentsConfig;

    @Autowired
    public TossPaymentsService(WebClient.Builder webClientBuilder, TossPaymentsConfig tossPaymentsConfig) {
        this.webClient = webClientBuilder.baseUrl(tossPaymentsConfig.getBaseUrl()).build();
        this.tossPaymentsConfig = tossPaymentsConfig;
    }

    public Mono<PaymentServiceConfirmResponseVO> sendPaymentConfirmRequest(PaymentServiceConfirmRequestVO requestVO) {
        String encodedAuth = Base64.getEncoder().encodeToString(tossPaymentsConfig.getSecretKey().getBytes());

        return webClient.post()
                .uri("/payments/confirm")
                .header("Authorization", tossPaymentsConfig.getAuthorizationType() + " " + encodedAuth)
                .header("Content-Type", tossPaymentsConfig.getContentType())
                .bodyValue(requestVO)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), this::handleError)
                .bodyToMono(PaymentServiceConfirmResponseVO.class);
    }

    private Mono<? extends Throwable> handleError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
                .flatMap(errorBody -> Mono.error(new RuntimeException("API Error: " + errorBody)));
    }
}
```