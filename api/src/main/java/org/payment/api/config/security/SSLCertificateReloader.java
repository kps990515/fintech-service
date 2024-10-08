package org.payment.api.config.security;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.*;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Profile("!local")
public class SSLCertificateReloader {

    private SSLContext sslContext;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int DAYS_BEFORE_EXPIRY = 30;
    private WatchService watchService;

    private final SSLConfigProperties sslConfigProperties; // SSLConfigProperties 주입

    // SSLConfigProperties를 주입받는 생성자
    public SSLCertificateReloader(SSLConfigProperties sslConfigProperties) throws Exception {
        this.sslConfigProperties = sslConfigProperties;
        // 애플리케이션 시작 시 초기 SSL 설정 로드
        reloadCertificate();
        // 인증서 변경 감지를 위한 WatchService 설정
        startScheduledCertificateCheck();
    }

    // WatchService를 설정하여 인증서 변경 감지
    private void startScheduledCertificateCheck() {
        try {
            watchService = FileSystems.getDefault().newWatchService();

            // 인증서 경로와 파일을 확인
            Path certPath;
            if (!sslConfigProperties.getKeyStore().startsWith("classpath:")) {
                // 파일 시스템 경로일 경우 WatchService 설정
                certPath = Paths.get(sslConfigProperties.getKeyStore());

                // 인증서 파일이 위치한 디렉토리에서 변경 감지 설정
                certPath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            } else {
                // 클래스패스 리소스는 WatchService로 감지할 수 없기 때문에 로그로 처리
                System.out.println("WatchService를 사용할 수 없습니다. Classpath 리소스는 변경 감지에서 제외됩니다.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 1시간마다 인증서 변경 및 만료 여부를 주기적으로 체크
    @Scheduled(fixedRate = 3600000)
    private void checkCertificate() {
        try {
            WatchKey key;
            boolean certificateChanged = false;

            // WatchService에서 파일 변경 이벤트가 있는지 확인
            while ((key = watchService.poll()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.context().toString().equals(Paths.get(sslConfigProperties.getKeyStore()).getFileName().toString())) {
                        certificateChanged = true;
                        reloadCertificate(); // 인증서 변경 시 재로드
                    }
                }
                key.reset();
            }

            // 인증서가 변경되었거나, 주기적으로 만료 여부를 체크
            if (certificateChanged) {
                reloadCertificate();
            }
            checkCertificateExpiry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 인증서 로드 및 SSLContext 갱신
    private void reloadCertificate() {
        lock.writeLock().lock();
        try {
            KeyStore keyStore = loadKeyStore();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, sslConfigProperties.getKeyPassword() != null ? sslConfigProperties.getKeyPassword().toCharArray() : sslConfigProperties.getKeyStorePassword().toCharArray());

            SSLContext tempContext = SSLContext.getInstance("TLS");
            tempContext.init(kmf.getKeyManagers(), null, null);
            this.sslContext = tempContext;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // 인증서 만료 여부 체크
    private void checkCertificateExpiry() {
        lock.readLock().lock();
        try {
            KeyStore keyStore = loadKeyStore();
            String alias = keyStore.aliases().nextElement();
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            long diffInDays = calculateDaysToExpiry(certificate);

            if (diffInDays <= DAYS_BEFORE_EXPIRY) {
                System.out.println("SSL 인증서가 " + diffInDays + "일 후 만료됩니다. 새로운 .p12 파일을 생성합니다.");
                generateNewP12File(
                        sslConfigProperties.getKeyStore(), sslConfigProperties.getKeyStorePassword());
                reloadCertificate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }

    // KeyStore 로드
    private KeyStore loadKeyStore() throws Exception {
        InputStream inputStream;

        if (sslConfigProperties.getKeyStore().startsWith("classpath:")) {
            // 클래스패스 리소스를 사용하여 InputStream을 얻어옴
            ClassPathResource resource = new ClassPathResource(sslConfigProperties.getKeyStore().replace("classpath:", ""));
            inputStream = resource.getInputStream();
        } else {
            // 파일 시스템의 절대 경로에서 InputStream을 얻어옴
            inputStream = Files.newInputStream(Paths.get(sslConfigProperties.getKeyStore()));
        }

        try (inputStream) {
            KeyStore keyStore = KeyStore.getInstance(sslConfigProperties.getKeyStoreType());
            keyStore.load(inputStream, sslConfigProperties.getKeyStorePassword().toCharArray());
            return keyStore;
        }
    }

    // 인증서 만료일까지 남은 일 수 계산
    private long calculateDaysToExpiry(X509Certificate certificate) {
        Date notAfter = certificate.getNotAfter();
        Date currentDate = new Date();
        long diffInMillies = notAfter.getTime() - currentDate.getTime();
        return diffInMillies / (1000 * 60 * 60 * 24);
    }

    // 새로운 p12 파일 생성 로직
    private void generateNewP12File(String outputP12Path, String password) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try (InputStream certInputStream = Files.newInputStream(Paths.get("src/main/resources/new-certificate.crt"))) {
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certInputStream);

            String keyPEM = new String(Files.readAllBytes(Paths.get("src/main/resources/new-private-key.key")))
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decodedKey = Base64.getDecoder().decode(keyPEM);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry("alias", privateKey, password.toCharArray(), new Certificate[]{cert});

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
