package exception;

import lombok.Getter;
import org.payment.db.user.UserEntity;

@Getter
public class EmailRetryableException extends RuntimeException {
    private final UserEntity user;

    public EmailRetryableException(UserEntity user, Throwable cause) {
        super(cause);
        this.user = user;
    }
}
