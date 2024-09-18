package org.payment.api.payments.service.pg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
