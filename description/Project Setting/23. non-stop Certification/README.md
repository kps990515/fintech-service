## 무중단 인증서

nginx에
server {
listen 443 ssl http2;
server_name example.com;

# ssl 인증서 적용하기
ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

location / { # location 이후 특정 url을 처리하는 방법을 정의(여기서는 / -> 즉, 모든 request)
proxy_pass https//localhost:9001; # Request에 대해 어디로 리다이렉트하는지 작성. 8443 -> 자신의 springboot app 이사용하는 포트
proxy_set_header Host $http_host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header

# 경험
리눅스의 참조를 활용해서 인증서 경로를 스크립트를 통해 관리하고 신규생성후 참조값만 변경해준다
전사적으로 날짜 정해버리기


### 방법
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
```java
@Component
@Component
public class SSLCertificateReloader {

    private SSLContext sslContext;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int DAYS_BEFORE_EXPIRY = 30; // 만료일 알람 날짜
    private WatchService watchService;

    public SSLCertificateReloader() throws Exception {
        // 초기 SSLContext 로드
        reloadCertificate();
        // WatchService를 사용하여 파일 변경 감지
        startScheduledCertificateCheck();
    }

    private void startScheduledCertificateCheck() {
        // 주기적으로 인증서 변경 및 만료 여부를 체크합니다.
        Path certPath = Paths.get("src/main/resources/keystore-local.p12");
        try {
            watchService = FileSystems.getDefault().newWatchService();
            certPath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 3600000) // 1시간마다 실행 (밀리초 단위)
    private void checkCertificate() {
        try {
            WatchKey key;
            while ((key = watchService.poll()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context().toString().equals("keystore-local.p12")) {
                        reloadCertificate();
                    }
                }
                key.reset();
            }
            // 만료 여부 체크
            checkCertificateExpiry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reloadCertificate() {
        lock.writeLock().lock();
        try {
            ClassPathResource resource = new ClassPathResource("keystore-local.p12");

            try (InputStream inputStream = resource.getInputStream()) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(inputStream, "password1!".toCharArray());

                SSLContext tempContext = SSLContext.getInstance("TLS");
                tempContext.init(null, null, null);

                this.sslContext = tempContext;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void checkCertificateExpiry() {
        lock.readLock().lock();
        try {
            ClassPathResource resource = new ClassPathResource("keystore-local.p12");

            try (InputStream inputStream = resource.getInputStream()) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(inputStream, "password1!".toCharArray());

                String alias = keyStore.aliases().nextElement();
                Certificate certificate = keyStore.getCertificate(alias);

                if (certificate instanceof X509Certificate) {
                    X509Certificate x509Cert = (X509Certificate) certificate;
                    Date notAfter = x509Cert.getNotAfter();
                    Date currentDate = new Date();

                    long diffInMillies = notAfter.getTime() - currentDate.getTime();
                    long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);

                    if (diffInDays <= DAYS_BEFORE_EXPIRY) {
                        System.out.println("SSL 인증서가 " + diffInDays + "일 후 만료됩니다. 새로운 .p12 파일을 생성합니다.");
                        // 새로운 p12 파일을 생성
                        generateNewP12File("src/main/resources/new-certificate.crt",
                                "src/main/resources/new-private-key.key",
                                "src/main/resources/keystore-local.p12",
                                "password1!");
                        reloadCertificate(); // 새로 생성한 p12 파일로 SSLContext를 다시 로드
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void generateNewP12File(String certPath, String keyPath, String outputP12Path, String password) throws Exception {
        // 인증서 로드
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try (InputStream certInputStream = Files.newInputStream(Paths.get(certPath))) {
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certInputStream);

            // 개인 키 로드
            String keyPEM = new String(Files.readAllBytes(Paths.get(keyPath)))
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decodedKey = Base64.getDecoder().decode(keyPEM);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // 키스토어에 인증서와 키를 저장
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null); // 새로운 빈 키스토어 생성
            keyStore.setKeyEntry("alias", privateKey, password.toCharArray(), new Certificate[]{cert});

            // p12 파일로 저장
            try (FileOutputStream outputStream = new FileOutputStream(outputP12Path)) {
                keyStore.store(outputStream, password.toCharArray());
            }
        }
    }

    @PreDestroy
    public void onDestroy() {
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SSLContext getSSLContext() {
        lock.readLock().lock();
        try {
            return sslContext;
        } finally {
            lock.readLock().unlock();
        }
    }
}
```


