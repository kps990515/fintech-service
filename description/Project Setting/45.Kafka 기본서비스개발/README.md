## Kafka 기본 서비스 개발

### build.gradle
```yaml
implementation 'org.springframework.kafka:spring-kafka
```

### Kafka Cofig
- Producer : 기본적으로 한개로도(단일작업으로도) 다수의 메시지 전송 가능
- Consumer : 병렬처리로 메시지를 소비(높은처리량, 부하 분산, 확장성)
  - ConcurrentKafkaListenerContainerFactory : 여러개의 Listener를 관리하는 Factory
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
    @KafkaListener(topics = "defaultTopic", groupId = "spring")
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