## MYsql 세팅

### 로컬
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/payment?useSSL=false&useUnicode=true&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: root1234!!
```

### EC2
```yaml
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://proejct-rds-mysql8.cjug24qm0gxh.ap-northeast-2.rds.amazonaws.com:3306/fintech_service
    username: developer
    password: password1!
```

### 실행
```shell
mysql -h proejct-rds-mysql8.cjug24qm0gxh.ap-northeast-2.rds.amazonaws.com -u developer -p
show databases
create databases projectdb;
```

## JPA

### OSIV(Open Session In View)
- 영속성 컨텍스트가 view에 응답이 갈때가지 유지할지 결정하는 설정(기본값 true)
- Lazy Loading을 사용하기 위해 주로 사용
- 문제는 HTTP요청마다 영속성컨텍스트 <-> DB 커넥션 유지되서 과부하 발생가능
- 실사례
  - true : 상품상세정보를 호출할때 Lazy로딩이 실행되서 LazyInitializationException 미발생
  - fasel : Lazy로딩 실패해서 Exception발생
- 활용법
  - true : Lazy로딩 미사용 / 요청이 많지 않은 곳 / 커넥션 많지 않은 곳
  - false : 실시간 고객서비스(채팅)
  - 어차피 요즘은 front / back이 분리되서 매번 호출하기 때문에 false가 기본값


### 1. JPAConfig
- db모듈에 세팅
```java
@Configuration
// 모든 JPA Repository 인터페이스를 스캔하여 빈으로 등록
@EnableJpaRepositories(basePackages = "org.payment.db")
// @Entity(엔터티) 클래스들을 스캔하여 영속성(Persistence) 컨텍스트에 등록
@EntityScan(basePackages = "org.payment.db")
public class JpaConfig {
}
```

### 2. BaseEntity생성 : 공통사용 컬럼(생성, 변경일자 관리)
- @EnableJpaAuditing을 사용하면, 엔티티가 저장되거나 변경될 때 자동으로 필드를 업데이트
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing  // JPA Auditing 기능 활성화
public class JpaConfig {
}
```

```java
// 공통되는 필드를 상속받아 사용할 수 있도록 정의하는 부모 클래스
// 직접 테이블이 생성되지 않고, 하위 엔티티가 상속받아 사용
@MappedSuperclass
// Auditing 기능을 활성화하여 엔티티의 생성 및 수정 시간을 자동으로 관리
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity implements Persistable<String> {
  @Id
  @Column(nullable = false, updatable = false, length = 45)
  private String id; // 고유 식별자

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  @PrePersist
  private void generateUUID() {
    if (this.id == null || this.id.isEmpty()) {
      this.id = UuidCreator.getTimeOrderedEpoch().toString();
    }
  }

  @Override
  @Transient
  public boolean isNew() {
    return this.createdAt == null;
  }

  @Override
  public String getId() {
    return this.id;
  }
}
```

### 3. Entity생성
- Persistable 사용이유
  - 엔티티의 신규생성 여부 판단 가능
  - getID()를 통해 이미 있는 user에 대한 불필요한 select 방지
  - isNew()를 통해 신규는 insert 있으면 update
  
- @Prepersist
  - 엔티티가 처음 데이터베이스에 저장되기 전에 특정 로직을 실행
  - UUID를 먼저 생성하며 persist전에 불필요한 select쿼리 방지(어차피 중복안될값이라)

- 순서
  - isNew() : CrudRepository.save(entity) → 내부적으로 isNew() 호출하여 INSERT or UPDATE 결정.
  - INSERT 전에는 @PrePersist 실행
  - UPDATE이면 @PreUpdate 실행
```java
// 부모클래스까지 포함하여 equals()와 hashCode()를 생성
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity implements Persistable<String> {
    // Persistable : 엔티티의 신규 상태여부 구분하고 식별자 관리로직 커스터마이징 가능
    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @PrePersist
    // persist전에 기존 uuid체크해서 JPA SAVE시 Select 실행안되도록 함(효율적)
    // persist 연산을 통해 처음으로 데이터베이스에 저장되기 전에 메소드가 실행
    // DB에 처음 저장될때만 실행(Update할때마다 바꾸고 싶으면 @PreUpdate사용)
    private void generateUUID() {
      if (this.userId == null || this.userId.isEmpty()) {
        this.userId = UuidCreator.getTimeOrderedEpoch().toString();
      }
    }

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "email", length = 256, unique = true)
    @Email
    private String email;

    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Override
    @Transient // JPA에서 특정 필드를 데이터베이스 컬럼으로 매핑하지 않도록 설정하는 어노테이션
    // 새로운 Entity의 경우 insert, 아니면 update
    public boolean isNew() {
        return getCreatedAt() == null || getCreatedAt().equals(getModifiedAt());
    }

    @Override
    // id값 반환해서 신규인지 판단
    public String getId() {
        return this.userId;
    }
}
```

### 4. Repository
```java
@Repository
public interface UserRdbRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUserId(String userId);
    Optional<UserEntity> findByEmail(String email);
}
```



