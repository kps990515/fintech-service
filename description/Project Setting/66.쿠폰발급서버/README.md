## 쿠폰발급서버

- CouponIssueListener
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

- couponIssueService.issue
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

- CouponEventListener
  - put이 실행될떄 캐시가 없으면 DB조회해서 새로운 Entity를 만드는데 availble=false로 저장되서
  - 이후로는 Controller부터 막힘
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








