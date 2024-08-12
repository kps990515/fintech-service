# 오픈jdk설정
FROM openjdk:17-jdk-slim

# 컨테이너 내부의 작업디렉토리 세팅
WORKDIR /app

# jar파일 생성할 위치
COPY api/build/libs/*.jar app.jar

# 외부에 공개할 포트
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app/app.jar"]