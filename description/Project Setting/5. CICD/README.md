## ci/cd 구축
1. CI 
    - 개발자가 지속적으로 중앙 리포지토리에 병합(통합)하는 것을 의미
    - 자동으로 빌드와 테스트가 수행되며, 이를 통해 코드 충돌이나 버그를 조기에 발견하고 해결
2. CD
    - 빌드된 코드가 테스트를 거쳐 프로덕션 환경에 자동으로 배포되는 것

- github Action 세팅
1. repository 생성
2. .github/workflows/ci-cd.yml파일 생성
```yml
name: CI/CD Pipeline

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew

      - name: Build with Gradle
        run: ./gradlew clean build

      - name: Deploy to EC2
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.HOST }}
          username: ${{ secrets.USERNAME }}
          key: ${{ secrets.KEY }}
          port: ${{ secrets.PORT }}
          script: |
            cd /home/ec2-user/fintech-service/api/build/libs/
            ## 'java -jar' 문자열을 포함하는 모든 프로세스를 찾아 종료
            ## true : pkill 명령어가 실패해도 계속 스크립트 실행
            sudo pkill -f 'java -jar' || true
            
            ## nohup : 로그아웃 후에도 명령어가 계속 실행
            ## 2>&1: 표준 에러 출력을 표준 출력과 같은 곳으로 리다이렉트합니다. 이는 에러 메시지도 app.log 파일에 기록 
            ## & : 백그라운드에서 실행
            nohup sudo java -jar api-1.0-SNAPSHOT.jar > app.log 2>&1 &
```