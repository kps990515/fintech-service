package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentCancel {
    private BigDecimal cancelAmount;
    private String cancelReason;
    private BigDecimal taxFreeAmount;
    private BigDecimal taxExemptionAmount;
    private BigDecimal refundableAmount;
    private BigDecimal easyPayDiscountAmount;
    private LocalDateTime canceledAt;
    private String transactionKey;
    private String receiptKey;
    private String cancelStatus;
    private String cancelRequestId;
}
