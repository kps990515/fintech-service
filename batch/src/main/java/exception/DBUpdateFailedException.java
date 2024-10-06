package exception;

import lombok.Getter;

@Getter
public class DBUpdateFailedException extends RuntimeException {
    private final String user;

    public DBUpdateFailedException(String user, Throwable cause) {
        super(cause);
        this.user = user;
    }
}
