## 동기화 문제 해결

### Java
1. Synchronized
    - 해당 블록 내부 로직은 동시 접근 불가
    - 메서드, 블록 단위로 동작
    - 락 획득 -> 락 해제 방식으로 동작
    - 문제점 : DB트랜잭션과는 별개, 멀티서버에서는 적용안됨
```java
    @Transactional
    public void issue(long couponId, long userId) {
        synchronized(this) {
            Coupon coupon = findCouponWithLock(couponId);
            coupon.issue();
            saveCouponIssue(couponId, userId);
        }
    }
```
- 문제점 : synchronized 블록은 자바 레벨에서 동작, 데이터베이스의 트랜잭션과는 별개
    - Transaction 커밋전에 Lock이 반납되어 두번째 API가 findCouponWithLock 실행시킴 

### Redis 락
- config
```java
@RequiredArgsConstructor
@Component
public class DistributeLockExecutor {

    private final RedissonClient redissonClient;
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    public void execute(String lockName, long waitMilliSecond, long leaseMilliSecond, Runnable logic) {
        RLock lock = redissonClient.getLock(lockName); // 락 이름으로 락을 가져옴
        try {
            // 기다릴시간, 락 놓아줄 시간 설정
            boolean isLocked = lock.tryLock(waitMilliSecond, leaseMilliSecond, TimeUnit.MILLISECONDS);
            if (!isLocked) {
                throw new IllegalStateException("[" + lockName + "] lock 획득 실패");
            }
            logic.run();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```
- controller
```java
@PostMapping("/v1/issue")
public CouponIssueResponseDto issueV1(@RequestBody CouponIssueRequestDto body) {
    distributeLockExecutor.execute("lock_" + body.couponId(), 1000, 1000, () -> {
        couponIssueRequestService.issueRequestV1(body);
    });
    return new CouponIssueResponseDto(true, null);
}
```

### MYSQL
- for update : 비관적락 중 쓰기락 적용(commmit될떄까지 Lock)
```java
select * from COUPON where id = 1 for update
```
- QueryDSL
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Coupon c WHERE c.id = :id")
Optional<Coupon> findCouponWithLock(long id); 
```

### JPA
#### 비관적 락
- 다른 트랜잭션이 동시 접근 못하도록 락을 거는 방식
- 장점 : 동시성 충돌방지 
- 단점 : 데드락, 트랜잭션 대기 
1. PESSIMISTIC_READ : 읽기 전용 락(데이터 수정 불가)
2. PESSIMISTIC_WRITE : 쓰기 전용 락(데이터 읽기, 수정 불가)
3. PESSIMISTIC_FORCE_INCREMENT : 비관적 락으로서 트랜잭션 완료시 강제로 값 증가

#### 긍정적 락
- 데이터 동시수정가능성을 낮게보고, 트랜잭션이 끝날때 데이터 변경 여부를 확인
- 데이터 변경이 감지되면 롤백
- 장점 : 비관적락보다 성능이 좋음, 자원 절약, 데드락 회피 가능
- 단점 : 충돌발생시 롤백하므로 비효율적
1. OPTIMISTIC : 트랜잭션이 완료될때, 읽었을때와 비교해 데이터 변경을 감지
2. OPTIMISTIC_FORCE_INCREMENT : 데이터가 변경되지 않더라도 강제로 값 증가


