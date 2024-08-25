## Exception

### GlobalExceptionHandler
- 전체 컨트롤러 Exception 중앙화
- 각 Exception에 대한 세팅 가능(HTTP Status 등)
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidSessionException.class)
    public ResponseEntity<String> handleInvalidCredentialsException(InvalidSessionException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(ExistUserFoundException.class)
    public ResponseEntity<String> handleExistUserException(ExistUserFoundException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
```

### 예시
```java
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
```