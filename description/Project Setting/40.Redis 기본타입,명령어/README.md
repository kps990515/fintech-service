## Redis공부

### Redis 라이브러리
1. Jedis : 동기식 API, 간단한 구조, 싱글스레드, non Thread safe
2. Lettus : 비동기 지원, 멀티스레드 지원, Netty 지원, 리액티브 지원

### 데이터타입
1. String(기본적으로 원래쓰던 get, set)
2. List
3. Sets
4. ZSET : sortedSet(Score로 정렬 가능)
5. Hashes : Field:Value 형태로 여러개가 존재(RDB Table개념으로 사용)

### 명령어
1. ZADD : 입력
2. ZCARD : COUNT
3. ZRANGE : 정렬조회(score오름차순)
4. ZRANGEBYSCORE : SCORE 함께 조회
5. ZREM : 삭제
6. ZSCORE : 특정 member의 Score조회
7. ZRANK : 특정 member의 rank를 조회

### 사용법
```shell
ubuntu@ip-172-31-11-234:~$ redis-cli info | grep role
role:master
ubuntu@ip-172-31-11-234:~$ redis-cli
## ZADD
127.0.0.1:6379> zadd rediszsettest 12000 p0001
(integer) 1
127.0.0.1:6379> zcard rediszsettest
(integer) 1
127.0.0.1:6379> zadd rediszsettest 13000 p0002
(integer) 1
127.0.0.1:6379> zadd rediszsettest 13500 p0003
(integer) 1
127.0.0.1:6379> zadd rediszsettest 8500 p0004
(integer) 1
127.0.0.1:6379> zadd rediszsettest 100000 p0005
(integer) 1

## ZRANGE(0~2)
127.0.0.1:6379> zrange rediszsettest 0 2
1) "p0004"
2) "p0001"
3) "p0002"

## ZRANGE WITH SCORES
127.0.0.1:6379> zrange rediszsettest 0 2 withscores
1) "p0004"
2) "8500"
3) "p0001"
4) "12000"
5) "p0002"
6) "13000"

## ZRANGEBYSCORE WITHSCORES
127.0.0.1:6379> ZRANGEBYSCORE rediszsettest 50000 2000000 WITHSCORES
1) "p0005"
2) "100000"

## ZSCORE
127.0.0.1:6379> ZSCORE rediszsettest p0003
"13500"

## ZRANK
127.0.0.1:6379> Zrank rediszsettest p0003
(integer) 2
```