## 스프링 어노테이션

### Main 어노테이션
1. @SpringBootApplication : 아래 3개가 합쳐진 어노테이션
   - @SpringBootConfiguration : 어플리케이션 설정 클래스를 의미
   - @EnableAutoConfiguration: 클래스 경로나 의존성을 바탕으로 적절한 설정을 자동으로 구성
   - @ComponentScan: 해당 클래스의 패키지와 하위 패키지에서 @Component, @Service, @Repository, @Controller 등을 자동으로 스캔하여 빈을 등록
2. @EnableJpaAuditing : JPA Auditing을 활성화(엔티티가 생성/수정시 자동으로 생성자, 생성 시각, 수정자, 수정 시각 등의 필드를 업데이트)
3. @EnableAspectJAutoProxy 
   - AOP활성화, @Aspect가 적용되있는 곳의 메서드 실행전 메서드를 가로채(프록시로 감싸서) 작업 실행(로깅 등)
 ```java
@Aspect
@Component
public class LoggingAspect {

@Before("execution(* com.example.service.*.*(..))")
public void logBeforeMethod(JoinPoint joinPoint) {
System.out.println("Executing method: " + joinPoint.getSignature().getName());
}
```
   - exposeProxy = true 
     - 기본적으로 AOP는 외부에서 메서드를 호출할때만 프록시를 통해 동작
     - exposeProxy = true는 현재 객체의 프록시를 노출해서 자기자신을 호출해도 프록시가 작동하도록 함
     ```java
        @Cacheable(cacheNames = "coupon", cacheManager = "localCacheManager")
        public CouponRedisEntity getCouponLocalCache(long couponId) {
            return proxy().getCouponCache(couponId);
        }
    
        @Cacheable(cacheNames = "coupon")
        public CouponRedisEntity getCouponCache(long couponId) {
            Coupon coupon = couponIssueService.findCoupon(couponId);
            return new CouponRedisEntity(coupon);
        }
     ```

4. @EnableAsync : @Async가 붙은 메서드를 별도의 쓰레드에서 비동기적으로 실행
5. @EnableCaching : 캐싱기능 활성화, @Cacheable 사용가능
6. @ConfigurationPropertiesScan : @ConfigurationProperties로 선언된 클래스를 자동으로 스캔하고 등록
```yaml
myapp:
  api:
    key: abc123
    timeout: 5000
 
```
```java
@ConfigurationProperties(prefix = "myapp.api") // yaml에 있는 값을 읽어와서 넣어줌
public class ApiProperties {
    private String key;
    private int timeout;
    // getters and setters
}
```

### 기타등등
1. @RestController : @Controller + @ResponseBody -> JSON반환
2. @Transactional: 성공하면 트랜잭션 커밋 / 실패하면 롤백
3. @Profile : @Profile("dev")처럼 특정 프로파일에서만 빈을 활성화
4. @EntityListeners(AuditingEntityListener.class)
   - 엔티티가 생성되거나 수정될 때 자동으로 @CreatedDate와 @LastModifiedDate로 지정된 필드에 값을 자동으로 채워줌
    ```java
    @Getter
    @MappedSuperclass
    @EntityListeners(AuditingEntityListener.class)
    public abstract class BaseTimeEntity {
    
        @CreatedDate
        private LocalDateTime dateCreated;
    
        @LastModifiedDate
        private LocalDateTime dateUpdated;
    
    }    
    ```
5. @Enumerated(value = EnumType.STRING) : Entity컬럼에 Enum을 어떤 타입으로 저장할지 지정
```java
@Column(nullable = false)
@Enumerated(value = EnumType.STRING)
private CouponType couponType; 
```

6. @JsonInclude(value = NON_NULL)
- null인 컬럼은 제외하고 return
- Include.NON_EMPTY : "", null, 빈 배열등 제거
```java
@JsonInclude(value = NON_NULL)
public record CouponIssueResponseDto(boolean isSuccess, String comment) {
} 
```

7. @RestControllerAdvice 
   - REST Controller 전역 발생 예외처리
   - 응답 상태 코드 제어가능
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return response;
    }
}
```