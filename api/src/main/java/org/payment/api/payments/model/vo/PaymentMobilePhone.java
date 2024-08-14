package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;
import org.payment.api.payments.model.enumVo.PaymentSettlementStatus;

@Getter
@Setter
public class PaymentMobilePhone {
    private String customerMobilePhone;
    private PaymentSettlementStatus settlementStatus;
    private String receiptUrl;
}
