## 타입 기반 DI

### 정의
- 의존성 주입 시 타입을 기준으로 빈을 주입하는 방식
- 스프링은 컨텍스트에서 같은 타입의 빈을 찾아 주입

### 장점
- 인터페이스의 여러 구현체를 자동 주입
- 새로운 구현체가 생겨나도 자동 주입
- 다형성

### 작동원리
- 타입 기반 자동 주입
- List<PGAdapter> pgAdapterList처럼 인터페이스 타입을 기반으로 주입을 요구하는 경우 해당 인터페이스를 구현한 모든 클래스를 자동으로 주입
- 주입되는 클래스들은 스프링의 @Component, @Service, @Repository 같은 어노테이션을 통해 빈으로 등록되어 있어야 함

### 사용 범위
1. 다중 구현체 처리(인터페이스, 추상클래스 등)
```java
 public interface PaymentService {
  void processPayment(PaymentRequest request);
}

@Service
public class TossPaymentService implements PaymentService {
  @Override
  public void processPayment(PaymentRequest request) {
    System.out.println("Processing payment with Toss");
    // Toss 결제 로직
  }
}

@Service
public class PaypalPaymentService implements PaymentService {
  @Override
  public void processPayment(PaymentRequest request) {
    System.out.println("Processing payment with PayPal");
    // PayPal 결제 로직
  }
}
```
```java
@Service
public class PaymentProcessor {

    private final PaymentService tossPaymentService;
    private final PaymentService paypalPaymentService;

    @Autowired
    public PaymentProcessor(@Qualifier("tossPaymentService") PaymentService tossPaymentService,
                            @Qualifier("paypalPaymentService") PaymentService paypalPaymentService) {
        this.tossPaymentService = tossPaymentService;
        this.paypalPaymentService = paypalPaymentService;
    }
```
2. 컬렉션 주입 
- List : private final List<PaymentService> paymentServices;
- Map : private Map<String, PGAdapter> pgAdapterMap
- @Qualifier : @Qualifier("tossPaymentService")

3. 생성자 주입
- @Autowired, @Inject, @Resource
