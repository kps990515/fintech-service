## 쿠폰발급 LOCK 구조개선

### 기존
- Redis 락을 걸어서 쿠폰 수량제어 / 중복발급 / 저장 / 큐 적재를 진행함
- 락 issue 문제 발생
  - 작업이 3초 안에 끝나지 않으면 락이 자동으로 해제, 다른 요청이 락을 획득해서 두 요청이 동시 쿠폰 요청 가능

```JAVA
@RequiredArgsConstructor
@Service
public class AsyncCouponIssueServiceV1 {

    private final RedisRepository redisRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final DistributeLockExecutor distributeLockExecutor;
    private final CouponCacheService couponCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void issue(long couponId, long userId) {
        CouponRedisEntity coupon = couponCacheService.getCouponCache(couponId);
        coupon.checkIssuableCoupon();
        distributeLockExecutor.execute("lock_%s".formatted(couponId), 3000, 3000, () -> {
            couponIssueRedisService.checkCouponIssueQuantity(coupon, userId);
            issueRequest(couponId, userId);
        });
    }

    private void issueRequest(long couponId, long userId) {
        CouponIssueRequest issueRequest = new CouponIssueRequest(couponId, userId);
        try {
            String value = objectMapper.writeValueAsString(issueRequest);
            redisRepository.sAdd(getIssueRequestKey(couponId), String.valueOf(userId));
            redisRepository.rPush(getIssueRequestQueueKey(), value);
        } catch (JsonProcessingException e) {
            throw new CouponIssueException(FAIL_COUPON_ISSUE_REQUEST, "input: %s".formatted(issueRequest));
        }
    }
}
```

### 개선
- 기존 락을 걸어서 사용했던 로직들을 Redis EVAL Script를 통해 동작시킴
- Redis는 싱글 쓰레드로 원자성을 가지고 있어서 동기화 보장
- Reddison의 RedLock 사용해서 더 견고하게 가능
```java
RLock lock = redissonClient.getLock(lockName);
RLock multiLock = redissonClient.getRedLock(lock);
```

### Redis EVAL : Redis가 싱글스레드이기 때문에 가능
- 원자성 : 스크립트 내의 모든 작업이 단일 트랜잭션으로 실행
    - 락을 따로 설정하지 않아도 락의 역할을 대신
    - 스크립트 내부 로직들은 다른 명령어의 간섭을 받지 않음
    - LUA 스크립트는 하나의 노드에서만 실행됨
      - 복제지연으로 @Cacheable에서 최신데이터 못 가져올 수도 있음
      - 마스터 노드에서만 읽기를 강제해야할 수도 있음
- 성능향상 : 여러 Redis 명령어를 한 번의 네트워크 요청으로 수행가능
- 분산 락 사용 안함 : 락을 필요로 하지 않아 데드락, 락 유실의 문제 사라짐
```java
public void issueRequest(long couponId, long userId, int totalIssueQuantity) {
    String issueRequestKey = getIssueRequestKey(couponId);
    CouponIssueRequest couponIssueRequest = new CouponIssueRequest(couponId, userId);
    try {
        String code = redisTemplate.execute(
                issueScript,
                // KEY목록(1.쿠폰발급 요청 키 2.쿠폰 발급 전달 Queue키)
                List.of(issueRequestKey, issueRequestQueueKey),
                String.valueOf(userId), // ARGV[1]
                String.valueOf(totalIssueQuantity), // ARGV[2]
                objectMapper.writeValueAsString(couponIssueRequest) // ARGV[3]
        );
        CouponIssueRequestCode.checkRequestResult(CouponIssueRequestCode.find(code));
    } catch (JsonProcessingException e) {
        throw new CouponIssueException(FAIL_COUPON_ISSUE_REQUEST, "input: %s".formatted(couponIssueRequest));
    }
}
```

```java
private RedisScript<String> issueRequestScript() {
    String script = """
            // 중복체크 : List에 이미 값이 있으면 중복으로 판단
            if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
                return '2'
            end
                            
            // 수량체크 : 총수량 > 현재수량이면 쿠폰 발급 진행                
            if tonumber(ARGV[2]) > redis.call('SCARD', KEYS[1]) then
                redis.call('SADD', KEYS[1], ARGV[1])
                redis.call('RPUSH', KEYS[2], ARGV[3])
                return '1'
            end
            
            // 총수량 <= 현재수량이면 3 리턴                
            return '3'
            """;
    return RedisScript.of(script, String.class);
```

### 레디스가 클러스터일때 적용할 점
- Redis 해시태그{coupon_%s}를 사용해 여러키를 같은 동일한 노드에 들어갈 수 있게 할 수 있음
```java
public static String getIssueRequestKey(long couponId) {
    // 해시 태그 적용: {couponId}가 동일한 노드에 저장되도록 보장
    return "{coupon_%s}_issue_request_key".formatted(couponId);
}

public static String getIssueRequestQueueKey(long couponId) {
    // 동일한 해시 태그로 큐도 같은 노드에 저장되도록 설정
    return "{coupon_%s}_issue_request_queue".formatted(couponId);
}
```
