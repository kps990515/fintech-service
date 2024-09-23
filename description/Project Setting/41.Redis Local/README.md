## Redis공부

### Redis Config
```java
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisProperties redisProperties;

    @Bean
    // Lettuce 사용
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

}
```
### ZSET :  .add 뒤에 몇개의 파라미터가 들어오든 맨앞 값이 Key가 됨
#### product조회 방식 
1. Keyword Zset으로 groupID조회
2. groupID로 product들 조회
3. product들 리턴
  

- KeyWord ZSet
```text
키: "keyword_electronics"
값: {
  "product_group_123": 12000,  // 제품 그룹 ID: product_group_123, 최저가: 12000
  "product_group_124": 20000   // 제품 그룹 ID: product_group_124, 최저가: 20000
}
```
- GrpId ZSet
```text
키: "product_group_123"
값: {
  "prod_id_001": 10000,  // 제품 ID: prod_id_001, 가격: 10000
  "prod_id_002": 15000,  // 제품 ID: prod_id_002, 가격: 15000
  "prod_id_003": 12000   // 제품 ID: prod_id_003, 가격: 12000
}
```

### Product 조회/저장
```java
public int SetNewProduct(Product newProduct) {
    int rank = 0;
    myProdPriceRedis.opsForZSet().add(newProduct.getProdGrpId(), newProduct.getProductId(), newProduct.getPrice());
    rank = myProdPriceRedis.opsForZSet().rank(newProduct.getProdGrpId(), newProduct.getProductId()).intValue();
    return rank;
}
```
```java
public Set GetZsetValue(String key)  {
    Set myTempSet = new HashSet();
    myTempSet = myProdPriceRedis.opsForZSet().rangeWithScores(key, 0, 9);
    return myTempSet;
};
```

### ProductGrp 조회/저장
```java
public int SetNewProductGrp(ProductGrp newProductGrp){

    List<Product> product = newProductGrp.getProductList();
    String productId = product.get(0).getProductId();
    double price = product.get(0).getPrice();
    myProdPriceRedis.opsForZSet().add(newProductGrp.getProdGrpId(), productId, price);
    int productCnt = myProdPriceRedis.opsForZSet().zCard(newProductGrp.getProdGrpId()).intValue();
    return productCnt;
}
```

```java
public int SetNewProductGrpToKeyword (String keyword, String prodGrpId, double score){
    myProdPriceRedis.opsForZSet().add(keyword, prodGrpId, score);
    return myProdPriceRedis.opsForZSet().rank(keyword, prodGrpId).intValue();
}
```

### 키워드 통해 조회
```java
public List<ProductGrp> GetProdGrpUsingKeyword(String keyword) {

    List<ProductGrp> returnInfo = new ArrayList<>();
    List<String> prodGrpIdList = new ArrayList<>();
    List<Product> tempProdList = new ArrayList<>();
    
    // input 받은 keyword 로 productGrpId를 조회
    prodGrpIdList = List.copyOf(myProdPriceRedis.opsForZSet().reverseRange(keyword, 0, 9));
    
    //10개 prodGrpId로 loop
    for (final String prodGrpId : prodGrpIdList) {
        Set prodAndPriceList = new HashSet();
        ProductGrp tempProdGrp = new ProductGrp();

        // Loop 타면서 ProductGrpID 로 Product:price 가져오기 (10개)
        prodAndPriceList = myProdPriceRedis.opsForZSet().rangeWithScores(prodGrpId, 0, 9);
        Iterator<Object> prodPricObj = prodAndPriceList.iterator();

        // loop 타면서 product obj에 bind (10개)
        while (prodPricObj.hasNext()) {
            ObjectMapper objMapper = new ObjectMapper();
            // {"value":00-10111-}, {"score":11000}
            Map<String, Object> prodPriceMap = objMapper.convertValue(prodPricObj.next(), Map.class);
            Product tempProduct = new Product();
            // Product Obj bind
            tempProduct.setProductId(prodPriceMap.get("value").toString()); // prod_id
            tempProduct.setPrice(Double.valueOf(prodPriceMap.get("score").toString()).intValue()); //es 검색된 score
            tempProduct.setProdGrpId(prodGrpId);

            tempProdList.add(tempProduct);
        }
        // 10개 product price 입력완료
        tempProdGrp.setProdGrpId(prodGrpId);
        tempProdGrp.setProductList(tempProdList);
        returnInfo.add(tempProdGrp);
    }

    return returnInfo;
    }
```

```java
public Keyword GetLowestPriceProductByKeyword(String keyword) {
    Keyword returnInfo = new Keyword();
    List<ProductGrp> tempProdGrp = new ArrayList<>();
    // keyword 를 통해 ProductGroup 가져오기 (10개)
    tempProdGrp = GetProdGrpUsingKeyword(keyword);

    // 가져온 정보들을 Return 할 Object 에 넣기
    returnInfo.setKeyword(keyword);
    returnInfo.setProductGrpList(tempProdGrp);
    // 해당 Object return
    return returnInfo;
}
```




