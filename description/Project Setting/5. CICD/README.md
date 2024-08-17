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

      - name: Build Docker image
        run: docker build -t fintech-service .