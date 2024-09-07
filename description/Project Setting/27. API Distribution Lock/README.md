## API 분산락

### 사용이유 : 여러 서버에서 동시에 접근하는 공유 자원에 대해 동시성을 제어

### 원리
- 각 함수에서 Redis를 통해 Lock을 걸고, 해제하는 비동기 방식 사용

### 동기 방식 VS 비동기 방식
1. 동기방식 : RedissonClient
    - 락에 대해 블로킹 방식 사용(락 해제될때까지 스레드 대기)
    - lock.lock(5, TimeUnit.SECONDS) : 5초동안 락을 시도하면 다른 작업 중지
    - Mono.usingWhen을 통해 락을 관리하고 / lock.unlock을 통해 명시적 Lock 해지
  

2. 비동기/논블로킹 방식 : RedissonReactiveClient
    - Lock 설정/해제가 비동기적으로 처리(Lock있어도 다른 작업가능)
    - lock.lock(5, TimeUnit.SECONDS).flatMap(...) : 5초동안 락을 시도하면서 다른 작업 가능
    - lock.unlock().subscribe(): Lock 해제 비동기적으로 처리


### 적용 코드설명
1. RedissonConfig
- API 트랜잭션마다 Redis에서 Lock ID를 설정 후 관리
```java
@Configuration
public class RedissonConfig {

   @Value("${spring.redis.host}")
   private String redisHost;

   @Value("${spring.redis.port}")
   private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        // 단일 노드 설정
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);

        return Redisson.create(config);
    }
} 
```

2. Reactive 방식 락 적용 : RedissonReactiveClient redissonReactiveClient

3. requestVO.getPaymentKey()를 이용해 고유한 락 적용
```java
String lockName = "paymentConfirmLock:" + requestVO.getPaymentKey();
RLockReactive lock = redissonReactiveClient.getLock(lockName); 
```

4. Lock 획득 동안 진행할 Non-blocking 코드
```java
// 비동기적으로 락을 획득하는 동안, 다른 작업을 논블로킹으로 진행
Mono<Void> backgroundTask = Mono.fromRunnable(() -> log.info("비동기 백그라운드 작업 진행 중..."))
    .subscribeOn(Schedulers.parallel()) // 메인 쓰레드와 별개로 병렬 쓰레드풀에서 실행
    .then(); // Mono<Void>반환해 이후 작업에 결합
```

5. Lock 획득 및 획득 후(4번 backgroundTask와 결합되서 실행)
- flatMap() : 락을 성공적으로 획득한 이후 WebClient로 API 호출

```java
// 최대 5초동안 락 획득시도 & 그동안 획득하지 못하면 else문으로 이동
return lock.tryLock(0, 5, TimeUnit.SECONDS)  // 락을 시도하고 최대 5초 대기
        .flatMap(isLocked -> {
            if (isLocked) {
```
```java
else {
    // 락을 획득하지 못한 경우 대체 로직 실행
    log.warn("Lock 획득 실패, 다른 처리 진행");
    return Mono.just(new PaymentServiceConfirmResponseVO());  // 대체 응답
}
```

6. Lock해제
```java
// 작업이 완료되거나 에러가 발생했을 때 무조건 실행
// Reactive방식으로 unlock
.doFinally(signalType -> {
    // 락 해제는 리액티브 방식으로 처리
    lock.unlock().subscribe();
})
```


### 한계점
1. 불완전한 Lock
   - 네트워크, Redis 장애시 락이 해제되지 않아 DeadLock 발생
2. 단일 Redis 장애
    - Redis가 단일서버일 경우 전체 시스템에 영향을 미침

### API 분산락 종류
1. Redis/Redisson : Redis를 기반으로 한 다양한 락 기능을 제공(인메모리 DB)
    - 장점 : 빠른성능, 간편한구현
    - 단점 : Redis 비동기적 데이터 복제로 정보유실 가능 / Redis를 관리할 클러스터 필요

2. ZooKeeper :분산 시스템을 위한 코디네이션 서비스로, 안정적인 분산 락을 제공
    - 장점 : Sequential 락을 통해 순서, 일관성 보장 / 안정성
    - 단점 : Redis에 비해 느린 성능, 복잡성

3. Etcd : Kubernetes에 적합한 높은 일관성, 신뢰성 제공
    - 장점 : Raft알고리즘으로 모든 노드에서 데이터 일관성 제공 / 클라우드 환경 적합
    - 단점 : 제일 느림, 복잡한 설정

