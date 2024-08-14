package org.payment.api.payments.model.enumVo;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PaymentType {
    NORMAL("일반결제"),
    BILLING("자동결제"),
    BRANDPAY("브랜드페이");

    private final String description;
}
