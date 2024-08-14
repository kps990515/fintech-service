package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentFailure {
    private String code;
    private String message;
}
