## SpringEventListener
- 이벤트 기반의 프로그래밍을 가능하게 해주는 Spring 프레임워크의 기능

### 특징
- 내부 애플리케이션에서 동기/비동기 이벤트 처리
- 단일 애플리케이션 내 모듈 간 통신이나 이벤트 처리를 위해 사용(DB전송, 알림 발송 등)
- 메모리 내에서 이벤트가 처리되며 저장은 지원하지 않음
- 이벤트 클래스타입 기준으로 pub / sub

### Kafka와의 차이점
- Kafka : 분산시스템을 목적으로 확장성과 내구성에 초점
- SpringEvnetListener : 애플케이션 내부 경량 이벤트 처리 및 @TransactionalEventListener처리

  | **특징**                  | **Kafka**                                           | **Spring EventListener**                               |
  |---------------------------|-----------------------------------------------------|--------------------------------------------------------|
  | **주요 목적**              | **분산 시스템 간 메시지 처리**                       | **JVM 내 컴포넌트 간 이벤트 전달**                     |
  | **적용 범위**              | **다른 시스템/서비스 간 통신**                       | **동일 애플리케이션 내 모듈 간 통신**                   |
  | **내구성**                 | 메시지 **내구성 보장** (디스크 저장 및 재처리 가능)   | 내구성 없음 (애플리케이션 컨텍스트 내에서만 처리)      |
  | **확장성**                 | **높은 확장성** (분산 처리)                         | 낮음 (동일 JVM 내에서만 처리 가능)                     |
  | **비동기성**               | 기본적으로 **비동기 처리**                           | **동기/비동기** 처리 모두 가능                         |
  | **사용 예시**              | **대규모 이벤트 스트리밍, 시스템 간 통신**           | **JVM 내에서 간단한 이벤트 전달 및 모듈 간 통신**      |


## 멀티모듈설정하자 jenkins가 안돌았던 이유 : 로컬 dockerfile 수정을 안해서 alarm, common copy

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

