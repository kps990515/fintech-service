## 멀티모듈 2
- 하나의 모듈이 다 관리하는것이 아닌 전체 프로젝트에서 하위 모듈들을 다 관리하는 방식

### 최상위 프로젝트
- settings.gradle
```yaml
rootProject.name = "coupon-version-control"
include("coupon-core", "coupon-api", "coupon-consumer")
```
- build.gradle : 하위모듈들에 한번에 dependency 적용
```yaml
subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.springframework.boot")

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.springframework.boot:spring-boot-starter-data-redis")
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        runtimeOnly("com.h2database:h2")
        runtimeOnly("com.mysql:mysql-connector-j")
        implementation("org.springframework.boot:spring-boot-starter")
        implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("io.micrometer:micrometer-registry-prometheus")
        annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
        annotationProcessor("jakarta.annotation:jakarta.annotation-api")
        annotationProcessor("jakarta.persistence:jakarta.persistence-api")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }
}
```

### 하위 모듈

1. Core : 공통 모듈
```java
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableCaching
@EnableJpaAuditing
@SpringBootApplication
public class CouponCoreConfiguration {
}
 
```

2. consumer : consumer 모듈
- implementation은 단순히 해당 모듈의 코드를 가져다가 쓰게 해줌
```yaml
dependencies {
  implementation(project(":coupon-core"))
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
} 
```

- @Import는 해당 모듈의 설정(빈, DB연결, 트랜잭션, 보안 등)을 가져와 스프링 컨텍스트에 포함시킴
- System.setProperty: 해당 모듈 설정에 여러개의 설정을 한번에 읽어서 적용 가능
```java
@Import(CouponCoreConfiguration.class)
@SpringBootApplication
public class CouponConsumerApplication {

    public static void main(String[] args) {
        // application-core.yml과 application-consumer.yml 파일에서 설정을 읽고 적용
        System.setProperty("spring.config.name", "application-core,application-consumer");
        SpringApplication.run(CouponConsumerApplication.class, args);
    }

}
```