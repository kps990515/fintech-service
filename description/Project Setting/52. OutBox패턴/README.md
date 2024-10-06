## OutBox 패턴

### 정의
- 이벤트기반시스템에서 DB와 메시지 브로커 간 트랜잭션 일관성을 유지하기 위한 패턴

### 원리
1. 트랜잭션 내에서 비즈니스이벤트 발생 -> OutBox테이블에 저장
2. 저장된 Outbox테이블 레코드 기반으로 메시징 시스템에 전달
3. 메시지가 정상 전송되면 Outbox테이블에서 레코드 삭제



- 비즈니스 코드
```java
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRedisService outboxRedisService;

    @Transactional
    public Order createOrder(String customerName, String product, int quantity, double price) {
        // 주문 생성
        Order order = new Order(customerName, product, quantity, price);
        orderRepository.save(order);

        // 주문 생성 이벤트 발생 (Outbox에 저장)
        OrderCreatedEvent event = order.toEvent();
        outboxRedisService.saveMessageToOutbox(event.toString());

        return order;
    }
}
```

- OutboxRedisService
```java
@Service
public class OutboxRedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String OUTBOX_KEY = "outbox_messages";

    // 메시지 저장
    public void saveMessageToOutbox(String message) {
        redisTemplate.opsForList().rightPush(OUTBOX_KEY, message);
    }

    // Outbox에서 메시지 가져오기
    public List<String> fetchOutboxMessages() {
        return redisTemplate.opsForList().range(OUTBOX_KEY, 0, -1);
    }

    // 메시지 삭제
    public void removeMessageFromOutbox(String message) {
        redisTemplate.opsForList().remove(OUTBOX_KEY, 1, message);
    }
}
```

- kafka이벤트 전송
```java
@Service
public class KafkaOutboxProcessor {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OutboxRedisService outboxRedisService;

    @Autowired
    private FailureHandler failureHandler;  // 비동기 호출 클래스

    private static final int MAX_RETRY_COUNT = 5;

    // 일정 주기로 Outbox 메시지 처리
    @Scheduled(fixedRate = 5000)
    public void processOutboxMessages() {
        List<String> messages = outboxRedisService.fetchOutboxMessages();

        for (String message : messages) {
            try {
                // Kafka로 메시지 전송
                kafkaTemplate.send("main-topic", message);

                // 전송 성공 시 Outbox에서 삭제 및 재시도 횟수 초기화
                outboxRedisService.removeMessageFromOutbox(message);
                outboxRedisService.clearRetryCount(message);
            } catch (Exception e) {
                // 실패 시 비동기로 처리
                failureHandler.handleFailureAsync(message);
            }
        }
    }
}
```
- 실패시 비동기로 재처리
- DLQ : 실패한 메시지 처리하기 위한 Redis큐(실패메시지처리, 추적, 재처리)
```java
@Service
public class FailureHandler {

    @Autowired
    private OutboxRedisService outboxRedisService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final int MAX_RETRY_COUNT = 5;

    @Async
    public void handleFailureAsync(String message) {
        String messageId = extractMessageId(message);  // 메시지 ID를 추출하는 로직

        int retryCount = outboxRedisService.getRetryCount(messageId);
        if (retryCount < MAX_RETRY_COUNT) {
            // 재시도 횟수 증가
            outboxRedisService.incrementRetryCount(messageId);
            
            // 메시지를 다시 Kafka로 전송 (재시도)
            kafkaTemplate.send("main-topic", message);
            System.out.println("Retrying message: " + message + " - Retry count: " + retryCount);
        } else {
            // 최대 재시도 횟수 초과 시 DLQ로 메시지 전송
            kafkaTemplate.send("dlq-topic", message);
            outboxRedisService.removeMessageFromOutbox(message);
            outboxRedisService.clearRetryCount(messageId);
            System.out.println("Message sent to DLQ: " + message);
        }
    }

    private String extractMessageId(String message) {
        // 메시지에서 ID를 추출하는 로직
        return message;
    }
}
```

