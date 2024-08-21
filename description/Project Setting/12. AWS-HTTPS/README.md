## AWS HTTPS

1. EPEL 저장소 활성화
```yaml
sudo amazon-linux-extras install epel
```
2. Certbot 설치
```yaml
sudo yum install -y certbot python3-certbot-nginx
```
3. Certbot 실행
    - 설정한 도메인 2개다 세팅
```yaml
sudo certbot certonly --standalone -d fintechservice.shop -d www.fintechservice.shop
```

4. 기존 로컬 keystore.p12랑 구분하기
   - keystore-local.p12로 이름바꾸고 local yaml 수정  
   

5. 운영용 keystore.p12만들기
    - ec2-user/home/에 있음
```shell
sudo openssl pkcs12 -export -in /etc/letsencrypt/live/fintechservice.shop/fullchain.pem \
-inkey /etc/letsencrypt/live/fintechservice.shop/privkey.pem \
-out keystore.p12 -name tomcat -CAfile /etc/letsencrypt/live/fintechservice.shop/chain.pem -caname root
```
6. keystore.p12 -> jks변환
```shell
keytool -importkeystore -deststorepass password1! -destkeypass password1! -destkeystore keystore.jks -srckeystore keystore.p12 -srcstoretype PKCS12 -srcstorepass password1! -alias tomcat
```

7. keystore.jks -> keystore-prod.jks로 바꾸고 resources에 넣어주기
  

8. prod-yaml세팅
```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore-prod.jks
    key-store-password: password1!
    key-password: password1!
    key-alias: tomcat 
```
9. 실행시키기
- docker image에 버전이 안보이는 이유는 jenkins설정에서 태그를 지우고 latest로 설정해서
- Amazon Elastic Container Registry - Repository에는 v9으로 잘 있음
```shell
docker images 
```

```shell
docker run -d -p 8443:8443 --name fintech-service-new fintech-service:latest
```
