package org.payment.api.common.exception;

public class ExistUserFoundException extends RuntimeException {

    public ExistUserFoundException() {
        super("이미 가입된 유저가 존재합니다");
    }

    public ExistUserFoundException(String message) {
        super(message);
    }

    public ExistUserFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExistUserFoundException(Throwable cause) {
        super(cause);
    }
}