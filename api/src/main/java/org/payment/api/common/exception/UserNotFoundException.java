package org.payment.api.common.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("유저정보가 존재하지 않습니다");
    }

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotFoundException(Throwable cause) {
        super(cause);
    }
}