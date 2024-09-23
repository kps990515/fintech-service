## EC2세팅
```shell
sudo yum install epel-release ## 1.epel설치

sudo yum install redis 

sudo systemctl start redis
sudo systemctl enable redis

sudo systemctl status redis

sudo vi /etc/redis.conf

sudo systemctl restart redis

sudo redis-cli
```

## Configuration
- master, replica로 2개의 Redis
- Sentinel은 3개로 설정 : 안정성확보 & Failover 판단을 다수결로 의사결정 하기 때문
- 보안그룹은 6379, 26379 열어주기

1. 기존 instanse로 새로운 instance생성
2. 신규 instance(relica용) 
   - redis.conf에서
   ```shell
   replicaof 172.31.11.234 6379 ## 기존 master IP
   ```
3. 기존 master reids 세팅 변경
```shell
sudo vi /etc/redis.conf
```
- 설정파일에서
  1. bind 127.0.0.1 -::1 -> bind 0.0.0.0
  2. protected-mode no

  4. 확인
  - master
    ```shell
    # Replication
    role:master
    connected_slaves:1
    ```
  - slave
     ```shell
      # Replication
      role:slave
      master_host:172.31.11.234
      master_port:6379
      ```
4. 데이터 세팅 예제
- Master : redis-cli -> set testKey testValue
- Slave : redis-cli -> get testKey -> testValue가 찍혀야함

## Centinel
1. Master/Slave/API 서버 다
```shell
apt-get install redis-sentinel
```
2. sentinel.conf
```shell
sentinel monitor mymaster 172.31.11.234 ## 3곳에 다 master ip 넣어주기
```