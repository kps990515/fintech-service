package org.payment.api.payments.service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionGetRequestVO {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String startingAfter;
    private int limit;
}
