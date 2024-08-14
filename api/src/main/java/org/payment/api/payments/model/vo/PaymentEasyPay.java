package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentEasyPay {
    private String provider;
    private BigDecimal amount;
    private BigDecimal discountAmount;
}
