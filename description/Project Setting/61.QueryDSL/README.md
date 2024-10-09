## QueryDSL

### 정의
- TYPE-SAFE 방식으로 쿼리를 작성할 수 있는 자바 기반의 쿼리 빌더 라이브러리
- 여러 데이터베이스에서 쿼리를 작성할 수 있는 다양한 모듈을 제공
- 동적 쿼리를 쉽게 작성할 수 있음
```java
JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QMember member = QMember.member;

        List<Member> members = queryFactory
        .selectFrom(member)
        .where(member.age.gt(20))
        .fetch();
```

### QueryDSL Configuration
- 이 방법을 쓰면 어디서든지 jpaQueryFactory로 QueryDSL 사용가능
- 각 클래스마다 JPAQueryFactory(entityManager)을 생성해서 사용해도되지만 이게 더 편리
```java
@RequiredArgsConstructor
@Configuration
public class QueryDslConfiguration {

  @PersistenceContext
  private final EntityManager entityManager;

  @Bean
  public JPAQueryFactory jpaQueryFactory() {
    return new JPAQueryFactory(entityManager);
  }
}
```
