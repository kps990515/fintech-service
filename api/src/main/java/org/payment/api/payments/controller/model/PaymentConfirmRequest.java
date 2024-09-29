package org.payment.api.payments.controller.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentConfirmRequest {
    private String paymentKey ;
    private String orderId;
    private BigDecimal amount;
    private String pg;
}
