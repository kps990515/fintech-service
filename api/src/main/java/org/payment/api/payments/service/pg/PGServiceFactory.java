package org.payment.api.payments.service.pg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PGServiceFactory {

    private final Map<String, PGAdapter> paymentServices;

    @Autowired
    public PGServiceFactory(List<PGAdapter> PGServices) {
        this.paymentServices = PGServices.stream().collect(Collectors.toMap(
                        this::getQualifierValue,  // Qualifier 값을 키로 사용
                        service -> service
                ));
    }

    private String getQualifierValue(PGAdapter service) {
        Qualifier qualifier = service.getClass().getAnnotation(Qualifier.class);
        if (qualifier != null) {
            return qualifier.value(); // @Qualifier의 값을 반환
        } else {
            throw new IllegalArgumentException("PGAdapter에 @Qualifier가 설정되지 않았습니다.");
        }
    }

    public PGAdapter getPaymentService(String paymentProvider) {
        return Optional.ofNullable(paymentServices.get(paymentProvider))
                .orElseThrow(() -> new IllegalArgumentException("지원되지 않는 결제사: " + paymentProvider));
    }

}
