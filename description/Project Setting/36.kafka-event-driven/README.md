## 실제 Kafka사용해보기
- Kafka를 통하면 기존 feign을 사용할 필요가 없음
- Kafka가 대신 메시지를 전달/수신하기 때문

1. application.yml
```yml
  kafka:
    bootstrap-servers:
      - kafka1:9092
      - kafka2:9092
      - kafka3:9092
    consumer:
      group-id: testgroup
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
```

2. build.gradle
- 데이터직렬화 도구
- Protobuf 파일(.proto)을 사용해 정의된 데이터 구조를 자동으로 Java, Python, Go 등의 여러 언어로 변환해주는 기능을 제공
```yaml
id 'com.google.protobuf' version '0.9.4' 
```
```yaml
implementation 'com.google.protobuf:protobuf-java:3.25.2'
```
```yaml
protobuf {
	protoc {
		artifact = 'com.google.protobuf:protoc:3.6.1'
	}
}
```

3. proto파일 만들기
- java패키지와 동일한 레벨로 proto 패키지생성
- eda_message.proto 파일 생성
```protobuf
syntax = "proto3";

package sideproject.protobuf;

// Catalog -> Search(상품생성, 상품삭제 메시지)
message ProductTags{
  int64 product_id = 1;
  repeated string tags = 2;
}
```

4. Catalog-service 상품생성/삭제 메시지 발송
```java
private final KafkaTemplate<String, byte[]> kafkaTemplate;

public Product registerProduct(Long sellerId, String name, String description, Long price,
                               Long stockCount, List<String> tags){
    var sellerProduct = new SellerProduct(sellerId);
    sellerProductRepository.save(sellerProduct);

    Product product = new Product(sellerProduct.id, sellerId, name, description, price, stockCount, tags);

    var message = EdaMessage.ProductTags.newBuilder()
                    .setProductId(product.id)
                    .addAllTags(tags)
                    .build();

    kafkaTemplate.send("product_tags_added", message.toByteArray());

    return productRepository.save(product);
} 
```
```java
public void deleteProduct(Long productId){
    var product = productRepository.findById(productId);

    if(product.isPresent()){
        var message = EdaMessage.ProductTags.newBuilder()
                .setProductId(product.get().id)
                .addAllTags(product.get().tags)
                .build();

        kafkaTemplate.send("product_tags_removed", message.toByteArray());
    }
    
    productRepository.deleteById(productId);
    sellerProductRepository.deleteById(productId);
}
```
5. Search-service 메시지수신
```java
@Component
@RequiredArgsConstructor
public class EventListener {
    private final SearchService searchService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @KafkaListener(topics = "product_tags_added")
    public void consumeTagAdded(byte[] message) throws InvalidProtocolBufferException {
        var object = EdaMessage.ProductTags.parseFrom(message);
        logger.info("[product_tags_added] consumed : {}", object);

        searchService.addTagCache(object.getProductId(), object.getTagsList());
    }

    @KafkaListener(topics = "product_tags_removed")
    public void removeTagAdded(byte[] message) throws InvalidProtocolBufferException {
        var object = EdaMessage.ProductTags.parseFrom(message);
        logger.info("[product_tags_removed] removed : {}", object);

        searchService.removeTagCache(object.getProductId(), object.getTagsList());
    }
} 
```
