## 실행법

### jenkins
1. 실행
```shell
sudo docker start jenkins_prod 
```
2. jenkins bash 접근
```shell
docker exec -it --user root jenkins_prod /bin/bash
```

### spring
1. 실행
```shell
docker run -d -p 8443:8443 \
  -v /var/log/myapp/:/app/logs \
  --name fintech-service-new fintech-service:latest
```

2. 컨테이너 충돌시
```shell
docker stop fintech-service-new
docker rm fintech-service-new
```

3. 컨테이너 실행 중 로그 확인
```shell
docker logs fintech-service-new
docker logs -f fintech-service-new //전체 컨테이너 로그확인
```

4. logback 저장된 로그 확인
```shell
docker exec -it fintech-service-new /bin/bash
cd logs
ls // 로그 파일확인
cat spring-boot-app.2024-08-21.log
```

### MYSQL
```shell
mysql -h proejct-rds-mysql8.cjug24qm0gxh.ap-northeast-2.rds.amazonaws.com -u developer -p
show databases
create databases projectdb;
```