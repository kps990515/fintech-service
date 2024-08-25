## 세팅

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

## 활용

### 1. JPAConfig
- db모듈에 세팅
```java
@Configuration
@EnableJpaRepositories(basePackages = "org.payment.db")
@EntityScan(basePackages = "org.payment.db")
public class JpaConfig {
}
```

### 2. BaseEntity생성
- 공통사용 컬럼(생성, 변경일자 관리)
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BaseEntity {
   @CreatedDate
   @Column(updatable = false)
   private LocalDateTime createdAt;

   @LastModifiedDate
   private LocalDateTime modifiedAt;
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
```java
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
    private void generateUUID(){
        this.userId = UuidCreator.getTimeOrderedEpoch().toString();
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
    @Transient
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

### 3. Repository
```java
@Repository
public interface UserRdbRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUserId(String userId);
    Optional<UserEntity> findByEmail(String email);
}
```



