package org.payment.api.config.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {
    @Bean
    public KafkaTemplate<String, Object> KafkaTemplateForGeneral() {
        return new KafkaTemplate<String, Object>(ProducerFactory());
    }

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

    // Producer
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

    @Bean
    // ConcurrentKafkaListenerContainerFactory : 하나 이상의 Kafka 리스너 컨테이너를 생성하고 관리하는 역할
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> myfactory = new ConcurrentKafkaListenerContainerFactory<>();
        myfactory.setConsumerFactory(ConsumerFactory());
        return myfactory;
    }
}