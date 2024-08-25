package org.payment.api.payments.service;

import org.payment.api.config.service.TossPaymentsConfig;
import org.payment.api.payments.service.model.PaymentServiceConfirmRequestVO;
import org.payment.api.payments.service.model.PaymentServiceConfirmResponseVO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class TossPaymentsService {

    private final WebClient webClient;
    private final TossPaymentsConfig tossPaymentsConfig;

    @Autowired
    public TossPaymentsService(WebClient.Builder webClientBuilder, TossPaymentsConfig tossPaymentsConfig) {
        this.webClient = webClientBuilder.baseUrl(tossPaymentsConfig.getBaseUrl()).build();
        this.tossPaymentsConfig = tossPaymentsConfig;
    }

    public Mono<PaymentServiceConfirmResponseVO> sendPaymentConfirmRequest(PaymentServiceConfirmRequestVO requestVO) {
        String credentials = tossPaymentsConfig.getSecretKey() + ":"; // Secret key에 ":"를 추가
        String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        //대용량 트래픽을 고려해야한다
        //(TODO)방법들을 찾아서 의견여쭤보기
        //(TODO)실패했을떄의 재처리 방법(retry방식고려 count고려해서 그 뒤에 어캐할건지)
        //(TODO)실패했을떄의 로그저장
        return webClient.post()
                .uri(tossPaymentsConfig.getBaseUrl() + "/v1/payments/confirm")
                .header("Authorization", tossPaymentsConfig.getAuthorizationType() + " " + encodedAuth)
                .header("Content-Type", tossPaymentsConfig.getContentType())
                .bodyValue(requestVO)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), this::handleError)
                .bodyToMono(PaymentServiceConfirmResponseVO.class);
    }


    private Mono<? extends Throwable> handleError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(String.class)
                .flatMap(errorBody -> Mono.error(new RuntimeException("API Error: " + errorBody)));
    }
}
