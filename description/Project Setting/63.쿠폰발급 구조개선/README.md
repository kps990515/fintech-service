## 쿠폰발급 구조개선
- 기존 : Client -> Api Server -> DB
    - 요청이 많아질수록 결국 DB 트랜잭션 폭주

### 구조개선 방식
- 쿠폰요청 트래픽과 쿠폰발급 트랜잭션 분리
    - Redis를 통한 트래픽 대응
    - DB 트래픽제어
- 비동기 쿠폰 발급
    - Queue를 인터페이스로 발급요청 / 발급처리를 분리
- 쿠폰 요청 API 서버 / 쿠폰 발급 서버를 분리

### 처리순서
1. 쿠폰 요청 처리
    1. 사용자 요청은 API 서버가 수신
    2. API 서버는 쿠폰 발급요청을 Redis에 전달
    3. Redis -> Queue에 쿠폰발급 대상 저장
2. 쿠폰 발급 처리
    1. 쿠폰 발급 SERVER를 분리, Queue에서 쿠폰발급대상 Pull
    2. 쿠폰발급 SERVER에서 쿠폰발급 트랜잭션 진행


### 시나리오 1(ZSET)
1. 유저 요청
2. 쿠폰 캐시를 통한 유효성 검증(쿠폰 존재, 발급 시작시간 등)
3. ZSET에 요청 추가(SCORE=TIMESTAMP)
   #### 여기서 문제 발생
    - Sort하는 과정에서 같은 timestamp일때 순서가 바뀔수 있음
    - redis반영타임과 신청시간의 순서가 동일할거란 보장이 없음
    - ZADD : O(log n)의 시간복잡도도 발생
    - 결론 : 발급성공 응답을 줬는데 반영시간에 따른 재 SORT로 인해 발급순서가 바뀔 수 있음

4. ZADD 응답 기반 중복 발급 검증
5. ZRANK를 통해 요청 순서 조회 및 발급 성공 응답
6. 발급 성공 시 발급 QUEUE에 저장

### 시나리오 2(SET)
1. 유저 요청
2. 쿠폰 캐시를 통한 유효성 검증(쿠폰 존재, 발급 시작시간 등)
#### 동시성 처리
```java
public void issue(long couponId, long userId) {
    CouponRedisEntity coupon = couponCacheService.getCouponCache(couponId);
    coupon.checkIssuableCoupon();
    distributeLockExecutor.execute("lock_%s".formatted(couponId), 3000, 3000, () -> {
        couponIssueRedisService.checkCouponIssueQuantity(coupon, userId);
        issueRequest(couponId, userId);
    });
}
```
3. 수량 조회(SCARD) 및 발급 가능 여부 검증
```java
public void checkCouponIssueQuantity(CouponRedisEntity coupon, long userId) {
    if (!availableUserIssueQuantity(coupon.id(), userId)) {
        throw new CouponIssueException(DUPLICATED_COUPON_ISSUE, "발급 가능한 수량을 초과합니다. couponId : %s, userId: %s".formatted(coupon.id(), userId));
    }
    if (!availableTotalIssueQuantity(coupon.totalQuantity(), coupon.id())) {
        throw new CouponIssueException(INVALID_COUPON_ISSUE_QUANTITY, "발급 가능한 수량을 초과합니다. couponId : %s, userId : %s".formatted(coupon.id(), userId));
    }
}
```
```java
public boolean availableTotalIssueQuantity(Integer totalQuantity, long couponId) {
    if (totalQuantity == null) {
        return true;
    }
    String key = getIssueRequestKey(couponId);
    return totalQuantity > redisRepository.sCard(key);
}
```
4. 요청추가 및 중복요청 확인(SADD)
5. 발급 성공 시 발급 QUEUE에 저장
```java
private void issueRequest(long couponId, long userId) {
    CouponIssueRequest issueRequest = new CouponIssueRequest(couponId, userId);
    try {
        String value = objectMapper.writeValueAsString(issueRequest);
        // 요청이 개인당 1개인지 확인, 발급수량 관리
        redisRepository.sAdd(getIssueRequestKey(couponId), String.valueOf(userId));
        // 쿠폰 발급 큐에 넣을 데이터
        redisRepository.rPush(getIssueRequestQueueKey(), value);
    } catch (JsonProcessingException e) {
        throw new CouponIssueException(FAIL_COUPON_ISSUE_REQUEST, "input: %s".formatted(issueRequest));
    }
}
```
