# 1단계: 빌드 단계
FROM eclipse-temurin:17-jdk-jammy as builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper와 관련 파일 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# 하위 모듈의 Gradle 설정 파일 복사
COPY api/build.gradle api/
COPY db/build.gradle db/

# 의존성 다운로드 (Gradle 캐시를 활용하여 속도 향상)
RUN ./gradlew dependencies --no-daemon

# 하위 모듈 소스 코드 복사
COPY api/src api/src
COPY db/src db/src

# 애플리케이션 빌드 (모든 모듈 빌드)
RUN ./gradlew clean build -x test --no-daemon

# 2단계: 실행 단계
FROM eclipse-temurin:17-jre-jammy

# 실행 디렉토리 설정 (실제 JAR 파일이 실행될 위치)
WORKDIR /app

# 빌드된 JAR 파일 복사 (여기서 api 모듈의 빌드 결과만 복사하는 예시)
COPY --from=builder /app/api/build/libs/*.jar ./app.jar

# 애플리케이션 실행
CMD ["java", "-jar", "./app.jar"]
