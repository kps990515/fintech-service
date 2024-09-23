## RedisTest

### Redis benchmark
- 명령어
```shell
redis-benchmark
```
- option : Client, RequestCount, DataSize, Key Range 등
```shell
redis-benchmark -n 50000 -d 1000000 -t hset -r 5000 -c 500
## n : 요청횟수 / d : 요청 시 데이터크기 / t : 테스트할 타입 / r: 키 랜덤범위 / c: 클라이언트 수
```

## Redis 장애Case
### 1. Master, Slave간 역할 변경 되었을때 Client 인식불가
- 기본적으로 Master는 write위주 / Slave는 read위주
- 특정이유로 서로의 역할이 바뀜
- Client가 바뀐 정보를 못 읽고 Master에서 Slave로 바뀐 redis에 wirte를 요청
- READONLY You Can't write againat a read only replica 에러 발생

### 1. 해결법
- Client에서 접근하는 Master Redis의 도메인 라우팅을 변경(기존 -> 신규)
- Cluster환경에서는 Client의 Meta정보 변경(Lettuce : ClusterTopologyRefreshOptions)


### 2. Full sync 실패
- 동작원리
  1. Master-Slave sync 중단
  2. 부분 동기화 시도(Partial Sync)
  3. 전체 동기화 시도(full Sync) -> 이때 RDB에 데이터 요청
  4. Master는 BGSAVE를 통해 rdb 생성
  5. rdb를 Slave에 전달
  6. Slave는 rdb File 처리
- 5번에서 Rdb데이터가 클 시 **Client-output-buffter-limit에 걸림
  - slave 256Mb 64Mb 60 : 한번에 256M가 오거나 64M이상이 60초 이상 메모리에 머물시 Transaction 중단

### 2. 해결법 : Client-output-buffter-limit 수정

### 3. Client쪽 수신불가로인한 Buffer증가, 데이터 삭제
- 시나리오
  1. 데이터요청
  2. Redis에서 Data return
  3. Client에서 inbound가 막힘, timeout 발생
  4. return안된 Data가 Memory 상 임시보관됨
  5. 임시보관 Data가 누적되면서 Redis 공간차지
  6. Memory정책에 따라 write가 불가해지거나, 지워지는 현상 발생

### 3. 해결법 : client-output-buffer-limit Normal 세팅
- normal : Client에 대한 설정(slave, pusub 이렇게 3가지 있음)
```shell
client-output-buffer-limit normal 256mb 64mb 60
## 256mb : hard-limit(256mb 초과하면 Client와의 연결 중단)
## 64mb : soft-limit(60초 동안 64mb이상 Client가 사용하면 연결 중단)
```

### 4. 그 외
- Client 무한 증가 : timeout설정해도 idle Connection으로 인식되지않아 close안되는 현상 발생
  - tcp임의 kill 필요
- AOF 쓰기 작업 : 너무 빈번하게 발생할경우 서비스 부하 발생 / 오래걸리는 경우 대량쓰기시 full sync 안하도록 설정필요
- KEYS, HGETALL 등 과도한 요청 : 해당 요청들을 실행되지 않게 미리 세팅

### 장애방지 세팅
1. slowlog-log-slower-than 세팅
```shell
config get slowlog-low-slower-than 1000000
```
2. slowlog-max-len 세팅
3. client-output-buffer-limit 세팅(normal, pubsub, slave)
4. redis insight로 모니터링

