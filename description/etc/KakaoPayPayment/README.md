## 카카오페이 성능개선

### 1. QPS 감소

#### 1. Entity 기준으로 DB 조회를 기준으로 제한적으로 캐시를 적용
- immutable 데이터값 기준 적용
- mutable한 값은 entity caching적용(일정단위로 데이터 업데이트)

#### 1-1. 문제점 : Cacheable, Cacheput 데이터정합성
- @Cacheable : 메서드의 실행 결과를 캐시에 저장, 있으면 가져옴
  - Redis 조회 -> DB 조회 -> Redis 데이터 put
- @Cacheput : 메서드의 실행 결과를 항상 캐시에 저장
  - Redis 조회 안함 -> 수정시 DB에 업데이트 -> Redis에 put
  
#### 1-2. 데이터 정합성 깨짐
1. @Cacheable -> Redis 데이터 X -> DB 조회(A데이터) 
2. @CachePut -> DB업데이트(B데이터) -> Redis put(B데이터)
3. @Cacheable -> A데이터로 Redis put
4. DB는 B데이터지만 Redis에는 A데이터로 저장(문제 발생!!!)

### 1-3. 해결법 : @Cacheable시 Redis Put제거

### 1-4. 완벽하지않음
- CachePut끼리의 데이터 동시성 문제(한번에 다른 데이터 update)
- 분산 Lock이 필요함
- 하지만 결제시스템 상 PK가 Unique하기 떄문에 분산 Lock까지는 사용하지 않음
- DB만 조회하던걸 -> Redis조회,DB조회,Redis업데이트로 API가 늘어났기에 Redis 타임아웃을 짧게함

  
### 2. Transaction 길이 감소

#### 1. OSIV설정 & 커넥션 풀 이슈
- OSIV가 True로 되어있으면 API만큼 DB커넥션 풀이 과부하
- OSIV를 False로 변경

#### 1-1. LazyLoading 문제 해결법
- JPA EntityGraph : 특정 엔티티를 조회할 때 그 엔티티와 연관된 다른 엔티티(관계된 엔티티들)를 함께 로드 
- DTO쿼리 방식 : 쿼리 실행 시 전체 Entity가 아닌 필요한 데이터만 조회
  

1. EntityGraph 설정을 해제하고 명시적으로 LazyLoading을 따로 구현
2. LazyLoading을 할때도 DTO쿼리 방식으로 필요한 데이터만 조회해서 성능 개선

### 3. Redis 병목문제
- Redis에서 병목 문제 발견(하지만 Scale Out이 쉽지 않음)
- Immutable데이터에 대해서는 로컬 캐시 추가도입
