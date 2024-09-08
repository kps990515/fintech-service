## DB SLow Query 모니터링
- 쿼리 중 실행 시간이 비정상적으로 오래 걸리는 쿼리(Slow Query)를 추적하고 분석
- 이로인해 시스템 병목현상을 발견하고 해결하는 것이 목적

### 언제사용하는가
1. 응답시간이 느려질때
2. DB 부하가 높을때
3. 고비용 쿼리 실행시
4. 자주 실행하는 쿼리가 있을 떄
5. 데이터가 지속적으로 증가할 때

### 시나리오
1. 애플리케이션 특정 페이지 로딩이 느릴때
2. 대용량 데이터 처리 분석이 필요할 때
3. 쇼핑몰, 금융처럼 다수의 트랜잭션을 비동기/논블로킹으로 처리할때

### 모니터링 방법
1. DB 내장기능 : MySql(Slow Query Log), Oracle(AWR 리포트)
2. 애플리케이션 단위 모니터링 : APM(Application Performance Monitoring)
    - New Relic, Datadog, AppDynamics 등
3. SQL 실행계획 분석 : EXPLAIN 명령어
4. DB 모니터링 툴(서드파티)
    - Mysql(PMM), Oracle(Oracle Enterprise Manager)

### 나에게 적용할 수 있는 방법
#### 1. Mysql 로깅 : Slow Query Log 기록
    - etc/mysql/my.cnf
    - Docker desktop 내부 파일디렉토리에서 찾기
```shell
slow_query_log = 1
long_query_time = 2 # 2초 이상 걸리는 쿼리 기록
slow_query_log_file = /var/log/mysql/mysql-slow.log
log_queries_not_using_indexes = 1 # 인덱스를 사용하지 않는 쿼리도 기록
```
- 로그저장소 만들기 & 재기동
```shell
mkdir -p /var/log/mysql
touch /var/log/mysql/mysql-slow.log
chown mysql:mysql /var/log/mysql/mysql-slow.log
```
- 설정확인
```shell
cat /var/log/mysql/mysql-slow.log
```
  

#### 2. Hibernate Interceptor VS SpringAOP
1. Hibernate
   - 장점 : Hibernate 엔티티, 쿼리 생성/저장/실행등 모든 이벤트 모니터링 가능
   - 단점 : 비즈니스부분은 안봄, 다른 ORM있으면 적용 불가
2. SpringAOP
   - 장점 : 비즈니스적 트랜잭션 처리 시간 확인 가능
   - 단점 : 성능 오버헤드, SQL자체에 대한 정보는 부족

#### 3. 전역 JPA Monitoring
1. gradle
```yaml
implementation 'mysql:mysql-connector-java:8.0.30'
```
2. yaml
- statement_inspector
  - Hibernate가 SQL쿼리 처리마다 StatementInspector를 통해 검사 / 수정 등 진행
```yaml
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect 
        show_sql: true  
        session_factory:
          statement_inspector: org.payment.api.config.web.interceptor.JpaQueryInterceptor
```
3. JpaQueryInterceptor
```java
public class JpaQueryInterceptor implements StatementInspector {
   private static final Logger log = LoggerFactory.getLogger(JpaQueryInterceptor.class);

   @Override
   public String inspect(String sql) {
      long startTime = System.currentTimeMillis();  // SQL 시작 시간 기록
      String result = sql;  // SQL 문을 그대로 사용

      // SQL 실행 시간 측정
      long executionTime = System.currentTimeMillis() - startTime;
      if (executionTime > 2000) {  // 2초 이상 걸리는 쿼리 로깅
         log.warn("Slow Query detected: {} executed in {} ms", sql, executionTime);
      }

      return result;
   }
} 
```

#### 4. 개별 적용
```java
    public String registerUser(UserRegisterServiceRequestVO requestVO) {
        // try-with-resources사용해서 entityManager & Hibernate Session/Interceptor 생성
        try (EntityManager entityManager = entityManagerFactory.createEntityManager();
             Session session = entityManager.unwrap(Session.class).getSessionFactory().withOptions()
                     .statementInspector(new JpaQueryInterceptor())  // 특정 세션에 StatementInspector 적용
                     .openSession()) {

            Transaction transaction = session.beginTransaction();  // 트랜잭션 시작

            userRdbRepository.findByEmail(requestVO.getEmail())
                    .ifPresent(user -> {
                        throw new ExistUserFoundException();
                    });

            UserEntity newUser = userMapper.toUserEntity(requestVO);
            session.persist(newUser);  // Hibernate 세션을 통해 저장

            transaction.commit();  // 트랜잭션 커밋

            // 비동기 이메일 발송
            emailService.sendWelcomeEmailAsync(newUser.getEmail());

            return requestVO.getEmail();

        } catch (Exception e) {
            // 트랜잭션 롤백
            throw new RuntimeException("Error during registration", e);
        }
    }
}
```
