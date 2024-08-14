package org.payment.api.payments.controller.model;

import lombok.Getter;
import lombok.Setter;
import org.payment.api.payments.model.enumVo.PaymentMethod;
import org.payment.api.payments.model.enumVo.PaymentStatus;
import org.payment.api.payments.model.enumVo.PaymentType;
import org.payment.api.payments.model.vo.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PaymentConfirmResponse {
    private String version;
    private String paymentKey;
    private PaymentType type;
    private String orderId;
    private String mId; 
    private String orderName;
    private String currency;
    private PaymentMethod method;
    private BigDecimal totalAmount;
    private BigDecimal balanceAmount;
    private PaymentStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private boolean useEscrow;
    private String lastTransactionKey;
    private BigDecimal suppliedAmount;
    private BigDecimal vat;
    private boolean cultureExpense;
    private BigDecimal taxFreeAmount;
    private BigDecimal taxExemptionAmount;
    private List<PaymentCancel> cancels;
    private boolean isPartialCancelable;
    private PaymentCard card;
    private PaymentVirtualAccount virtualAccount;
    private String secret;
    private PaymentMobilePhone mobilePhone;
    private PaymentGiftCertificate giftCertificate;
    private PaymentTransfer transfer;
    private PaymentReceipt receipt;
    private PaymentCheckout checkout;
    private PaymentEasyPay easyPay;
    private String country;
    private PaymentFailure failure;
    private PaymentCashReceipt cashReceipt;
    private List<PaymentCashReceipt> cashReceipts;
    private PaymentDiscount discount;
    private BigDecimal easyPayAmount;
    private BigDecimal easyPayCancelAmount;
}
