# 1단계: 빌드 단계
FROM eclipse-temurin:17-jdk-jammy as builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper와 관련 파일 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# 의존성 다운로드 (Gradle 캐시를 활용하여 속도 향상)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN ./gradlew clean build -x test --no-daemon

# 2단계: 실행 단계
FROM eclipse-temurin:17-jre-jammy

# 실행 디렉토리 설정 (실제 JAR 파일이 실행될 위치)
WORKDIR /app

# 빌드 결과물만 복사
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 애플리케이션 실행
CMD ["java", "-jar", "./app.jar"]