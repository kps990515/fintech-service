package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;
import org.payment.api.payments.model.enumVo.PaymentSettlementStatus;

@Getter
@Setter
public class PaymentTransfer {
    private String bankCode;
    private PaymentSettlementStatus settlementStatus;
}
