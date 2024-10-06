package org.payment.api.payments.service.pg;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PGServiceFactory {

    private final List<PGAdapter> pgAdapterList;

    public PGAdapter getPaymentService(String paymentProvider) {
        // isAcceptable이 true인 PGAdapter를 찾음
        return pgAdapterList.stream()
                .filter(adapter -> adapter.isAcceptable(paymentProvider)) // 결제사와 일치하는 서비스 찾기
                .findFirst() // 첫 번째로 일치하는 서비스 반환
                .orElseThrow(() -> new IllegalArgumentException("지원되지 않는 결제사: " + paymentProvider)); // 없을 경우 예외 발생
    }
}
