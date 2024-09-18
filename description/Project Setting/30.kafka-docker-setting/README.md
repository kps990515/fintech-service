## 메시지브로커
- 메시지 송신자, 수신자를 중개하는 미들웨어
- 메시지브로커 : 메시지 전달 / 큐역할 / pub,sub구조
- 이벤트브로커 : 메시지브로커 포함 & 메시지 인덱스 / 이벤트 저장 / 이벤트 소싱
- 장점 : 비동기, 시스템 디커플링, 탄력성, 확장성

## Kafka
- producer : 메시지 생상자
- consumer : 메시지 소비자
- consumer group : 특정 Topic메시지를 읽어가는 소비자 그룹
- Topic : 메시지 구분자
  - 같은 Topic을 여러 Consuer Group이 소비 가능
- Partition : 각 topic을 parition으로 나누어 병렬처리
  - 하나의 Partition은 하나의 Consumer Group에서 하나의 Consumer만 소비 가능
  - 하나의 consumer는 여러 Partition을 소비할수있지만, 하나의 partiton을 여러 consumer 소비는 불가능
- offset : 각 partition안에서 메시지 순서 표시

## Docker 설정
1. build.gradle에서 version제거하기(버전이 붙어서 jar제작됨)
2. dockerfile만들기
```yaml
# 1단계: 빌드 단계
FROM eclipse-temurin:17-jdk-jammy as builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper와 관련 파일 복사
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# 소스 코드 복사
COPY src/ src/

# 의존성 다운로드 및 애플리케이션 빌드 (Gradle 캐시를 활용)
RUN ./gradlew clean build -x test --no-daemon

# 2단계: 실행 단계
FROM eclipse-temurin:17-jre-jammy

# 실행 디렉토리 설정 (실제 JAR 파일이 실행될 위치)
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar ./app.jar

# 애플리케이션 실행
CMD ["java", "-jar", "./app.jar"]
```
3. docker-compose.yml
  - 외부접근을 포트로 구분
  - 여러 컨테이너를 포트로 구분해서 사용하기 위해
  - 내부에서는 8080을 동일적으로 사용
  - 내부 mysql을 따로 띄울수도 있음
```yml
version: '3.8'

services:
  app-1:
    image: kafka-kasandra
    ports:
      - "8080:8080"
  app-2:
    image: kafka-kasandra
    ports:
      - "8050:8080" 
  mysql-server:
    image: mysql
    environment:
      - MYSQL_ROOT_PASSWORD=root1234!!
    ports:
      - "3306:3306"

  member-service:
    image: member-service
    ports:
      - 8081:8080
```
4. 빌드
  - ./gradlew clean build
  - docker build -t kafka-kasandra .

5. 실행
  - cmd창에서 docker run -p 8080:8080 kafka-kasandra

