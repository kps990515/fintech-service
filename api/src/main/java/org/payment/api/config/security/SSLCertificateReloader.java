package org.payment.api.config.security;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
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
public class SSLCertificateReloader {

    private SSLContext sslContext;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final int DAYS_BEFORE_EXPIRY = 30;
    private WatchService watchService;

    @Value("${server.ssl.key-store}")
    private String keystorePath;

    @Value("${server.ssl.key-store-password}")
    private String keystorePassword;

    @Value("${server.ssl.key-alias}")
    private String keyAlias;

    @Value("${server.ssl.key-password:#{null}}") // 키 비밀번호는 프로필에 따라 없을 수 있으므로 optional로 처리
    private String keyPassword;

    @Value("${server.ssl.key-store-type}")
    private String keystoreType;

    public SSLCertificateReloader() throws Exception {
        // 애플리케이션 시작 시 초기 SSL 설정 로드
        reloadCertificate();
        // 인증서 변경 감지를 위한 WatchService 설정
        startScheduledCertificateCheck();
    }

    // WatchService를 설정하여 인증서 변경 감지
    private void startScheduledCertificateCheck() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            Path certPath = Paths.get(keystorePath);
            // 파일이 위치한 디렉토리에 파일 변경 이벤트 등록 (ENTRY_MODIFY: 파일이 수정될 때 감지)
            certPath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
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
                    if (event.context().toString().equals(Paths.get(keystorePath).getFileName().toString())) {
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
            kmf.init(keyStore, keyPassword != null ? keyPassword.toCharArray() : keystorePassword.toCharArray());

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
                generateNewP12File("src/main/resources/new-certificate.crt",
                        "src/main/resources/new-private-key.key",
                        keystorePath, keystorePassword);
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
        ClassPathResource resource = new ClassPathResource(keystorePath);
        try (InputStream inputStream = resource.getInputStream()) {
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(inputStream, keystorePassword.toCharArray());
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
    private void generateNewP12File(String certPath, String keyPath, String outputP12Path, String password) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        try (InputStream certInputStream = Files.newInputStream(Paths.get(certPath))) {
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(certInputStream);

            String keyPEM = new String(Files.readAllBytes(Paths.get(keyPath)))
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

