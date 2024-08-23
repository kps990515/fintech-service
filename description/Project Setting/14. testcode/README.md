## testCode

### 환경세팅
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.squareup.okhttp3:mockwebserver:4.9.3'

```yaml
test {
    ignoreFailures = true // 실패 테스트가 있어도 빌드 될수있도록 세팅
}
```


### Mocking
#### 정의
- 실제 대신 가짜(Mock)객체를 사용해서 테스트하는 기법

#### 사용법
- 의존성을 모킹 + mock객체 생성 + 예상된 결과 세팅하여 테스트
- 
