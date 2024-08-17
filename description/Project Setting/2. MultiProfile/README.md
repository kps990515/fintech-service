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