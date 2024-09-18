## 어댑터 패턴
- 호환성 없는 인터페이스 때문에 함께 동작할 수 없는 클래스들을 함께 작동해주도록 변환해주는 패턴
  - AS-IS : 기존시스템 -> 업체제공 클래스
  - TO-BE : 기존시스템 -> Adapter -> 여러 업체 제공 클래스들
  
### 예제
- Adapter
```java
// 공통 인터페이스 (Adapter 역할)
public interface CreditCardPaymentAdapter {
    Long processCreditPayment(Long amountKRW, String creditCardNumber);
}

// Adapter: 특정 PG사의 결제 처리 로직 (EasyPay)
@Service
public class EasyCreditCardPaymentAdapter implements CreditCardPaymentAdapter {
    @Override
    public Long processCreditPayment(Long amountKRW, String creditCardNumber) {
        // EasyPay API 연동 코드
        System.out.println("Processing EasyPay payment...");
        return amountKRW * 100L;  // 결제 승인 번호 (예시)
    }
}

// Adapter: 또 다른 PG사의 결제 처리 로직 (TossPay)
@Service
public class TossCreditCardPaymentAdapter implements CreditCardPaymentAdapter {
    @Override
    public Long processCreditPayment(Long amountKRW, String creditCardNumber) {
        // TossPay API 연동 코드
        System.out.println("Processing TossPay payment...");
        return amountKRW * 200L;  // 결제 승인 번호 (예시)
    }
}
```
- Service
  - CreditCardPaymentAdapter 인터페이스를 받아서 PG사 신경안쓰고 처리
```java
// 클라이언트 코드 (Adapter를 이용하여 결제 처리)
@Service
public class PaymentService {

  // PG사별로 어떤 Adapter를 사용할지 선택하는 로직
  public Long processPayment(Long amountKRW, String creditCardNumber, CreditCardPaymentAdapter adapter) {
    return adapter.processCreditPayment(amountKRW, creditCardNumber);
  }
}
```
