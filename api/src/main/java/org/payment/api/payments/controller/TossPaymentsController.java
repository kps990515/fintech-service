package org.payment.api.payments.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.payment.api.common.util.ObjectConvertUtil;
import org.payment.api.payments.controller.model.PaymentConfirmRequest;
import org.payment.api.payments.controller.model.PaymentConfirmResponse;
import org.payment.api.payments.model.vo.TransactionRequest;
import org.payment.api.payments.service.model.UserVO;
import org.payment.api.payments.service.pg.PGServiceFactory;
import org.payment.api.payments.service.TossPaymentsService;
import org.payment.api.payments.service.pg.PGAdapter;
import org.payment.api.payments.service.model.PaymentServiceConfirmRequestVO;
import org.payment.api.payments.service.model.TransactionGetRequestVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class TossPaymentsController {

    private final TossPaymentsService tossPaymentsService;
    private final PGServiceFactory PGServiceFactory;

    @PostMapping("/v1/confirm")
    public Mono<ResponseEntity<PaymentConfirmResponse>> confirmPayment(HttpSession httpSession,  @RequestBody PaymentConfirmRequest requestVO) {
        PaymentServiceConfirmRequestVO serviceVo = ObjectConvertUtil.copyVO(requestVO, PaymentServiceConfirmRequestVO.class);

        UserVO userVO = ObjectConvertUtil.copyVO(httpSession.getAttribute("USER_SESSION"), UserVO.class);
        serviceVo.setEmail(userVO.getEmail());

        PGAdapter pgAdapter = PGServiceFactory.getPaymentService(requestVO.getPg());

        return pgAdapter.sendPaymentConfirmRequest(serviceVo)
                .map(response -> {
                    PaymentConfirmResponse paymentConfirmResponse = ObjectConvertUtil.copyVO(response, PaymentConfirmResponse.class);
                    return ResponseEntity.ok().body(paymentConfirmResponse);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(null)));
    }


    @GetMapping("/v1/transactions")
    public Mono<ResponseEntity<PaymentConfirmResponse>> getTransaction(@ModelAttribute TransactionRequest requestVO) {
        TransactionGetRequestVO serviceVo = ObjectConvertUtil.copyVO(requestVO, TransactionGetRequestVO.class);

        PGAdapter pgAdapter = PGServiceFactory.getPaymentService(requestVO.getPg());

        return pgAdapter.getTransaction(serviceVo)
                .map(response -> {
                    PaymentConfirmResponse paymentConfirmResponse = ObjectConvertUtil.copyVO(response, PaymentConfirmResponse.class);
                    return ResponseEntity.ok().body(paymentConfirmResponse);
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(null)));
    }
}


