## Session

### 정의
- 웹은 기본적으로 Stateless
- 이를 해결하기 위해 HTTP 세션을 사용하여 사용자의 상태를 서버에 유지

### 작동방식
1. 세션생성(사용자 로그인)
2. 세션ID 저장(서버 : 메모리, 사용자 : 쿠키)
3. 세션만료(TTL)

### 내가 사용한 방법
- Session발급 & Redis저장 활용
```java
// 세션저장
httpSession.setAttribute("USER_SESSION", userVO);
// Redis 저장
String sessionId = httpSession.getId();
String redisKey = "USER_SESSION:" + sessionId;
redisTemplate.opsForValue().set(redisKey, userVO); 
```
- SessionID활용해서 Redis정보 조회
```java
String sessionId = httpSession.getId();
UserVO userVO = (UserVO) redisTemplate.opsForValue().get("USER_SESSION:" + sessionId); 
```

- 로그이웃시 Session & Redis삭제
```java
// Redis에서 세션 데이터 삭제
redisTemplate.delete(redisKey);
// HttpSession 무효화
httpSession.invalidate(); 
```

- TTL세팅
```yaml
  session:
     store-type: redis
     timeout: 1800s # 세션 타임아웃 설정 (고객 움직이 없이 30분 후) 
```