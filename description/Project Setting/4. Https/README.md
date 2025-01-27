## Https
1. ssl 인증서
```bash
keytool -genkeypair -alias myalias -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

2. yaml설정
```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: password1!
    key-store-type: PKCS12
    key-alias: myalias
```

3. spring security 설정
- build.gradle
```yaml
implementation 'org.springframework.boot:spring-boot-starter-security'
```

4. SecurityConfig.java
```java
@Configuration
public class SecurityConfig {
    @Bean
    // SecurityFilterChain : HTTP 요청이 서버에 도달하기 전에 여러 보안 필터를 거치도록 구성하는 체계
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // HTTP 요청에 대한 보안 규칙을 설정합니다.
            .authorizeHttpRequests(authorizeRequests ->
                // 보안 규칙 : 모든 요청에 대해 인증된 사용자만 접근할 수 있도록 합니다.
                authorizeRequests.anyRequest().authenticated()
            )
            // HTTP 기본 인증을 활성화합니다.
            .httpBasic(withDefaults());

        // 구성된 HttpSecurity 객체를 기반으로 SecurityFilterChain 객체를 생성하고 반환합니다.
        return http.build();
    }
}
```

5. 접속하면 아이디 비밀번호 치라고 나올떄
```yaml
// application.yaml에 아래와 같이 테스트 세팅
  security:
    user:
      name: user  # 사용자 이름
      password: password1!  # 비밀번호
```

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

2. 메인 java세팅
```java
@SpringBootApplication
@ConfigurationPropertiesScan // @ConfigurationProperties로 주석이 달린 클래스를 자동으로 스캔하고, 빈으로 등록하는 역할
public class ApiApplication {
    public static void main(String[] args){
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

3. 토스페이먼츠 config
```java
@Getter
// 타입-세이프(type-safe)한 방식으로 외부 설정을 클래스로 바인딩하는 용도로 사용
@ConfigurationProperties(prefix = "toss.payments")
@RequiredArgsConstructor
public class TossPaymentsConfig {

    private final String baseUrl;
    private final String secretKey;
    private final String authorizationType;
    private final String contentType;
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



## 객체변환
1. ObjectMapper
  - Jackson 라이브러리
  - JSON과 Java 객체 간의 직렬화 및 역직렬화
  - 장점 : 빠른개발속도, 범용성
  - 단점 : 런타임에 작업됨, 타입안정성부족
```java
ObjectMapper objectMapper = new ObjectMapper();
String jsonString = objectMapper.writeValueAsString(someObject); // Java 객체를 JSON 문자열로 변환
SomeObject obj = objectMapper.readValue(jsonString, SomeObject.class); // JSON 문자열을 Java 객체로 변환
```

2. Mapstruct
  - 타입 간의 매핑(예: DTO와 엔티티 간)을 처리하는 컴파일러 기반의 매핑 프레임워크
  - 장점 : 컴파일 타임 매핑, 타입 안정성
  - 단점 : 개발어려움
```java
@Mapper
public interface PersonMapper {
    PersonMapper INSTANCE = Mappers.getMapper(PersonMapper.class);
    
    @Mapping(source = "userId", target = "id")
    UserVO toUserVO(UserEntity userEntity);

    @Mapping(target = "joinedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "modifiedAt", expression = "java(java.time.LocalDateTime.now())")
    UserEntity toUserEntity(UserRegisterServiceRequestVO userRegisterServiceRequestVO);
}
```