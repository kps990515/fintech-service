package org.payment.api.payments.model.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionRequest {
    // LocalDateTime은 객체라 @NotEmpty를 사용할 필요없음
    @NotNull(message = "startDate는 필수 값")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @NotNull(message = "endDate 필수 값")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    private String startingAfter;
    private int limit = 100; // 기본값 설정
}
