## Zookeeper 
- 브로커 상태 관리: kafka 클러스터에서 각 Kafka 브로커 모니터링
- 파티션 리더 선출: Kafka는 데이터를 여러 파티션에 분산 저장 / Zookeeper는 각 파티션에 대한 리더 브로커를 선출하고 관리합니다.
- 브로커 동적 설정: Zookeeper를 통해 Kafka 브로커들의 설정을 동적으로 관리
```shell
Docker pull confluentinc/cp-zookeeper
```

## Kafka
```shell
Docker pull confluentinc/cp-kafka
```

## ZooKepper & kafka : Zookeeper는 Kafka 브로커들 간의 **조정(coordination)**을 담당하는 역할
- Kafka 2.8.0 이후 Zookeeper 없이도 운영할 수 있는 KRaft 모드가 도입
1. Kafka의 분산 아키텍처 관리 : 모니터링
2. 리더 선출(+ 장애방지/고가용성)
   - Kafka 클러스터는 각 파티션에서 리더와 팔로워(follower) 브로커를 운영
   - 리더 브로커는 클라이언트 요청을 처리하고, 팔로워 브로커는 리더의 데이터를 복제합니다.
   - 리더 문제 발생 시 Zookeeper개 새로운 리더 선정
3. 브로커와 클라이언트의 메타데이터 저장
   Zookeeper가 kafka의 위치 정보와 파티션 정보를 가지고 클라이언트에게 전달
4. Kafka의 구성 변경 사항 반영
   Kafka 클러스터의 구성(브로커 추가/제거, 파티션 확장 등)이 변경되면 Zookeeper를 통해 이 정보를 전체 클러스터에 전달하고, 클러스터가 새로운 상태에 맞게 조정됩니다.

### 세팅
1. docker.compose
```yaml
version: "3.8"

services:
   zookeeper:
      image: confluentinc/cp-zookeeper:latest
      environment:
         ZOOKEEPER_SERVER_ID: 1
         ZOOKEEPER_CLIENT_PORT: 2181
      ports:
         - "22181:2181"

   kafka1:
      image: confluentinc/cp-kafka:latest
      depends_on: ## zookepper 이후 실행된다는 의미
         - zookeeper
      ports:
         - "19092:19092"
      environment:
         KAFKA_BROKER_ID: 1
         KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
         KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
         KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka1:9092,EXTERNAL://localhost:19092 # 내부는 9092, 외부에서 들어올때는 19092
         KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL

   kafka2:
      image: confluentinc/cp-kafka:latest
      depends_on:
         - zookeeper
      ports:
         - "19093:19093"
      environment:
         KAFKA_BROKER_ID: 2
         KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
         KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
         KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka2:9093,EXTERNAL://localhost:19093
         KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL


   kafka3:
      image: confluentinc/cp-kafka:latest
      depends_on:
         - zookeeper
      ports:
         - "19094:19094"
      environment:
         KAFKA_BROKER_ID: 3
         KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
         KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
         KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka3:9094,EXTERNAL://localhost:19094
         KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL 
```
```shell
## 설정한 다음 
docker-compuse up
```
2. application.yaml
```yaml
 spring:
    kafka:
       bootstrap-servers:
          - 127.0.0.1:19092
          - 127.0.0.1:19093
          - 127.0.0.1:19094
       consumer:
          group-id: testgroup
          auto-offset-reset: earliest
          key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
          value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
       producer:
          key-serializer: org.apache.kafka.common.serialization.StringSerializer
          value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### 예제
- /kafka-test 호출 시 topic1주제로 "message sent (topic1)" produce
- consumer가 콘솔에 "message sent (topic1)" 출력
```java
@GetMapping("/kafka-test")
public void kafkaTest() {
  kafkaService.publish();
}

@Service
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void publish() {
        kafkaTemplate.send("topic1", "message sent (topic1)");
    }

    @KafkaListener(topics = "topic1", groupId = "testgroup")
    public void consume(String message) {
        System.out.println("consumed: " + message);
    }
}
```