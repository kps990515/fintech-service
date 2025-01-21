## 멀티모듈
- fintech-Serivce가 최상위 모듈
- 하위 api, service, db, common모듈
- 장점
  - 모듈화와 책임 분리: 프로젝트를 작은 부분으로 나눔으로써 각 모듈은 특정 기능을 담당
  - 재사용성 증가: 공통적으로 사용되는 기능을 모듈로 분리하여, 다른 프로젝트에서도 해당 모듈을 재사용
  - 병렬 개발 가능: 각 모듈이 독립적으로 작동하기 때문에, 여러 팀이나 개발자가 동시에 다른 모듈을 개발
  - 유연한 배포 옵션: 각 모듈을 독립적으로 배포할 수 있기 때문에, 전체 애플리케이션을 재배포하지 않고도 특정 모듈만 업데이트하는 것이 가능

### 최상위 모듈
- settings.gradle : 프로젝트의 모듈 구조를 정의
- build.gradle : 각 모듈의 종속성 및 빌드 구성을 정의
- bootjar 
  - 목적 : Spring Boot 실행 가능한 JAR 파일을 생성, 애플리케이션을 실행하는 데 필요한 모든 종속성을 포함, 내장된 Servlet 컨테이너(예: Tomcat)를 사용하여 독립 실행이 가능
  - 특징 
    - 최상위 모듈의 bootJar 작업은 실행 가능한 JAR 파일(uber-jar 또는 fat-jar)을 생성합니다. 이 JAR 파일은 애플리케이션을 실행하는 데 필요한 모든 클래스, 리소스, 그리고 종속성을 포함합니다.
    - 이 설정은 최상위 모듈의 Main-Class를 기반으로 애플리케이션의 실행 진입점을 정의하며, 필요한 모든 하위 모듈의 컴파일 결과물(클래스와 리소스)과 외부 라이브러리 종속성을 JAR 파일 내에 포함시킵니다.
    - 최상위 모듈의 bootJar는 하위 모듈의 JAR 파일을 직접 사용하지 않습니다. 대신, 하위 모듈에서 생성된 클래스 파일과 리소스가 빌드 과정 중에 클래스패스에 포함되며, 이를 기반으로 최종적인 실행 가능한 JAR 파일이 만들어집니다.

- jar
  - 목적 : 표준 Java 라이브러리 JAR 파일을 생성, 종속성 라이브러리는 포함하지 않습니다.
  - 특징 
    - JAR 파일은 종속성을 포함하지 않기 때문에, 이 JAR 파일을 사용하는 다른 프로젝트나 모듈은 필요한 종속성을 자신의 build.gradle에 명시적으로 추가해야 합니다.
    - JAR 파일에 종속성 라이브러리는 없지만 컴파일될때 maven에서 가져와 다운로드되어 실행
    - JAR 파일을 개별적으로 배포가능
    - 개별 모듈의 기능을 독립적으로 테스트가능

- bootjar와 jar의 관계
  - 최상위 모듈에서 bootJar를 실행할 때, 하위 모듈에서 생성된 클래스와 리소스는 최상위 모듈의 bootJar 생성 과정에 포함
  - 하위 모듈들이 생성한 독립적인 jar 파일 자체는 포함되지 않습니다. 대신, 하위 모듈의 컴파일 결과(클래스 파일)가 bootJar에 직접 통합
  - bootjar만 있으면 jar파일 없어도 단독 실행가능
  - 단, jar파일에 변동이 있으면 sync를 위해 bootjar 재생성 필요

- plugins : 소프트웨어 빌드 과정이나 개발 환경을 확장하거나 커스터마이즈하는 데 사용되는 도구
- dependencies : 애플리케이션 개발, 실행, 컴파일을 위해 필요한 외부 코드 라이브러리

1. build.gradle - allprojects 사용
```java
plugins { // 프로젝트에서 사용할 Gradle 플러그인을 선언
    id 'java'
    id 'org.springframework.boot' version '2.3.1.RELEASE'
    id 'io.spring.dependency-management' version '1.0.9.RELEASE'
    apply false // 최상위 레벨에서는 플러그인 적용을 막고, 각 모듈에서 개별적으로 적용
}

group = 'org.payment'
version = '1.0-SNAPSHOT'

allprojects {
    // 최상위 포함 하위 모듈에 적용될 설정
    repositories {
        // Gradle이 종속성을 해결할 때 Maven 중앙 저장소를 사용하도록 지정
        mavenCentral()
    }
    
    java {
        sourceCompatibility = '17'
    }

    configurations {
        compileOnly { // 컴파일 시간에만 필요하고 런타임에는 포함되지 않는 종속성을 관리
            // annotationProcessor가 컴파일 시에만 필요하며 런타임에는 필요하지 않다는 것을 명시
            extendsFrom annotationProcessor
        }
    }
}

subprojects {
    // 모든 하위 프로젝트에 적용될 설정
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'
    
    dependencies {
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
    }
}
bootJar{
    enabled = true
    mainClassName = 'com.example.banking.BankingApplication'
}

jar {
    enabled = false
}

```
2. build.gradle - subprojects만 사용
```java
plugins {
    // Java와 Spring Boot 관련 플러그인 추가 - 하위모듈에도 적용
    id 'java'
    id 'org.springframework.boot' version '2.3.1.RELEASE'
    id 'io.spring.dependency-management' version '1.0.9.RELEASE'
}

group = 'org.payment'   // 프로젝트 그룹 아이디 설정
version = '1.0-SNAPSHOT' // 프로젝트 버전 설정

java {
    sourceCompatibility = '17' // 사용할 Java 버전 설정
}

repositories {
    mavenCentral() // Maven 중앙 저장소 설정
}

dependencies {
    // 테스트 관련 종속성 예시
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

subprojects {
    // 모든 하위 프로젝트에 적용될 설정
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    repositories {
        mavenCentral() // 모든 하위 프로젝트에서 Maven 중앙 저장소를 사용하도록 설정
    }

    dependencies {
        // 하위 모듈에서 공통으로 사용될 기본 테스트 종속성
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
    }

    java {
        sourceCompatibility = '17' // 하위 모듈도 동일한 Java 버전 사용
    }

    configurations {
        compileOnly {
            extendsFrom annotationProcessor
        }
    }
}
bootJar{
    enabled = false
}

jar {
    enabled = false
}
```

3. settings.gradle
```java
rootProject.name = 'banking'
include 'api', 'service', 'db', 'common'
```

### 하위모듈
1. common
```java
dependencies {
    // 공통 종속성 추가
} 
```

2. db
```java
dependencies {
    implementation project(':common')
    runtimeOnly 'com.h2database:h2' // 예시 DB
}
jar {
    enabled = true
}
```

3. service
```java
dependencies {
    implementation project(':common')
    implementation project(':service')
    implementation 'org.springframework.boot:spring-boot-starter-web'
} 
jar {
    enabled = true
}
```

4. api
```java
dependencies {
    implementation project(':common')
    implementation project(':service')
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
bootJar {
    enabled = true
    mainClassName = 'com.example.banking.BankingApplication'
}
jar {
    enabled = false
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