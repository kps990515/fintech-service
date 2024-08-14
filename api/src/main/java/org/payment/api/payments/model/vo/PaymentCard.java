package org.payment.api.payments.model.vo;

import lombok.Getter;
import lombok.Setter;
import org.payment.api.payments.model.enumVo.PaymentCardAcquireStatus;
import org.payment.api.payments.model.enumVo.PaymentCardInterestPayer;
import org.payment.api.payments.model.enumVo.PaymentCardType;
import org.payment.api.payments.model.enumVo.PaymentOwnerType;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentCard {
    private BigDecimal amount;  
    private String issuerCode;  
    private String acquirerCode; 
    private String number;  
    private BigDecimal installmentPlanMonths;
    private String approveNo;  
    private boolean useCardPoBigDecimal; 
    private PaymentCardType cardType;
    private PaymentOwnerType ownerType;
    private PaymentCardAcquireStatus acquireStatus;
    private boolean isBigDecimalerestFree;  
    private PaymentCardInterestPayer interestPayer;
}
