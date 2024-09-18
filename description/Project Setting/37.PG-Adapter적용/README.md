## PG사 Adapter패턴 적용해보기
- 유연한 확장 가능성 확보

1. PGAdapter
```java
public interface PGAdapter {
    Mono<PaymentServiceConfirmResponseVO> sendPaymentConfirmRequest(PaymentServiceConfirmRequestVO requestVO);
    Mono<List<TransactionVO>> getTransaction(TransactionGetRequestVO requestVO);
}
```

2. 각 PG사 클래스에 implements
```java
@Service
public class TossPaymentsService implements PGAdapter  
```

3. PGServiceFactory
- PGAdapter들을 implements받은 클래스들을 모아서 관리
```java
@Component
public class PGServiceFactory {

    private final Map<String, PGAdapter> paymentServices;

    @Autowired
    public PGServiceFactory(List<PGAdapter> PGServices) { //PGAdapter가 implement된 bean들은 자동으로 주입(toss 등)
        this.paymentServices = PGServices.stream()
                .collect(Collectors.toMap(service -> service.getClass().getSimpleName(), service -> service));
    }

    public PGAdapter getPaymentService(String paymentProvider) {
        return Optional.ofNullable(paymentServices.get(paymentProvider))
                .orElseThrow(() -> new IllegalArgumentException("지원되지 않는 결제사: " + paymentProvider));
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
