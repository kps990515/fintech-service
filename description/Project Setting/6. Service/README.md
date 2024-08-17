## 멀티프로필
1. application.yaml, local, dev, prod 분리

### application.yaml
  ```java
  spring:
  profiles:
    active: prod  # 기본 활성화할 프로파일 (이 경우 local)

  jpa:
    show-sql: true
    properties:
      format_sql: true
      dialect: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: validate

  datasource:
    url: jdbc:mysql://localhost:3306/payment?useSSL=false&useUnicode=true&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: root1234!!

  security:
    user:
      name: user  # 사용자 이름
      password: password1!  # 비밀번호

server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: password1!
    key-store-type: PKCS12
    key-alias: myalias
    client-auth: none

toss:
  payments:
    base-url: "https://api.tosspayments.com/v1"
    secret-key: "test_sk_EP59LybZ8BploGpKdzqJV6GYo7pR"
    authorization-type: "Basic"
    content-type: "application/json"
  ```

### application-prod.yaml
```java
spring:
  jpa:
    show-sql: false
    properties:
      format_sql: true
      dialect: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: validate
  datasource:
    url: jdbc:mysql://sideproject-rds-mysql8.cjug24qm0gxh.ap-northeast-2.rds.amazonaws.com:3306/sideproject?useSSL=false&useUnicode=true&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: developer
    password: password1!
  security:
    user:
      name: user  # 사용자 이름
      password: password1!  # 비밀번호

logging:
  level:
    root: WARN

server:
  port: 443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: password1!
    key-store-type: PKCS12
    key-alias: myalias
    client-auth: none
```

- ci/cd 구축
1. CI 
    - 개발자가 지속적으로 중앙 리포지토리에 병합(통합)하는 것을 의미
    - 자동으로 빌드와 테스트가 수행되며, 이를 통해 코드 충돌이나 버그를 조기에 발견하고 해결
2. CD
    - 빌드된 코드가 테스트를 거쳐 프로덕션 환경에 자동으로 배포되는 것

- github Action 세팅
1. repository 생성
2. .github/workflows/ci-cd.yml파일 생성
```yml
name: CI/CD Pipeline

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew

      - name: Build with Gradle
        run: ./gradlew clean build

      - name: Build Docker image
        run: docker build -t fintech-service .

      - name: Run Docker container
        run: docker run -d -p 8080:8080 fintech-service
```
3. intellij 최상위에 dockerfile 생성
```yml
# 오픈jdk설정
FROM openjdk:17-jdk-slim

# 컨테이너 내부의 작업디렉토리 세팅
WORKDIR /app

# jar파일 생성할 위치(api모듈만 생성할꺼기 때문에 api/... 세팅)
COPY api/build/libs/*.jar app.jar 

# 외부에 공개할 포트
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

4. docker-compose파일 작성
```yml
version: '3.8'

services:
  api:
    build:
      context: .
      dockerfile: Dockerfile
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://host.docker.internal:3306/payment?useSSL=false&useUnicode=true&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root1234!!
    ports:
      - "8080:8080"
```

- docker란 무엇인가
1. 정의 
    - Docker는 **애플리케이션을 컨테이너(container)**라는 경량의 가상 환경에서 실행할 수 있게 해주는 오픈 소스 플랫폼
    - 서버내에서 docker를 실행시키고 docker내부에 있는 웹어플리케이션을 실행시키면 일관된 실행환경에서 실행가능
2. 장점
    - 일관된 실행 환경 제공
    - 운영 체제의 커널을 공유하면서도 애플리케이션을 격리하여 실행
    - 빠른 배포 및 확장성
    - 이식성 및 호환성
    - 개발 및 운영의 통합(DevOps)

## https 설정
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
```yaml
implementation 'org.springframework.boot:spring-boot-starter-security'
```

- Configuration
  - 하나 이상의 @Bean 메서드를 포함하고 있으며, 이 메서드들이 Spring 컨테이너에 의해 관리되는 빈(Bean)을 정의

- SecurityFilterChain
  - HTTP 요청이 서버에 도달하기 전에 여러 보안 필터를 거치도록 구성하는 체계

- authorizeHttpRequests()
  - HTTP 요청 권한 부여 설정
  - 여기서는 인증(로그인)이되어있는 Request만 받는 다는 세팅

- .httpBasic(withDefaults());
  - Authorization 헤더에 있는 사용자 이름과 비밀번호를 인증

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests.anyRequest().authenticated()
            )
            .httpBasic(withDefaults());

        return http.build();
    }
}
```

4. 접속하면 아이디 비밀번호 치라고 나올떄
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

    // PersonDTO를 Person 엔티티로 변환
    Person toEntity(PersonDTO dto);

    // Person 엔티티를 PersonDTO로 변환
    PersonDTO toDto(Person entity);
}
```