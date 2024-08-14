package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;
import org.payment.api.payments.model.enumVo.PaymentCashReceiptIssueStatus;
import org.payment.api.payments.model.enumVo.PaymentCashReceiptTransactionType;
import org.payment.api.payments.model.enumVo.PaymentCashReceiptType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentCashReceipt {
    private String receiptKey;
    private String orderId;
    private String orderName;
    private PaymentCashReceiptType type;
    private String issueNumber;
    private String receiptUrl;
    private String businessNumber;
    private PaymentCashReceiptTransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal taxFreeAmount;
    private PaymentCashReceiptIssueStatus issueStatus;
    private PaymentFailure failure;
    private String customerIdentityNumber;
    private LocalDateTime requestedAt;
}
