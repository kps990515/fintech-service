package org.payment.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentConfirmedEvent {
    private final String paymentKey;
    private final String email;
}
