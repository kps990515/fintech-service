## @Async(메소드용 비동기, 논블라킹 어노테이션)
- Spring에서 제공하는 비동기 애너테이션으로, @Async 붙은 메서드가 새로운 스레드에서 비동기적으로 실행

### 작동방법(프록시)
- 비동기 작업을 위해 프록시를 사용
- @Async가 달려있으면 프록시를 사용해 프록시 객체가 대신 메서드 실행
- 같은 클래스 내 @Async함수면 Spring이 프록시를 거치지 않아 사용불가

### 프록시
- 객체에 대한 대리자 역할을 하는 중간 객체
- 클라이언트가 직접 접근하지 않고 프록시 객체를 통해 호출을 대신 처리
- AOP, 트랜잭션, 비동기 관리에서 많이 사용

### 비동기 프로그래밍(3가지의 쓰임새가 다름)
1. WebClient(HTTP요청 처리용): Spring의 비동기적이고 논블로킹 방식으로 HTTP 요청을 처리하는 클라이언트
2. Mono, Flux(데이터용) : 리액티브 프로그래밍을 지원하는 Reactor 라이브러리의 비동기적 데이터 스트림
3. @Async(메서드용): Spring에서 제공하는 비동기 애너테이션으로, 메서드를 호출할 때 해당 메서드가 새로운 스레드에서 비동기적으로 실행

| **특징**               | **Mono (리액터)**                              | **WebClient**                          | **@Async**                            |
|------------------------|-----------------------------------------------|----------------------------------------|---------------------------------------|
| **주요 사용처**         | 비동기적 데이터 흐름 처리                      | 비동기적 HTTP 요청 및 응답 처리         | 메서드 실행을 비동기적으로 처리        |
| **비동기 방식**         | 리액티브 스트림 기반, 논블로킹 방식            | 리액터를 기반으로 논블로킹 HTTP 처리    | 새로운 스레드에서 메서드 실행          |
| **스레드 사용 방식**    | 스케줄러를 통해 적절한 스레드에서 실행          | 논블로킹 방식으로 스레드 점유 최소화   | 새로운 스레드를 사용하여 비동기 처리  |
| **처리 방식**           | 데이터 흐름 자체가 비동기적으로 논블로킹 처리   | HTTP 요청-응답을 논블로킹 처리         | 메서드 호출 시 메인 스레드를 차단하지 않음 |
| **주요 반환 타입**      | Mono 또는 Flux                                | Mono 또는 Flux                         | Future, CompletableFuture             |
| **블로킹 여부**         | 논블로킹                                      | 논블로킹                               | 비동기지만 블로킹 가능                |


### 사용 : 가입시 @Async로 가입완료 이메일 발송
1. build.gradle
```yaml
implementation 'org.springframework.boot:spring-boot-starter-mail'
```
  
2. yaml
```yaml
  mail:
    host: smtp.gmail.com # gmail로 설정
    port: 587
    username: kps990515@gmail.com
    password: bvjg roha grok dhrb
    properties:
      mail.smtp.debug: true
      mail.smtp.connectiontimeout: 1000 #1초
      mail.starttls.enable: true
      mail.smtp.starttls.enable: true  
```

```java
public String registerUser(UserRegisterServiceRequestVO requestVO) {
    userRdbRepository.findByEmail(requestVO.getEmail())
            .ifPresent(user -> {
                throw new ExistUserFoundException();
            });

    UserEntity newUser = userMapper.toUserEntity(requestVO);
    userRdbRepository.save(newUser);

    // 비동기 이메일 발송
    emailService.sendWelcomeEmailAsync(newUser.getEmail());

    return requestVO.getEmail();
}
```
- @Async함수
```java
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    @Async
    public void sendWelcomeEmailAsync(String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("환영합니다!");
            message.setText("회원 가입을 축하드립니다!");

            emailSender.send(message);
            System.out.println("비동기 이메일 전송 완료");
        } catch (MailException e) {
            // 예외 처리 로직 추가 (로깅 등)
            System.err.println("이메일 전송 중 오류 발생: " + e.getMessage());
        }
    }
}
```