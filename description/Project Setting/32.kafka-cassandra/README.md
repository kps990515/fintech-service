## Cassandra
### 장점
  - 대용량 데이터 처리에 좋음
  - 시계 데이터 등 파티셔닝이 용이하고 고속처리에 용이
  - 높은 확장성
  - 고가용성, 다중 데이터 센터
  

### 특징
- masterless 분산 아키텍처
- gossip 프로토콜사용(하나의 노드에 들어오면 주변 노드에 정보 전달)
- key-value 형태
- CQL(Cassandra Query Langunage)제공
- 데이터구성
  - Keyspace : 테이블 그룹(스키마)
  - Table : 테이블
  - Row : key(pk)로 구분되어 row로 데이터 저장
  - Column : row마다 다른 Column 세팅 가능
  - Partition : 각 row의 PK를 기준으로 나뉘는 그룹
- 실행순서
  1. client -> 데이터들어오면 Partition Key 전달
  2. Partition Key를 통해 hashing을 통해 어떤 Node에 전달할지 결정


### 설정
- build.gradle
```yaml
implementation 'com.datastax.oss:java-driver-core:4.13.0'
implementation 'org.springframework.boot:spring-boot-starter-data-cassandra'
```

- docker-compose.yml
```yml
  cassandra-node-0:
     image: cassandra
     environment:
        - CASSANDRA_SEEDS=cassandra-node-0 #클러스터 내부 노드
        - CASSANDRA_CLUSTER_NAME=MyCluster #클러스터 이름
        - CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch #각 카산드라에 전파 방법(gosship)
        - CASSANDRA_DC=datacenter11
     ports:
        - "7000:7000"   # 노드간 클러스터 내부 통신
        - "7001:7001"   # 노드간 보안 통신에 사용
        - "9042:9042"   # CQL 클라이언트와 통신
```

- application.yml
```yaml
spring:
  data:
    cassandra:
      keyspace-name: catalog
      port: 9042
      contact-points: localhost
      local-datacenter: dc1
      schema-action: create-if-not-exists 
```

- config
```java
@Configuration
public class CassandraConfig extends AbstractCassandraConfiguration {

  @Bean
  @Primary  // 이 빈을 우선적으로 사용
  public CqlSession cqlSession() {
    return CqlSession.builder()
            .withKeyspace(getKeyspaceName())  // 키스페이스 설정
            .build();
  }

  @Override
  protected String getKeyspaceName() {
    return "catalog";  
  }
} 
```

- repository
```java
@Repository
public interface ProductRepository extends CassandraRepository<Product, Long> {
} 
```

- entity
```java
@Table("product")
public class Product {
    @PrimaryKey
    public Long id;

    @Column
    public Long sellerId;
}
```

- keyspace, table 생성법
```shell
docker ps
```
```shell
docker exec -it catalogservice-cassandra-node-0-1 cqlsh
```
```shell
CREATE KEYSPACE catalog WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
```
```shell
CREATE TABLE IF NOT EXISTS catalog.product (
  id BIGINT PRIMARY KEY,
  sellerId BIGINT,
  name TEXT,
  description TEXT,
  price BIGINT,
  stockCount BIGINT,
  tags list<TEXT>
);
```
