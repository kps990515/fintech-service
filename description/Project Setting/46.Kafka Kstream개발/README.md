## Kafka KStream 서비스 개발
- Kafka Broker : 메시지 저장 및 전송 관리
- Kafka Streams : Kafka의 메시지를 읽어와서 변환/필터링/집계 등 작업 후 Kafka나 다른 시스템에 전송 

| **구분**              | **Kafka 브로커**                                | **Kafka Streams 애플리케이션**                       |
|-----------------------|-------------------------------------------------|------------------------------------------------------|
| **역할**              | 메시지 저장 및 전송 관리                         | 실시간 데이터 스트림 처리 (필터링, 집계 등)            |
| **복제본 관리**       | 데이터 내구성을 위해 복제본 관리 (`MIN_IN_SYNC_REPLICAS_CONFIG`) | 상태 저장소의 스탠바이 복제본 관리 (`NUM_STANDBY_REPLICAS_CONFIG`) |
| **데이터 처리**       | 메시지를 저장하고 컨슈머에게 전달                | 메시지를 실시간으로 처리하고 변환                    |
| **주요 기능**         | 토픽, 파티션, 리더 선출, 복제본 관리              | 스트림 처리, 상태 관리, 데이터 변환 및 집계           |


### build.gradle
```yaml
implementation 'org.apache.kafka:kafka-streams
```

### Kafka Cofig
- Sereds : Serialization, Desirialization의 약자
- ack옵션
  - 0 : Producer가 메시지 전송하고 나서 기록확인 안하고 다음 메시지 발송
  - 1 : 리더 브로커가 메시지를 받았을때 다음 메시지 발송
  - all : 리더와 모든 복제본이 받았을때 다음 메시지 발송
```java
@Configuration
@EnableKafkaStreams
@EnableKafka
public class KafkaConfig {

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration myKStreamConfig() {
        Map<String, Object> myKStreamConfig = new HashMap<>();
        myKStreamConfig.put(StreamsConfig.APPLICATION_ID_CONFIG, "kakfa-stream");
        myKStreamConfig.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "13.125.205.11:9092, 3.36.63.75:9092, 54.180.1.108:9092");
        // key 직렬화/역직렬화
        myKStreamConfig.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        // value 직렬화/역직렬화
        myKStreamConfig.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        // KafkaStream은 토픽의 파티션을 기반으로 처리 -> 3개의 파티션을 병렬로 처리
        myKStreamConfig.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 3);
        // "all: 모든 복제본이 메시지를 받을때까지 ACK(승인)을 기다리도록 하는 방식
        myKStreamConfig.put(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), "all");
        // 최소 2개의 복제본이 동기화 상태일 때만 데이터가 기록(데이터 안정성)
        myKStreamConfig.put(StreamsConfig.topicPrefix(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG), 2);
        // Kafka Streams 상태 저장 애플리케이션에서 사용할 KafkaStream 상태저장소 복제본을 1개 유지
        // 장애방지용으로 KafkaStreams만을 위한 것이기 때문에 1개로도 충분
        myKStreamConfig.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 1);
        return new KafkaStreamsConfiguration(myKStreamConfig);
    }
}
```

### Stream 기본 서비스
```java
@Service
public class StreamService {

    private static final Serde<String> STRING_SERDE = Serdes.String();

    @Autowired
    public void buildPipeline(StreamsBuilder sb) {
        // "default" Topic의 값들을 조회
        KStream<String, String> myStream = sb.stream("default", Consumed.with(STRING_SERDE, STRING_SERDE));
        // "default1"라는 값이 있으면 "test" Topic으로 전송
        myStream.filter((key, value)-> value.contains("default1")).to("test");
    }
}
```

### Stream Join 서비스
- JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(10))) 
  - 특정 시간 범위 내에서만 두 스트림의 레코드를 조인할 수 있도록 제한(10초)
1. Inner Join
   - 두개의 값이 만났을때 데이터가 생성
```java
ValueJoiner<String, String, String> stringJoiner = (leftValue, rightValue) -> {
    return "[StringJoiner]" + leftValue + "-" + rightValue;
};

KStream<String, String> joinedStream = leftStream.join(rightStream,
        stringJoiner,
        JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(10)));
```
2. Left Join
   - Left값이 생기는 순간 데이터 생성
```java
ValueJoiner<String, String, String> stringLeftJoiner = (leftValue, rightValue) -> {
    return "[StringLeftJoiner]" + leftValue + "-> {" + (rightValue != null ? rightValue : "null") + "}";
};

KStream<String, String> leftJoinedStream = leftStream.leftJoin(rightStream,
        stringLeftJoiner,
        JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(10))); 
```

3. Full Outer Join
   - Left나 Right 값이 생기는 순간 데이터 생성
```java
ValueJoiner<String, String, String> stringOuterJoiner = (leftValue, rightValue) -> {
    return "[StringOuterJoiner]" + leftValue + "+" + rightValue;
};

KStream<String, String> outerJoinedStream = leftStream.outerJoin(rightStream,
        stringOuterJoiner,
        JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(10))); 
```
