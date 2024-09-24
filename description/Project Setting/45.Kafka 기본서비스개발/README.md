## Kafka 기본 서비스 개발

### build.gradle
```yaml
implementation 'org.springframework.kafka:spring-kafka
```

### Kafka Cofig
- Producer : 기본적으로 한개로도(단일작업으로도) 다수의 메시지 전송 가능
- Consumer : 병렬처리로 메시지를 소비(높은처리량, 부하 분산, 확장성)
  - ConcurrentKafkaListenerContainerFactory : 여러개의 Listener를 관리하는 Factory
  - Group ID로 Consumer Group을 관리 -> 같은 Group내 Consumer는 서로 다른 Partition을 consume
- Partition 할당전략
  - RangeAssignor : 파티션을 범위로 나누어 할당(1,2/3,4/5,6) 
    - 장점 : 연속적 파티션할당으로 데이터 일관성, Consumer와 파티션의 수가 비슷한 경우 간단 적용 가능 
    - 단점 : Consumer와 파티션의 수가 다를 경우, 불균형 발생 가능
  - RoundRobinAssignor : 균등분배 방식(0,3/1,4/2,5)
    - 장점 : Consumer 파티션 수가 차이나도 Consumer가 고르게 파티션 처리 가능
    - 단점 : 특정 파티션에 데이터가 많은 경우 불균형 발생, 고정된 파티션<->consumer 설정 불가(둘다 RangeAssigner에도 적용)
  - StickyAssignor : 최소 변경 할당 -> Partition추가 시 기존을 최대한 유지하면서 재분배 
    - 장점 : 파티션 재할당을 최소화하여 Consumer 그룹의 안정성을 유지, 재할당 성능 저하 방지
    - 단점 : 시간이 지나면서 최적의 균형이 무너질 수 있음

```yaml
spring:
  kafka:
    consumer:
      group-id: spring-group
      ## offset정보 없을때 어디서 부터 읽을지 옵션 : earlist(맨처음), latest(가장최근), none(에러발생)
      auto-offset-reset: earliest 
      properties:
        ## 여기에 넣어도 되고 ConsumerConfig 클래스에 넣어도 되고
        partition.assignment.strategy: org.apache.kafka.clients.consumer.RoundRobinAssignor
```

```java
@Configuration
@EnableKafka
public class KafkaConfig {
    @Bean
    public KafkaTemplate<String, Object> KafkaTemplateForGeneral() {
        return new KafkaTemplate<String, Object>(ProducerFactory());
    }

    // Producer
    @Bean
    public ProducerFactory<String, Object> ProducerFactory() {
        Map<String, Object> myConfig = new HashMap<>();

        myConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "13.125.205.11:9092, 3.36.63.75:9092, 54.180.1.108:9092");
        myConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        myConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory<>(myConfig);
    }

    // Consumer
    @Bean
    public ConsumerFactory<String, Object> ConsumerFactory() {
        Map<String, Object> myConfig = new HashMap<>();
        myConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "13.125.129.151:9092, 3.39.236.110:9092, 13.125.110.158:9092");
        myConfig.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, RoundRobinAssignor.class.getName());
        myConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        myConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(myConfig);
    }

    @Bean
    // ConcurrentKafkaListenerContainerFactory : 하나 이상의 Kafka 리스너 컨테이너를 생성하고 관리하는 역할
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> myfactory = new ConcurrentKafkaListenerContainerFactory<>();
        myfactory.setConsumerFactory(ConsumerFactory());
        return myfactory;
    }
}
```

### Producer Controller / Service
```java
@RestController
@RequiredArgsConstructor
public class ProducerController {
    private final Producer producer;
    
    @PostMapping("/message")
    public void PublishMessage(@RequestParam String msg) {
        producer.pub(msg);
    }
}
```
```java
@Service
@RequiredArgsConstructor
public class Producer {
    private final KafkaConfig myConfig;

    private KafkaTemplate<String, Object> kafkaTemplate;
    String topicName = "defaultTopic";

    public void pub(String msg) {
        kafkaTemplate = myConfig.KafkaTemplateForGeneral();
        kafkaTemplate.send(topicName, msg);
    }
}
```

### Consumer Service
- @KafkaListener : KafkaConfig에 정의된 Listener 설정을 Spring이 자동으로 사용
```java
@Service
public class ConsumerService {
    @KafkaListener(topics = "defaultTopic", groupId = "spring") //groupdId : Consumer GroupId
    public void consumer (String message) {
        System.out.printf("Subscribed :  %s%n", message);
    }
}
``` 

### 실행방법 : 로컬에서 프로젝트 실행 후
- Kafka Topic생성
```shell
## ZooKeeper 사용버전
kafka-topics.sh --create --topic myTopic --zookeeper <zookeeper-server:port> --replication-factor 2 --partitions 3
## Kraft사용 버전
kafka-topics.sh --create --topic myTopic --bootstrap-server <broker-server:port> --replication-factor 2 --partitions 3
```

- produce
```shell
## Topic생성
~/kafka/bin/kafka-console-producer.sh --broker-list 172.31.20.112:9092,... (kafka.properties의 IP) --topic defaulTopic
consumingtest ## Listerner 로컬 intellij 로그에 찍힘
consumingtest2 ## Listerner 로컬 intellij 로그에 찍힘
```

- consume
```shell
## Zookeeper 버전
~/kafka-console-consumer.sh --zookeeper 172.31.20.112:9092,... (kafka.properties의 IP) --topic myTopic --from-beginning
## Kraft 버전
~/kafka-console-consumer.sh --bootstrap-server 172.31.20.112:9092,... (kafka.properties IP) --topic myTopic --from-beginning
```