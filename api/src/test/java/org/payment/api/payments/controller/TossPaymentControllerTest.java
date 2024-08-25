package org.payment.api.payments.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.payment.api.common.util.ObjectConvertUtil;
import org.payment.api.payments.controller.model.PaymentConfirmRequest;
import org.payment.api.payments.controller.model.PaymentConfirmResponse;
import org.payment.api.payments.service.TossPaymentsService;
import org.payment.api.payments.service.model.PaymentServiceConfirmRequestVO;
import org.payment.api.payments.service.model.PaymentServiceConfirmResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@WebFluxTest(TossPaymentsController.class) // TossPaymentsController 클래스만 테스트하도록 설정
public class TossPaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TossPaymentsService tossPaymentsService;

    @Test
    void confirmPayment_success() {
        // Given
        PaymentConfirmRequest requestVO = new PaymentConfirmRequest();
        requestVO.setPaymentKey("1");
        requestVO.setAmount(new BigDecimal("1"));
        requestVO.setOrderId("1");

        PaymentServiceConfirmRequestVO serviceVo = ObjectConvertUtil.copyVO(requestVO, PaymentServiceConfirmRequestVO.class);
        PaymentServiceConfirmResponseVO responseVO = new PaymentServiceConfirmResponseVO();
        PaymentConfirmResponse paymentConfirmResponse = ObjectConvertUtil.copyVO(responseVO, PaymentConfirmResponse.class);

        // tossPaymentsService.sendPaymentConfirmRequest(serviceVo) 호출시 responseVO받아오도록 세팅
        Mockito.when(tossPaymentsService.sendPaymentConfirmRequest(serviceVo))
                .thenReturn(Mono.just(responseVO));

        // When
        webTestClient.post()
                .uri("/api/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestVO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentConfirmResponse.class)
                .isEqualTo(paymentConfirmResponse);

        // Then
        // Service를 호출하였는지 확인
        Mockito.verify(tossPaymentsService).sendPaymentConfirmRequest(serviceVo);
    }

    @Test
    void confirmPayment_failure() {
        // Given
        PaymentConfirmRequest requestVO = new PaymentConfirmRequest();

        //ctossPaymentsService.sendPaymentConfirmRequest(Mockito.any() 호출시 error리턴하게 세팅
        Mockito.when(tossPaymentsService.sendPaymentConfirmRequest(Mockito.any()))
                .thenReturn(Mono.error(new RuntimeException("API Error")));

        // When
        //controller -> Mokito.when이 가로채서 에러세팅 -> service 호출 -> return
        webTestClient.post()
                .uri("/api/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestVO)
                .exchange()
                .expectStatus().is5xxServerError();

        // Then
        // Service를 호출하였는지 확인
        Mockito.verify(tossPaymentsService).sendPaymentConfirmRequest(Mockito.any());
    }
}
