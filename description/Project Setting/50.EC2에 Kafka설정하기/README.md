## EC2에 카프카 설정하기
- 인스턴스 생성
- 탄력적 퍼블릭IP 할당
- java설치
```shell
sudo yum update -y  # 패키지 목록 업데이트
sudo yum install java-17-amazon-corretto -y  # Java 17 설치 (Amazon Linux 2 기준)
java -version
```
- Docker 설치
```shell
sudo yum install docker -y  # Docker 설치
sudo service docker start   # Docker 시작
sudo usermod -aG docker ec2-user  # Docker 권한 부여
```

### 각 인스턴스에 다 해야함

1. Docker Compose 파일 작성
```shell
mkdir ~/kafka-docker
cd ~/kafka-docker
nano docker-compose.yml
```

2. docker-compose.yml
- 리더에만 이렇게 나머지는 자기것만
- 마지막엔 kafka-ui도 설정
```yaml
version: '2'
services:
  zookeeper:
    image: wurstmeister/zookeeper:3.4.6
    ports:
      - "2181:2181"
    environment:
      ZOO_MY_ID: 1
      ZOO_SERVERS: server.1=172.31.10.215:2888:3888;server.2=172.31.6.217:2888:3888;server.3=172.31.4.81:2888:3888
    restart: always

  kafka1:
    image: wurstmeister/kafka:latest
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: INSIDE://172.31.10.215:9093,OUTSIDE://13.209.230.183:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INSIDE:PLAINTEXT,OUTSIDE:PLAINTEXT
      KAFKA_LISTENERS: INSIDE://0.0.0.0:9093,OUTSIDE://0.0.0.0:9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_BROKER_ID: 0
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    restart: always

  kafka2:
    image: wurstmeister/kafka:latest
    ports:
      - "9093:9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: INSIDE://172.31.6.217:9093,OUTSIDE://3.38.76.223:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INSIDE:PLAINTEXT,OUTSIDE:PLAINTEXT
      KAFKA_LISTENERS: INSIDE://0.0.0.0:9093,OUTSIDE://0.0.0.0:9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_BROKER_ID: 1
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    restart: always

  kafka3:
    image: wurstmeister/kafka:latest
    ports:
      - "9094:9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: INSIDE://172.31.4.81:9093,OUTSIDE://43.200.22.86:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INSIDE:PLAINTEXT,OUTSIDE:PLAINTEXT
      KAFKA_LISTENERS: INSIDE://0.0.0.0:9093,OUTSIDE://0.0.0.0:9092
      KAFKA_INTER_BROKER_LISTENER_NAME: INSIDE
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_BROKER_ID: 2
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    restart: always

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local-cluster
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: 172.31.10.215:9092,172.31.6.217:9092,172.31.4.81:9092
      KAFKA_CLUSTERS_0_ZOOKEEPER: 172.31.10.215:2181,172.31.6.217:2181,172.31.4.81:2181
      KAFKA_CLUSTERS_0_SCHEMAREGISTRY: ""
    restart: always
```

3. docker-compose 설치 및 실행
```shell
sudo curl -L "https://github.com/docker/compose/releases/download/v2.12.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
cd ~/kafka-docker
docker-compose up -d
```

4. server.properties
```yaml
broker.id=0
advertised.listeners=INSIDE://172.31.10.215:9093,OUTSIDE://13.209.230.183:9092
listeners=INSIDE://0.0.0.0:9093,OUTSIDE://0.0.0.0:9092
inter.broker.listener.name=INSIDE
log.dirs=/var/lib/kafka/logs
zookeeper.connect=172.31.10.215:2181,<ZOOKEEPER2_PRIVATE_IP>:2181,<ZOOKEEPER3_PRIVATE_IP>:2181
log.retention.hours=168
log.segment.bytes=1073741824
zookeeper.connection.timeout.ms=6000
```

5. zookeeper.properties
```yaml
# Zookeeper의 기본 틱 간격 (밀리초)
tickTime=2000

# 클라이언트와 Zookeeper 간의 통신에 사용되는 포트
clientPort=2181

# Zookeeper가 데이터를 저장할 디렉토리 (호스트 경로로 설정)
dataDir=/var/lib/zookeeper

# 리더 선출 및 동기화를 위한 제한 시간 설정
initLimit=5
syncLimit=2

# Zookeeper 서버 설정 (클러스터 내 모든 Zookeeper 서버의 IP 및 포트 정보)
# 172.31.10.215 등은 각 Zookeeper 서버의 private IP로 변경해야 합니다
server.1=172.31.10.215:2888:3888
server.2=<ZOOKEEPER2_PRIVATE_IP>:2888:3888
server.3=<ZOOKEEPER3_PRIVATE_IP>:2888:3888
```

6. kafka.service
- 자동실행/종료, 자동재시작, 수동제어를 위한 설정
```shell
sudo vi /etc/systemd/system/kafka.service
```
```yaml
[Unit]
  Description=Docker-based Kafka Service
  Requires=docker.service
  After=docker.service

  [Service]
  User=ec2-user
  WorkingDirectory=/home/ec2-user/kafka-docker
  ExecStart=/usr/local/bin/docker-compose up -d 
  ExecStop=/usr/local/bin/docker-compose down
  Restart=always

  [Install]
  WantedBy=multi-user.target 
```
```shell
sudo systemctl daemon-reload
sudo systemctl enable kafka
sudo systemctl start kafka
sudo systemctl status kafka
```

7. zookeeper.service
```shell
sudo vi /etc/systemd/system/zookeeper.service
```
```yaml
[Unit]
Description=Docker-based Zookeeper Service
Requires=docker.service
After=docker.service

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user/kafka-docker
ExecStart=/usr/local/bin/docker-compose up -d zookeeper
ExecStop=/usr/local/bin/docker-compose down
Restart=always

[Install]
WantedBy=multi-user.target
```

```shell
```shell
sudo systemctl daemon-reload
sudo systemctl enable zookeeper
sudo systemctl start zookeeper
sudo systemctl status zookeeper
```

8. Zookeeper 데이터 디렉토리 생성
```shell
docker exec -it <zookeeper_container_id> /bin/bash
mkdir -p /var/lib/zookeeper
```

8. 각 인스턴스에 myid 세팅
```shell
docker exec -it <zookeeper_container_id> /bin/bash
echo 1 > /var/lib/zookeeper/myid
echo 2 > /var/lib/zookeeper/myid
echo 3 > /var/lib/zookeeper/myid
```
