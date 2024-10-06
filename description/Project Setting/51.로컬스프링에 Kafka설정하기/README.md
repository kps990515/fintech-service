## 로컬 스프링에 카프카 세팅
- @Profile로 로컬, 운영 구분하기

### 1. KafkaConfig
```java
@Bean
public KafkaTemplate<String, Object> KafkaTemplateForGeneral() {
    return new KafkaTemplate<String, Object>(ProducerFactory());
}
@Bean
// ConcurrentKafkaListenerContainerFactory : 하나 이상의 Kafka 리스너 컨테이너를 생성하고 관리하는 역할
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> myfactory = new ConcurrentKafkaListenerContainerFactory<>();
    myfactory.setConsumerFactory(ConsumerFactory());
    return myfactory;
}
```
- local
```java
@Profile("local")
@Bean
public ProducerFactory<String, Object> localProducerFactory() {
    Map<String, Object> myConfig = new HashMap<>();
    myConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "13.209.230.183:9092, 3.38.76.223:9092, 43.200.22.86:9092");
    myConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    myConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(myConfig);
}

@Profile("local")
@Bean
public ConsumerFactory<String, Object> localConsumerFactory() {
    Map<String, Object> myConfig = new HashMap<>();
    myConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "13.209.230.183:9092, 3.38.76.223:9092, 43.200.22.86:9092");
    myConfig.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, RoundRobinAssignor.class.getName());
    myConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    myConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(myConfig);
}
```
- prod
```java
@Profile("prod")
@Bean
public ProducerFactory<String, Object> ProducerFactory() {
    Map<String, Object> myConfig = new HashMap<>();

    myConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "172.31.10.215:9092, 172.31.6.217:9092, 172.31.4.81:9092");
    myConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    myConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    return new DefaultKafkaProducerFactory<>(myConfig);
}

// Consumer
@Profile("prod")
@Bean
public ConsumerFactory<String, Object> ConsumerFactory() {
    Map<String, Object> myConfig = new HashMap<>();
    myConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "172.31.10.215:9092, 172.31.6.217:9092, 172.31.4.81:9092");
    myConfig.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, RoundRobinAssignor.class.getName());
    myConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    myConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(myConfig);
} 
```