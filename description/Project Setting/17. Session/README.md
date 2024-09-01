## Session

### 정의
- 웹은 기본적으로 Stateless
- 이를 해결하기 위해 HTTP 세션을 사용하여 사용자의 상태를 서버에 유지

### 작동방식 : HttpSession 기본값이 Redis
1. 세션생성(사용자 로그인)
2. 세션ID 저장(서버 : 메모리, 사용자 : 쿠키)
3. 세션만료(TTL)

### 내가 사용한 방법
- Spring Session을 Redis로 기본설정
- application-local.yaml
```yaml
  redis:
    host: 127.0.0.1
    port: 6379
    session:
      store-type: redis
      timeout: 1800s  # 세션 타임아웃 설정 (30분)
```

- gradle
```yaml
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.session:spring-session-data-redis' 
```

- Session에 저장할 객체 생성
```java
@Getter
@Setter
public class UserVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L; // 직렬화 버전 관리용 ID
    private String id;
    private String name;
    private String password;
} 
```

- Session발급 & Redis저장 활용
```java
// 이미 기본 Session이 Redis이기 때문에 httpSession자체를 사용해도됨
if (userVO.getPassword().equals(password)) {
    // 세션에 사용자 정보 저장 (Redis로 자동 저장)
    httpSession.setAttribute("USER_SESSION", userVO);
}
```
- SessionID활용해서 정보 조회
```java
HttpSession httpSession = request.getSession(false); // 세션이 없으면 null 반환
if (httpSession == null) {
    throw new InvalidSessionException();
}
UserVO userVO = ObjectConvertUtil.copyVO(httpSession.getAttribute("USER_SESSION"), UserVO.class);
```

- 로그이웃시 Session & Redis삭제
```java
// HttpSession 무효화(Httpsession객체 내부적으로 세션 ID를 알고있음)
httpSession.invalidate();
```

- TTL세팅
```yaml
  session:
     store-type: redis
     timeout: 1800s # 세션 타임아웃 설정 (고객 움직이 없이 30분 후) 
```