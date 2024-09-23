## Kafka EC2 세팅
- instance 3대로구성
- port 2181, 3888(ZooKeeper) , 9092(Kafka)열기

1. Java설치
```shell
apt-get install default-jre
export JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
```

2. kafka 설치
```shell
mkdir kafka
curl "https://archive.apache.org/dist/kafka/2.7.2/kafka_2.12-2.7.2.tgz" -o /home/ubuntu/kafka/kafka.tgz
cd kafka
tar -svzf kafka-tgz --strip 1 ## 압축해제
```
- 재실행되도 세팅유지
```shell
vi .bashrc
export JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"
export KAFKA_HEAP_OPTS="-Xms 400m -Xmx 400m"
export PATH =$PATH:$JAVA_HOME/bin
```

3. Kafka, zookeeper service세팅
```shell
cd kafka/config/
vi server.properties
```
- server.properties
```shell
advertiesd.listeners=PLAINTEXT://52.79.249.86:9092 ## instance 퍼블릭IP
log.dirs=/home/ubuntu/kafka/logs
```
- kafka.service
```shell
sudo vi /etc/systemd/system/kafka.service
```
```shell
[Unit]
Requires=zookeeper.service
After=zookeeper.service
[Service]
Type=simple
User=ubuntu
ExecStart=/bin/sh -c '/home/ubuntu/kafka/bin/kafka-server-start.sh 
/home/ubuntu/kafka/config/server.properties > /home/ubuntu/kafka/kafka.log 2>&1'
ExecStop=/home/ubuntu/kafka/bin/kafka-server-stop.sh
Restart=on-abnormal
[Install]
WantedBy=multi-user.target
```

- zookeeper.serivce : /home/ubuntu/kafka/config
```shell
sudo vi /etc/systemd/system/zookeeper.service
```
```shell
[Unit]
Requires=network.target remote-fs.target
After=network.target remote-fs.target
[Service]
Type=simple
User=ubuntu
ExecStart=/home/ubuntu/kafka/bin/zookeeper-server-start.sh /home/ubuntu/kafka/config/zookeeper.properties
ExecStop=/home/ubuntu/kafka/bin/zookeeper-server-stop.sh
Restart=on-abnormal
[Install]
WantedBy=multi-user.target
```

4. Kafka, Zookeeper properties 세팅(모든 3개의 인스턴스에)
- kafka.properties
```shell
vi ~/kafka/config/server.properties
```
```shell
## 기존 값 수정
broker.id=0~2 ## 각 서버별로 다르게
advertised.listener=PLAINTEXT:// 3.38.1.13 :9092 ## 각 인스턴스의 publicIP
zookeeper.connect= 172.31.20.112:2181, 172.31.9.182:2181, 172.31.7.201:2181 ## 각서버의 privateIP
```

- ID설정
```shell
mkdir ~/kafka/zookeeper
echo 1> ~/kafka/zookeeper/myid
echo 2> ~/kafka/zookeeper/myid ## 2번째 인스턴스
echo 3> ~/kafka/zookeeper/myid ## 3번째 인스턴스
```
- zookeeper.properties
```shell
vi ~/kafka/config/zookeeper.properties
```
```shell
dataDir=/home/ubuntu/kafka/zookeeper ## 원래있는값 바꾸기
## 신규값들
initLimit=5
syncLimit=2
server.1=172.31.20.112:2888:3888 ## 2888: 팔로워가 리더와 동기화를 위한 포트 
server.2=172.31.9.182:2888:3888  ## 3888: 리더 문제 발생 시 리더를 선출하기 위한 포트
server.3=172.31.7.201:2888:3888
```



5. 실행
```shell
sudo systemctl start kafka ## Zookeeper도 같이 실행됨

## 안될떄는 meta.properties가 업데이트 안되서 그럼
cd ~/kafka/logs 
rm -rf meta.properties
```
- 상태확인
```shell
sudo systemctl status kafka 
```

- Service Enable
```shell
sudo systemctl enable zookeeper
sudo systemctl enable kafka
```


