## feign
- @EnableFeignClients 추가
```java
@SpringBootApplication
@EnableFeignClients
public class CatalogServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CatalogServiceApplication.class, args);
  }

} 
```
- feignClient 인터페이스 생성
```java
@FeignClient(name = "searchClient", url = "http://search-service:8080")
public interface SearchClient {
    @PostMapping(value = "/search/addTagCache")
    void addTagCache(@RequestBody ProductTagDto dto);
    @PostMapping(value = "/search/removeTagCache")
    void removeTagCache(@RequestBody ProductTagDto dto);
}
```
- 서비스 사용
```java
searchClient.addTagCache(dto);
```