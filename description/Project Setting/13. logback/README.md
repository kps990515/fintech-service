## logback

### logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="local">
        <include resource="logback-local.xml"/>
    </springProfile>

    <springProfile name="prod">
        <include resource="logback-prod.xml"/>
    </springProfile>
</configuration>
```

### logback-local.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- 2025-02-02 15:34:56 [main] INFO  com.example.MainClass - Application started successfully -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### logback-prod.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 로그를 파일로 기록하며, 롤링(파일 분할 및 관리)을 지원하는 appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/spring-boot-app.log</file> <!-- 컨테이너 내부 경로 -->
        <!-- TimeBasedRollingPolicy는 시간 기반으로 로그를 나누는 정책 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/spring-boot-app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory> <!-- 로그를 30일 동안 보관 -->
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="ERROR">
        <!-- 위에 있는 "FILE" appender를 사용해서 ERROR로그에 적용 -->
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 로그확인
```shell
docker exec -it fintech-service-new /bin/bash
cd logs
ls // 로그 파일확인
cat spring-boot-app.2024-08-21.log
```