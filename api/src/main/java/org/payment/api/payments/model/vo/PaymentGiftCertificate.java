package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;
import org.payment.api.payments.model.enumVo.PaymentSettlementStatus;

@Getter
@Setter
public class PaymentGiftCertificate {
    private String approveNo;
    private PaymentSettlementStatus settlementStatus;
}
