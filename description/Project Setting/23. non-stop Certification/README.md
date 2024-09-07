## 1.타 회사 무중단 인증서방법

1. 리버스 프록시를 사용한 nginx(클라이언트요청 -> 내부서버 전송)
- 흐름
    1. 클라이언트가 https://example.com 접근시, NGINX는 443번 포트에서 암호화된 HTTPS 요청 수령
    2. HTTP/2 프로토콜로 처리
    3. Springboot에 전달 
```shell
server {
listen 443 ssl http2; # 클라이언트에서 443으로 오는 HTTPS요청 처리
server_name example.com; # 클라이언트에서 example.com을 호출할때만 처리하도록 명시
```


2. Nginx에 SSL인증서를 적용 & Springboot에 요청을 전달하는 방식
    1. SSL인증서 적용
    ```shell
    # 서버가 사용할 SSL 인증서(공개키)
    ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
    # 서버의 개인 키
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
    ```

    2. 리버스 프록시 설정
    ```shell
    # Request 처리방법 정의 ( / 는 전체 API 처리, /api, /login 등등)
    location / {...}
    # Request를 어디로 리다이렉트하는지 작성. 8443 -> 자신의 springboot app 이사용하는 포트
    proxy_pass https//localhost:9001; 
    # Nginx가 보낼 HTTP 헤더 설정부분($http_host는 클라가 요청한 도메인 example.com)
    proxy_set_header Host $http_host; 
    # X-Real-IP에 클라의 실제 IP정보 담아서 전달
    proxy_set_header X-Real-IP $remote_addr;
    #$proxy_add_x_forwarded_for : 여러 프록시를 거쳐서 요청했을 때 원래 클라이언트의 IP를 추적
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    ```

    3. 리눅스 참조를 통해 인증서 경로 관리(파일만 바뀌고 경로는 고정)
       - 인증서 파일은 기본 /etc/letsencrypt/live/example.com/에 저장
       - 해당 경로에 대한 심볼릭 링크 지정
       - 인증서 파일 변경 해도 해당 심볼릭 링크는 고정이라 바뀐 인증서를 참조


## 2.공부한 방법
1. Certbot을 사용한 자동갱신
   - 자동갱신 스크립트 생성하여 만료되기 전 자동갱신
  

2. 로드밸런서를 사용한 무중단 배포
   - 한 서버의 인증서를 교체할 때 
   - 로드밸런서로 인증서 만료안된 서버로 라우팅
  

3. 서버 애플리케이션의 핫 리로드
   - 핫리로드 : 애플리케이션 서버나 웹 서버로 서비스 중단 없이 새로운 인증서를 로드(Ngingx, Apache)
   - Springboot에서 키스토어 자동갱신할수 있는 기능 제공
     - Spring Boot Actuator : SSL 컨텍스트를 모니터링해서 재로딩
     - WatchService : 키스토어 파일 변경 감지 후 자동으로 SSL 컨텍스트 로드
     - Srping Cloud Context 사용해 클라우드 서버와 연동 후 자동 갱신
  

4. 크론을 활용한 갱신 자동화
    - 인증서를 갱신한 후 새 인증서를 로드하도록 웹 서버를 자동으로 재시작하는 스크립트를 작성하여 일정 주기로 실행하도록 설정

### 방법
1. Certbot 설치
```shell
sudo apt-get update
sudo apt-get install certbot
sudo certbot renew --dry-run
```

2. application-local.yaml설정 : 이미함
3. keystore.p12 파일생성 : 이미함
4. 핫리로드 구현
    - reloadCertificate : SSL 인증서를 로드하고 SSLContext를 초기화
    - startScheduledCertificateCheck 
      - 인증서 파일의 변경을 감시하도록 설정
      - 파일 변경(수정)이 발생하면 이벤트를 발생
    - checkCertificate : 1시간마다 주기적으로 인증서의 만료 여부를 확인
      - checkCertificateExpiry : 만료되면 새로운 .p12파일 생성하고 SSLCONTEXT다시로드


### 작동
1. SSLConfigProperties로 yaml 세팅 가져오기 : 이상하게 @Value가 안되서
```java
@Component
@ConfigurationProperties(prefix = "server.ssl")
@Getter
@Setter
public class SSLConfigProperties {
    private String keyStore;
    private String keyStorePassword;
    private String keyAlias;
    private String keyPassword;
    private String keyStoreType;
}
```
2. 애플리케이션 시작 시 SSLCertificateReloader 생성자 호출
```java
public SSLCertificateReloader(SSLConfigProperties sslConfigProperties) throws Exception {
    this.sslConfigProperties = sslConfigProperties;
    // 애플리케이션 시작 시 초기 SSL 설정 로드
    reloadCertificate();
    // 인증서 변경 감지를 위한 WatchService 설정
    startScheduledCertificateCheck();
    } 
```
4. startScheduledCertificateCheck() : WatchService를 설정하여 인증서 파일 변경을 감지시작
5. checkCertificate() : @@Scheduled에 의해 1시간 마다 인증서 체크
   - WatchService에서 변경, 만료 감지
   - 변경 : reloadCertificate를 통해 SSL 재로드
   - 만료 : checkCertificateExpiry 호출해서 만료여부 체크
5. checkCertificateExpiry : 만료일 30일 이내인 경우 generateNewP12File()로 재생성하고 reloadCertificate 호출
6. 애플리케이션 종료시 : onDestroy()로 watchservice 종료

### 문제점(Local)
- Local에서 하면 공인된 인증서 cert? 문제로 인해 C드라이브 administration.keystore가 없다고 나옴

### 해결법
1. SSL사용하는 클래스에 로컬기동 제외하기
```java
@Component
@Profile("!local")
public class SSLCertificateReloader

@Component
@Profile("!local")
@ConfigurationProperties(prefix = "server.ssl")
```

2. prod-yaml에만 설정하기
```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore-prod.p12
    key-store-password: password1!
    key-alias: tomcat
    key-password: password1!
    #trust-store: classpath:truststore-local.jks
    #trust-store-password: password1! 
```

