package org.payment.api.payments.controller.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentConfirmRequest {
    private String paymentKey ;
    private String orderId;
    private BigDecimal amount;
}
