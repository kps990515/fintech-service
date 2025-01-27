## Docker

### docker란 무엇인가
1. 정의 
    - Docker
      - 애플리케이션을 컨테이너(container)라는 경량의 가상 환경에서 실행할 수 있게 해주는 오픈 소스 플랫폼
      - 애플리케이션과 그 의존성을 컨테이너 내에 패키징하여, 어떤 환경에서도 일관된 동작을 보장
    - Docker Image : Docker 컨테이너 실행에 필요한 모든 파일과 설정을 포함하는 불변의 템플릿
    - Docker Container
      - Docker 이미지를 실행한 인스턴스
      - 각 컨테이너는 독립된 환경을 제공하며, 하나의 시스템에서 여러 컨테이너를 동시에 실행가능
      - 애플리케이션과 그 애플리케이션이 필요로 하는 모든 환경(라이브러리, 종속성 등)을 패키징하는 도구(어느환경에서나 동일 작동)
      - 서버내에서 docker를 실행시키고 docker내부에 있는 웹어플리케이션을 실행시키면 일관된 실행환경에서 실행가능
    - 순서 : 파일 시스템과 설정이 포함된 템플릿(Dockerfile로 빌드) -> Docker Image(클래스) -> Docker Container(객체)
    - K8S : 컨테이너들을 효율적으로 관리 및 오케스트레이션(컨테이너 상태를 관리, 서비스 가용성 유지, 오토스케일링)
    - Istio : K8S위에서 동작하는 Service Mesh(MSA에서 네트워크 통신을 관리, 제어하기 위한 인프라 계층)
      - Kubernetes 상에서 실행되는 서비스들 간 보안 통신, 트래픽 관리, 서비스 모니터링 및 로깅 등을 제공 및 관리
2. 장점
    - 일관된 실행 환경 제공
    - 운영 체제의 커널을 공유하면서도 애플리케이션을 격리하여 실행
    - 빠른 배포 및 확장성
    - 이식성 및 호환성
    - 개발 및 운영의 통합(DevOps)

### DID
1. 정의 : Docker 컨테이너 내부에서 다른 Docker 컨테이너를 실행할 수 있게 하는 설정
2. 사용이유 : Jenkins를 Docker Container에서 실행하고, 그 Container 내부에서 다른 Docker 이미지, 컨테이너 실행

### docker 세팅

1. Dockerfile 생성
   -  하나의 컨테이너 이미지를 구축하기 위한 명령어들의 집합
     - 이미지 정의, 컨테이너 빌드, 재사용성
   - --no-daemon : Gradle 데몬을 사용하지 않음
     - Gradle 데몬은 빌드 속도를 향상시키기 위해 백그라운드에서 실행되는 프로세스
     - --no-daemon 옵션을 사용하면, 각 빌드가 독립적으로 실행되어 빌드 완료 후에 모든 리소스가 정리
     - 빌드가 각 실행마다 JVM을 새로 시작해야 하므로, 빌드 시간이 길어질 수 있음
```yml
# 1단계: 빌드 단계
FROM eclipse-temurin:17-jdk-jammy as builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper(Gradle 실행환경)와 관련 파일 복사
COPY gradlew . # host 머신의 gradlew 실행파일을 현재 디렉토리에 복사
COPY gradle/ gradle/ # gradle 디렉토리와 그 내용을 컨테이너의 /app/gradle로 복사
COPY build.gradle settings.gradle ./
# build.gradle 및 settings.gradle 파일을 컨테이너의 작업 디렉토리로 복사

# 하위 모듈의 Gradle 설정 파일 복사
COPY api/build.gradle api/
COPY db/build.gradle db/

# 의존성 다운로드 (--no-daemon : Gradle 데몬을 사용하지 않음)
RUN ./gradlew dependencies --no-daemon

# 하위 모듈 소스 코드 복사
COPY api/src api/src
COPY db/src db/src

# 애플리케이션 빌드 (모든 모듈 빌드) -x test(테스트 제외)
RUN ./gradlew clean build -x test --no-daemon

# 2단계: 실행 단계
FROM eclipse-temurin:17-jre-jammy

# 실행 디렉토리 설정 (실제 JAR 파일이 실행될 위치)
WORKDIR /app

# 빌드된 JAR 파일 복사 (여기서 api 모듈의 빌드 결과만 복사하는 예시)
COPY --from=builder /app/api/build/libs/*.jar ./app.jar

# 애플리케이션 실행
CMD ["java", "-jar", "./app.jar"]
```

2. docker-compose파일 작성
   - 여러 컨테이너를 정의하고 실행하기 위해 사용
   - docker-compose를 사용하여 여러 서비스(컨테이너), 네트워크, 볼륨 등을 설정 및 오케스트레이션
   - docker-compose up: docker-compose.yml 파일에 정의된 모든 서비스를 시작
   - docker-compose down: docker-compose.yml 파일에 정의된 모든 서비스를 중지하고 관련 리소스를 제거
   - docker-compose restart: 서비스를 재시작
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

2. docker image생성
   - mysql
   ```shell
   # -d : 백그라운드에서 실행, --name : 이름 할당
   docker run -d --name mysql_container 
   -e MYSQL_ROOT_PASSWORD=root1234!! # -e 환경변수 할당
   -p 3306:3306 
   --restart unless-stopped 
   mysql:8.0
   ```
   - redis
   ```shell
   docker run -d --name redis_container `
   -p 6379:6379 `
   --restart unless-stopped `
   redis:alpine
   ```



