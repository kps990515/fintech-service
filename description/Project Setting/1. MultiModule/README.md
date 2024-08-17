## 멀티모듈
- fintech-Serivce가 최상위 모듈
- 하위 API모듈
- 하위 DB모듈

### jar 생성 관리
- 루트 프로젝트는 빌드 관리를 위한 설정 파일(build.gradle, settings.gradle)을 포함하고, 실제 코드와 빌드 아티팩트는 서브모듈에서 관리

1. fintech-service
```java
bootJar { // 스프링부트 실행 Jar 미생성
    enabled = false
}

jar { // java jar 미생성
    enabled = false
}
```

2. api모듈
```java
bootJar { // 스프링부트 실행 Jar 생성
    enabled = true
}

jar { // 라이브러리로 사용할 jar 미생성
    enabled = false
}
```

3. db모듈
```java
bootJar { // 스프링부트 실행 Jar 미생성
    enabled = false
}

jar { // 라이브러리로 사용할 jar 생성
    enabled = true
}
```
- 멀티모듈에서 다른 모듈 가져다 쓰는 법
```java
// API 모듈에서
dependencies {
    implementation project(':db') // 멀티모듈에서 db모듈을 가져다가 쓰겠다
}
``````

- 하위모듈에서 스프링부트 프로젝트 실행 클래스 세팅
```java
@SpringBootApplication
public class ApiApplication {
    public static void main(String[] args){
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

1. application.yaml 분리
  - application.yaml
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

- 상위프로젝트에서 다른 모듈 세팅하는법
```java
allprojects { // Gradle 프로젝트의 루트 프로젝트(root project)와 모든 서브프로젝트에 공통으로 적용될 설정
    repositories { // 프로젝트가 의존성을 다운로드할 위치
        mavenCentral() // Maven Central 리포지토리에서 의존성설정(Gradle은 Maven도 사용가능)
    }
}
```

- application-prod.yaml
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