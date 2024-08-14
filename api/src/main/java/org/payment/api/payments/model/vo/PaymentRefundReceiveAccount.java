package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRefundReceiveAccount {
    private String bankCode;
    private String accountNumber;
    private String holderName;
}
