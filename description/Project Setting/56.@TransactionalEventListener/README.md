## @TransactionalEventListener

### 정의
- 트랜잭션이 성공적으로 커밋된 후 이벤트를 처리하고 싶을 때 사용

### 주요기능
- 트랜잭션과 이벤트 연계: 이벤트 처리 시점을 트랜잭션 상태에 맞춰 조정할 수 있습니다.
- 트랜잭션 커밋 후에 실행: 트랜잭션이 성공적으로 완료된 후에만 이벤트를 처리하도록 기본 동작
- 다양한 단계에서 실행 가능: phase 속성을 사용하여 이벤트를 처리할 시점을 커밋 전 또는 롤백 시점으로 설정 가능
  - AFTER_COMMIT(기본값), AFTER_ROLLBACK, AFTER_COMPLETION, BEFORE_COMMIT

### 사용법
- service
```java
@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void createOrder(String orderId) {
        // 주문 생성 로직
        System.out.println("Order created with ID: " + orderId);

        // 이벤트 발행
        eventPublisher.publishEvent(new OrderCreatedEvent(orderId));
    }
}
```
- 이벤트 리스너
```java
@Service
public class NotificationService {

    @TransactionalEventListener  // 트랜잭션 커밋 후에 실행
    public void handleOrderCreated(OrderCreatedEvent event) {
        System.out.println("Sending notification for order ID: " + event.getOrderId());
        // 트랜잭션 커밋 후에 알림 전송 로직
    }
}
```
- 이벤트 처리 시점 제어
```java
@Service
public class RollbackNotificationService {

    @TransactionalEventListener(phase = TransactionalEventListener.Phase.AFTER_ROLLBACK)
    public void handleOrderRollback(OrderCreatedEvent event) {
        System.out.println("Transaction rolled back for order ID: " + event.getOrderId());
        // 롤백 후의 처리 로직
    }
}
```

### Kafka와의 연동
1. Kafka 메시지를 트랜잭션 커밋 후에 발행하는 예시
```java
@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderService(ApplicationEventPublisher eventPublisher, KafkaTemplate<String, String> kafkaTemplate) {
        this.eventPublisher = eventPublisher;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void createOrder(String orderId) {
        // 1. 주문 생성 로직 (DB에 저장 등)
        System.out.println("Order created with ID: " + orderId);
        
        // 2. 이벤트 발행 (트랜잭션이 성공적으로 커밋된 후에 처리)
        eventPublisher.publishEvent(new OrderCreatedEvent(orderId));
    }

    @TransactionalEventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 트랜잭션이 성공적으로 커밋된 후에 Kafka 메시지 발행
        kafkaTemplate.send("order-topic", event.getOrderId());
        System.out.println("Kafka message sent for order ID: " + event.getOrderId());
    }
}
```

2. Kafka 메시지를 수신한 후 트랜잭션 커밋 시점에 작업 수행
```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class OrderProcessingService {

    @KafkaListener(topics = "order-topic", groupId = "order-group")
    @Transactional
    public void processOrder(String orderId) {
        // 1. Kafka로부터 수신한 주문 처리 로직 (DB 작업 등)
        System.out.println("Processing order with ID: " + orderId);

        // 2. 트랜잭션 커밋 후 처리할 이벤트 발행
        eventPublisher.publishEvent(new OrderProcessedEvent(orderId));
    }

    @TransactionalEventListener
    public void handleOrderProcessed(OrderProcessedEvent event) {
        // 트랜잭션이 커밋된 후 실행되는 로직
        System.out.println("Transaction committed for order ID: " + event.getOrderId());
    }
}

```