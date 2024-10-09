## 쿠폰발급 Cache 구조개선

### 개선방향 : Redis와 LocalCache를 동시 사용
- Proxy(대리자) : 기존 메서드 호출을 가로채고, 추가작업(로깅, 트랜잭션, 캐싱 등)을 처리한 후 실제 객체 메서드 호출
- @Cacheable은 Proxy객체를 통해 동작 : 프록시를 통해 메서드 호출 후 캐시 저장
- proxy()를 사용하는 이유 
  - this.getCouponCache를 하게 되면 프록시가 아닌 실제 객체를 호출
  - 이로인해 Proxy가 동작을 안하면서 @Cacheable적용안됨

1. LocalCache에서 먼저 조회
```java
  // localCacheManager에 값있으면 반환
  @Cacheable(cacheNames = "coupon", cacheManager = "localCacheManager") 
  public CouponRedisEntity getCouponLocalCache(long couponId) {
      // 없으면 getCouponCache 실행
      return proxy().getCouponCache(couponId);
  }
```
2. 없으면 Redis 조회
```java
  @Cacheable(cacheNames = "coupon")
  public CouponRedisEntity getCouponCache(long couponId) {
      Coupon coupon = couponIssueService.findCoupon(couponId);
      return new CouponRedisEntity(coupon);
  }
```

3. proxy
- 현재 실행 중인 빈의 프록시 객체을 가져와 @Cacheable의 일관성 유지
- Proxy가 메서드 호출을 가로 채, 추가작업(로컬캐시 확인 후 Redis캐시 확인)을 한뒤 메서드 호출
```java
private CouponCacheService proxy() {
    return ((CouponCacheService) AopContext.currentProxy());
}
```
