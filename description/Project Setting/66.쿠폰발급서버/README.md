## 쿠폰발급서버

### CouponIssueListener
#### @Scheduled(fixedDelay = 1000)의 문제점
  - 1초 안에 작업이 완료되지 않으면 중복 처리가 발생
  - 멀티 인스턴스 환경 : 각 인스턴스가 동일한 작업을 동시에 실행되서 데이터 중복 처리 또는 경쟁 조건 문제발생
  - 해결책
    1. Schedule Lock 라이브러리 사용 
    ```java
    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(name = "processTask", lockAtLeastFor = "PT1S", lockAtMostFor = "PT5M")
    public void processTask() { 
    ```
    2. Redis에서 작업상태 관리
    - 기본값은 비어있음
    - Redis에서 해당값이 True이면 이미 작업 중으로 판단
    - 작업완료 되면 False로 변경
    ```java
    public void processTask() {
    if (redisTemplate.opsForValue().get("task:lock") != null) {
        return; // 이미 작업이 진행 중인 경우
    }

    // 작업 시작 표시
    redisTemplate.opsForValue().set("task:lock", "true");

    try {
        // 작업 로직
    } finally {
        // 작업 완료 후 상태 해제
        redisTemplate.delete("task:lock");
    }
    ```
    3. 데이터베이스에서 처리된 항목 마킹
```java
@RequiredArgsConstructor
@EnableScheduling
@Component
public class CouponIssueListener {

    private final CouponIssueService couponIssueService;
    private final RedisRepository redisRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String issueRequestQueueKey = getIssueRequestQueueKey();
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    @Scheduled(fixedDelay = 1000)
    public void issue() throws JsonProcessingException {
        log.info("listen...");
        while (existCouponIssueTarget()) {
            CouponIssueRequest target = getIssueTarget();
            log.info("발급 시작 target: " + target);
            couponIssueService.issue(target.couponId(), target.userId());
            log.info("발급 완료 target: " + target);
            removeIssuedTarget();
        }
    }

    private boolean existCouponIssueTarget() {
        return redisRepository.lSize(issueRequestQueueKey) > 0;
    }

    private CouponIssueRequest getIssueTarget() throws JsonProcessingException {
        return objectMapper.readValue(redisRepository.lIndex(issueRequestQueueKey, 0), CouponIssueRequest.class);
    }

    private void removeIssuedTarget() {
        redisRepository.lPop(issueRequestQueueKey);
    }
}
```

### 개선점 : 쿠폰발급 마감시 Cache의 available=false로 바꿔 controller부터 튕겨내게

- 쿠폰발급서버의 couponIssueService.issue
  - 쿠폰발급수량이 마감되었으면 publishCouponEvent(coupon)실행
```java
@Transactional
public void issue(long couponId, long userId) {
    Coupon coupon = findCouponWithLock(couponId);
    coupon.issue();
    saveCouponIssue(couponId, userId);
    publishCouponEvent(coupon);
}
```
- coupon의 availableIssueQuantity를 체크해서 다 발급되었는지 확인
```java
private void publishCouponEvent(Coupon coupon) {
    if (coupon.isIssueComplete()) {
        applicationEventPublisher.publishEvent(new CouponIssueCompleteEvent(coupon.getId()));
    }
}
```

- CouponEventListener
  - putCouponCache -> @CachePut으로 인해 DB조회 -> DB의 avaliableQuantity = false -> 캐시 저장
  - 캐시의 availableIssueQuantity가 false로 업데이트되서 Controller단부터 막히게됨
```java
@RequiredArgsConstructor
@Component
public class CouponEventListener {

    private final CouponCacheService couponCacheService;
    private final Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void issueComplete(CouponIssueCompleteEvent event) {
        log.info("issue complete. cache refresh start couponId: %s".formatted(event.couponId()));
        couponCacheService.putCouponCache(event.couponId());
        couponCacheService.putCouponLocalCache(event.couponId());
        log.info("issue complete cache refresh end couponId: %s".formatted(event.couponId()));
    }
}
```








