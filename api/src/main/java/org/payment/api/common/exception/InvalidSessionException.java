package org.payment.api.common.exception;

public class InvalidSessionException extends RuntimeException {

    public InvalidSessionException() {
        super("유저정보가 맞지 않습니다");
    }

    public InvalidSessionException(String message) {
        super(message);
    }

    public InvalidSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSessionException(Throwable cause) {
        super(cause);
    }
}
