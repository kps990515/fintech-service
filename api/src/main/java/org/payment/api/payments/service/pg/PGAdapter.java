package org.payment.api.payments.service.pg;

import org.payment.api.payments.service.model.PaymentServiceConfirmRequestVO;
import org.payment.api.payments.service.model.PaymentServiceConfirmResponseVO;
import org.payment.api.payments.service.model.TransactionGetRequestVO;
import org.payment.api.payments.service.model.TransactionVO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PGAdapter {
    Mono<PaymentServiceConfirmResponseVO> sendPaymentConfirmRequest(PaymentServiceConfirmRequestVO requestVO);
    Mono<List<TransactionVO>> getTransaction(TransactionGetRequestVO requestVO);
}
