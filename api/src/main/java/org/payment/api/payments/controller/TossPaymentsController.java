package org.payment.api.payments.controller;

import lombok.RequiredArgsConstructor;
import org.payment.api.common.ObjectConvertUtil;
import org.payment.api.payments.controller.model.PaymentConfirmRequest;
import org.payment.api.payments.controller.model.PaymentConfirmResponse;
import org.payment.api.payments.service.TossPaymentsService;
import org.payment.api.payments.service.model.PaymentServiceConfirmRequestVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class TossPaymentsController {

    private final TossPaymentsService tossPaymentsService;

    @PostMapping("/v1/confirm")
    public Mono<ResponseEntity<PaymentConfirmResponse>> confirmPayment(@RequestBody PaymentConfirmRequest requestVO) {
        PaymentServiceConfirmRequestVO serviceVo = ObjectConvertUtil.copyVO(requestVO, PaymentServiceConfirmRequestVO.class);

        return tossPaymentsService.sendPaymentConfirmRequest(serviceVo)
                .map(response -> {
                    PaymentConfirmResponse paymentConfirmResponse = ObjectConvertUtil.copyVO(response, PaymentConfirmResponse.class);
                    return ResponseEntity.ok().body(paymentConfirmResponse);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(null)));
    }
}


