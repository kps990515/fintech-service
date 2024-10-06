## PG사 Adapter패턴 적용해보기
- 유연한 확장 가능성 확보

### PGServiceFactory
1. 스프링 컨테이너
   - ApplicationContext 또는 BeanFactory와 같은 컨테이너에서 애플리케이션의 모든 빈을 관리
   - PGAdapter 인터페이스를 구현한 클래스들이 @Component, @Service, @Repository를 가지면 빈으로 등록/관리
2. 타입 기반 의존성 주입
    - List<PGAdapter>는 타입이 PGAdapter이므로, PGAdapter를 구현한 모든 빈들을 자동으로 찾아서 리스트에 주입
3. 자동주입
   - 생성자 주입을 통해 @Component나 @Service로 등록된 모든 PGAdapter 구현체가 스프링 컨테이너에 의해 자동으로 pgAdapterList에 주입


### 코드
1. PGAdapter
```java
public interface PGAdapter {
    Mono<PaymentServiceConfirmResponseVO> sendPaymentConfirmRequest(PaymentServiceConfirmRequestVO requestVO);
    Mono<List<TransactionVO>> getTransaction(TransactionGetRequestVO requestVO);
    boolean isAcceptable(String type);
}
```

2. 각 PG사 클래스에 implements
```java
@Service
public class TossPaymentsService implements PGAdapter

@Override
public boolean isAcceptable(String paymentProvider) {
    return "toss".equalsIgnoreCase(paymentProvider);
}
```

3. PGServiceFactory
- PGAdapter들을 implements받은 클래스들을 모아서 관리
```java
@Component
@RequiredArgsConstructor
public class PGServiceFactory {

    // PGAdapter를 구현한 코든 클래스를 빈으로 등록하고 
    // 생성자 주입방식으로 모든 PGAdapter 구현체를 주입
    private final List<PGAdapter> pgAdapterList;

    public PGAdapter getPaymentService(String paymentProvider) {
        // isAcceptable이 true인 PGAdapter를 찾음
        return pgAdapterList.stream()
                .filter(adapter -> adapter.isAcceptable(paymentProvider)) // 결제사와 일치하는 서비스 찾기
                .findFirst() // 첫 번째로 일치하는 서비스 반환
                .orElseThrow(() -> new IllegalArgumentException("지원되지 않는 결제사: " + paymentProvider)); // 없을 경우 예외 발생
    }
}
```

4. Controller
```java
@PostMapping("/v1/confirm")
public Mono<ResponseEntity<PaymentConfirmResponse>> confirmPayment(@RequestBody PaymentConfirmRequest requestVO) {
    PaymentServiceConfirmRequestVO serviceVo = ObjectConvertUtil.copyVO(requestVO, PaymentServiceConfirmRequestVO.class);

    // 여기서 PGAdapter가 등록된 PG사인지 판별
    PGAdapter pgAdapter = PGServiceFactory.getPaymentService(requestVO.getPG());

    return pgAdapter.sendPaymentConfirmRequest(serviceVo)
            .map(response -> {
                PaymentConfirmResponse paymentConfirmResponse = ObjectConvertUtil.copyVO(response, PaymentConfirmResponse.class);
                return ResponseEntity.ok().body(paymentConfirmResponse);
            })
            .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(null)));
} 
```
