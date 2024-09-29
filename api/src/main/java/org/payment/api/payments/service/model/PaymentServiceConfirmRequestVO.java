package org.payment.api.payments.service.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentServiceConfirmRequestVO {
    private String paymentKey ;
    private String orderId;
    private BigDecimal amount;
    private String email;
}
