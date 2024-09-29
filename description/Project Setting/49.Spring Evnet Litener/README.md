## SpringEventListener
- 이벤트 기반의 프로그래밍을 가능하게 해주는 Spring 프레임워크의 기능

### 특징
- 내부 애플리케이션에서 동기/비동기 이벤트 처리
- 단일 애플리케이션 내 모듈 간 통신이나 이벤트 처리를 위해 사용(DB전송, 알림 발송 등)
- 메모리 내에서 이벤트가 처리되며 저장은 지원하지 않음
- 이벤트 클래스타입 기준으로 pub / sub

### 사용법
1. 이벤트 클래스 정의(ApplicationEvent 상속 OR VO로도 가능)
- api, alarm모듈에서 사용할 수 있게 common모듈에 생성 및 implement
```java
@Getter
@RequiredArgsConstructor
public class UserRegisterEvent {
    private String email;
} 
```

2. api모듈 이벤트 발행
- UserRegisterEvent 기준으로 발행
```java
// 이메일 발송 pub
publisher.publishEvent(new UserRegisterEvent(newUser.getEmail())); 
```

3. alarm 모듈 이벤트 수신
- UserRegisterEvent 기준으로 수신
```java
@Service
@Slf4j
@RequiredArgsConstructor
public class UserNotificationService {
    private final EmailService emailService;

    @EventListener
    public void handleUserRegisterEvent(UserRegisterEvent event) {
        log.info("Received UserRegisterEvent: email={}", event.getEmail());
        String email = event.getEmail();
        sendEmail(email);

    }

    private void sendEmail(String email) {
        try {
            emailService.sendWelcomeEmailAsync(email);
            System.out.println("비동기 결제 이메일 전송 완료");
        } catch (MailException e) {
            // 예외 처리 로직 추가 (로깅 등)
            System.err.println("이메일 전송 중 오류 발생: " + e.getMessage());
        }
    }
} 
```

