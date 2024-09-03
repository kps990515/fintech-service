package org.payment.api.payments.service.model;

import lombok.Getter;
import lombok.Setter;
import org.payment.api.payments.model.enumVo.PaymentMethod;
import org.payment.api.payments.model.enumVo.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionVO {
    private String mId;
    private String transactionKey;
    private String paymentKey;
    private String orderId;
    private PaymentMethod method;
    private String customerKey;
    private boolean useEscrow;
    private String receiptUrl;
    private PaymentStatus status;
    private LocalDateTime transactionAt;
    private String currency;
    private BigDecimal amount;
}
