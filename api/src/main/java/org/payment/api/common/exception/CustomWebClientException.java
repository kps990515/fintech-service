package org.payment.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

// 커스텀 예외 생성
@Getter
public class CustomWebClientException extends RuntimeException {
    private final HttpStatus status;
    private final String responseBody;

    public CustomWebClientException(HttpStatus status, String responseBody) {
        super("WebClient 에러 발생: " + status);
        this.status = status;
        this.responseBody = responseBody;
    }
}
