## Redis

### In-memory-db 정의
- 디스크대신 메모리(RAM)상에 저장되는 DB
- 접근이 빠르지만, 서버 재시작/장애가 발생 시 메모리 휘발 가능

### Redis 정의
- 오픈소스 인메모리DB 
- 캐싱, 세션관리, 실시간분석, 메시지큐로 사용

### Redis 백업방안
1. RDB(Redis Database)스냅샷
   - 일정 주기마다 데이터를 디스크에 저장(적재 키 숫자 도달, 시간 등)
   - 장점 : 메모리 사용량 적음 / 전송이 쉬움
   - 단점 : 스냅샷 찍기 전에 날라가면 데이터 소실  


2. AOF(Append-Only File)
   - 모든 쓰기 명령을 디스크에 순차적으로 기록 & 압축
   - 장점 : 데이터 손실 최소화
   - 단점 : RDB보다 파일 크기가 큼 / 재로딩 시간 김 / 기록 시 부하발생

3. 혼합사용
   - 서버 정상 종료/재시작 상황 : AOF로 재로딩
   - 서버 비정상 종료/재시작 상황 
     - RDB스냅샷으로 먼저 빠르게 복구
     - AOF 손상 가능성을 대비해 RDB스냅샷과 싱크로나이징
     - 나머지 부분은 AOF로 복구

### 내가 사용할 방법
1. 로그인 : Redis
2. 금융거래 : Mysql

### 이유
- **Redis의 성능과 Mysql의 데이터 안정성을 조합해 사용자 경험 & 데이터 안정성 둘다 확보 가능**
- Redis를 사용해 로그인/세션관리를 해 사용자가 빠르게 시스템 이용 가능
- Redis TTL기능을 이용해 세션만료를 자동으로 관리해 보안성 강화 가능
- 중요한 금융거래정보는 데이터 영속,무결성이 필수이기에 MySql에 사용
- 비정상 서버 종료로 이체시작후 종료되더라도 이체의 데이터는 MySql에 남아서 안전

### 로컬세팅
1. 도커실행시 항상 레디스 실행시키기
```shell
docker update --restart unless-stopped my-redis
```
2. local yaml세팅
```yaml
  redis:
    host: 127.0.0.1
    port: 6379 
```

3. gradle 세팅
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

4. RedisConfig
- api모듈에 세팅
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
} 
```

5. 활용
    1. 저장
   ```java
    String sessionId = httpSession.getId();
    String redisKey = "USER_SESSION:" + sessionId;
    redisTemplate.opsForValue().set(redisKey, userVO); 
   ```
   2. 삭제
   ```java
   redisTemplate.delete(redisKey); 
   ```
   3. 호출
   ```java
   UserVO userVO = (UserVO) redisTemplate.opsForValue().get("USER_SESSION:" + sessionId); 
   ```
   4. TTL
   ```java
   redisTemplate.opsForValue().set(redisKey, userVO, 1800L, TimeUnit.SECONDS); 
   ```
