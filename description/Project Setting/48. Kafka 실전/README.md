## Kafka 실전

### 요건
#### 1. 광고
- 광고 이력이 먼저 들어와야함
- 광고에 머문시간이 10초 이상되어야만 join 대상
- 광고는 고정이라 Table로 변환
#### 2. 구매
- 구매 이력은 구매한 여러개의 상품이 같이 들어옴
- 특정 가격 이상의 구매 상품은 제외
- 구매 상품별로 쪼개서 상품별 구매이력 Table로 변환
#### 3. 광고에 대한 구매효과
- 광고Table, 상품별 구매이력 Table을 Join해서 EffectOrNot값 Produce


### 코드
1. 직렬화, 역직렬화 세팅 -> 역직렬화는 json이 어떤 타입으로 변환해야할지 알아야해서 class필요
```java
Serde<EffectOrNot> effectOrNotSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(EffectOrNot.class));
Serde<PurchaseLog> purchaseLogSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(PurchaseLog.class));
Serde<WatchingAdLog> watchingAdLogSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(WatchingAdLog.class));
Serde<PurchaseLogOneProduct> purchaseLogOneProductSerde = Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(PurchaseLogOneProduct.class));
```

2. 광고이력(adLog Topic) Consume -> KStream -> KTable(adTable)변환
    - toTable() : Kstream -> KTable 변환
    - Masterialized.as("adStore") : KStream을 유지할 스토어이름을 adStore로 설정
    - <String: Key값, WatchingAdLog: Value값, KeyValueStore<Bytes, byte[]>
      - Stream에서 가져온 Store의 Key, Value타입 지정
```java
// 1. Consuming : adLog Topic을 Consuming 하여 KStream으로 받고 KTable 생성
// 1-1. adLog Topic에서 Key(String), Value(watchingAdLogSerde) 읽어옴
KTable<String, WatchingAdLog> adTable = sb.stream("adLog", Consumed.with(Serdes.String(), watchingAdLogSerde))
        // 1-2. key를 userId_productId로 다시 생성
        .selectKey((k,v) -> v.getUserId() + "_" + v.getProductId())
        // 1-3. watchingAdLog.getWatchingTime()) > 10 인것만 필터링
        .filter((k,v)-> Integer.parseInt(v.getWatchingTime()) > 10) // 광고 시청시간이 10초 이상인 데이터만 Table에 담습니다.
        // 1-4. KTable생성
        .toTable(Materialized.<String, WatchingAdLog, KeyValueStore<Bytes, byte[]>>as("adStore") // Key-Value Store로 만들어줍니다.
                .withKeySerde(Serdes.String()) // KTable Key의 형태를 String으로 지정
                .withValueSerde(watchingAdLogSerde) // KTable Value의 형태를 watchingAdLog로 지정
                );
```

3. 구매이력(purchaseLog) KStream으로 Consume
```java
KStream<String, PurchaseLog> purchaseLogKStream = sb.stream("purchaseLog", Consumed.with(Serdes.String(), purchaseLogSerde));
```

4. 구매한 productInfo별로 3번 KStream데이터를 Produce
    - 유저, 상품ID, 주문ID, 가격, 구매시간
```java
BigDecimal highPrice = new BigDecimal("1000000");
purchaseLogKStream.foreach((k,v) -> {
    for (Map<String, ProductInfo> prodInfo : v.getProductInfoList())   {
        // 구매한 productInfoList를 돌면서 product 한개당 tempVO생성
        if (prodInfo.get("price").getPrice().compareTo(highPrice) < 0 ) {
            PurchaseLogOneProduct tempVo = new PurchaseLogOneProduct();
            tempVo.setUserId(v.getUserId());
            tempVo.setProductId(prodInfo.get("productId").getProductId());
            tempVo.setOrderId(v.getOrderId());
            tempVo.setPrice(prodInfo.get("price").getPrice());
            tempVo.setPurchasedDt(v.getPurchasedDt());

            // 1개의 product로 나눈 데이터(tempVo)를 purchaseLogOneProduct Topic으로 Produce
            myprdc.sendJoinedMsg("purchaseLogOneProduct", tempVo);

            // 하기의 method는 samplie Data를 생산하여 Topic에 넣습니다. 1개 받으면 여러개를 생성하기 때문에 무한하게 생성됩니다. .
            // sendNewMsg();
        }
    }
}
); 
```

5. 4번에서 Produce한 데이터(유저,상품별 구매이력)를 KTable(purchaseLogOneProduct)로 변환
```java
KTable<String, PurchaseLogOneProduct> purchaseLogOneProductTable= sb.stream("purchaseLogOneProduct", Consumed.with(Serdes.String(), purchaseLogOneProductSerde))
        .selectKey((k,v)-> v.getUserId()+ "_" +v.getProductId())
        .toTable(Materialized.<String, PurchaseLogOneProduct, KeyValueStore<Bytes, byte[]>>as("purchaseLogStore")
                .withKeySerde(Serdes.String())
                .withValueSerde(purchaseLogOneProductSerde)
        ); 
```

6. adTable과 purchaseLogOneProduct을 Inner조인할 tableStreamJoiner 생성 -> EffectOrNot에 담기
    - EffectOrNot : 유저ID, 광고ID, 주문ID, 구매상품정보(상품ID, 가격)
```java
ValueJoiner<WatchingAdLog, PurchaseLogOneProduct, EffectOrNot> tableStreamJoiner = (leftValue, rightValue) -> {
    EffectOrNot returnValue = new EffectOrNot();
    returnValue.setUserId(rightValue.getUserId());
    returnValue.setAdId(leftValue.getAdId());
    returnValue.setOrderId(rightValue.getOrderId());
    Map<String, String> tempProdInfo = new HashMap<>();
    tempProdInfo.put("productId", rightValue.getProductId());
    tempProdInfo.put("price", rightValue.getPrice().toString());
    returnValue.setProductInfo(tempProdInfo);
    return returnValue;
}; 
```

7. 조인해서 광고효과 Produce
   - Topic : AdEvaluationComplete
   - Key : userId_ProductId
   - value : EffectOrNot
```java
adTable.join(purchaseLogOneProductTable,tableStreamJoiner)
    .toStream().to("AdEvaluationComplete", Produced.with(Serdes.String(), effectOrNotSerde)); 
```