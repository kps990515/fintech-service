package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;
import org.payment.api.payments.model.enumVo.PaymentAccountType;
import org.payment.api.payments.model.enumVo.PaymentRefundStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentVirtualAccount {
    private PaymentAccountType accountType;
    private String accountNumber;
    private String bankCode;
    private LocalDateTime dueDate ;
    private PaymentRefundStatus refundStatus;
    private boolean expired;
    private String settlementStatus;
    private PaymentRefundReceiveAccount refundReceiveAccount;
}
