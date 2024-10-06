## Saga패턴

### 정의
- MSA에서 분산 트랜잭션들을 통한 전체 트랜잭션의 일관성을 유지하기 위한 패턴
- 특정 트랜잭션 실패 시 다른 성공한 분산 트랜잭션들을 취소(보상 트랜잭션)

### 핵심개념
- 연속된 로컬 트랜잭션
- 보상 트랜잭션
- 조정방식
  1. Choreography(안무 방식) : 서비스들이 직접 이벤트를 주고 받아 SAGA 실행
  2. Orchestration : 중앙 Saga Coordinator가 각 서비스의 트랜잭션 관리, 조정 

- Saga Coordinator
```java
@Service
public class SagaCoordinator {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DeliveryService deliveryService;

    public void executeOrderSaga(OrderRequest orderRequest) {
        try {
            // 1. 주문 생성
            Order order = orderService.createOrder(orderRequest);

            // 2. 결제 처리
            paymentService.processPayment(order);

            // 3. 배송 시작
            deliveryService.startDelivery(order);

            System.out.println("Order saga completed successfully!");
        } catch (Exception e) {
            // 실패 시 보상 작업 (Rollback)
            System.out.println("Saga failed. Starting compensation...");
            compensateOrderSaga(orderRequest);
        }
    }

    private void compensateOrderSaga(OrderRequest orderRequest) {
        // 결제 취소
        paymentService.refundPayment(orderRequest);

        // 주문 취소
        orderService.cancelOrder(orderRequest);

        System.out.println("Compensation completed.");
    }
}
```