## Docker

### docker란 무엇인가
1. 정의 
    - Docker는 **애플리케이션을 컨테이너(container)**라는 경량의 가상 환경에서 실행할 수 있게 해주는 오픈 소스 플랫폼
    - 서버내에서 docker를 실행시키고 docker내부에 있는 웹어플리케이션을 실행시키면 일관된 실행환경에서 실행가능
2. 장점
    - 일관된 실행 환경 제공
    - 운영 체제의 커널을 공유하면서도 애플리케이션을 격리하여 실행
    - 빠른 배포 및 확장성
    - 이식성 및 호환성
    - 개발 및 운영의 통합(DevOps)

### docker 세팅

1. docker.file생성
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

2. docker-compose파일 작성
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



