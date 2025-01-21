### 인기글 문제

### 풀어야할 문제
- 인기 게시물을 매번 계산하면 성능저하 발생
- 인기 게시물은 5분 유지가 됨
- 인기 게실물은 최근 3시간 이내 것으로 한정

### 풀이
- Elasticsearch와 + Redis
1. Elasticsearch와 데이터베이스 연동 
2. Elasticsearch에 주기적 인덱싱(id, likes, views, length, createdAt)
3. 5분마다 스케줄러를 사용해 Elasticsearch에서 인기 게시물을 계산
4. 결과값은 Redis 캐싱

### 전 국민 로또

### 풀어야할 문제
- 매주 토요일 7시에 6개 당첨 단어 선정
- 지난 일주일 동안 이체할 때 남긴 메모(적요)에 발표된 6개 단어가 모두 포함된 고객이 1등 당첨으로 선정
- 당첨자가 없을 경우에는 5개 또는 4개 일치하는 고객을 후보로 포함하여 최소 1명의 당첨자 선정
- 고객은 토요일 오후 7시 발표 후 즉시 당첨 여부를 조회가능해야함

### 설계 방식
- Elasticsearch + Spring WebFlux + Redis
1. Elasticsearch를 활용한 주별 데이터 인덱싱
   - 이체 내역 데이터를 Elasticsearch에 주별로 인덱싱(customer_id, memo, transaction_date)
2. 당첨 단어 발표 후 Elasticsearch 단일쿼리로 단어 매칭 및 당첨자 선정
    - 일치하는 단어수가 많은 수록 높은 score받게하여 sort
3. 당첨자 Redis캐싱
4. Spring WebFlux를 사용한 당첨자 조회 API 분리
   - Spring WebFlux를 사용해 비동기 논블로킹으로 당첨자 조회

### ElasticSearch
1. 역색인(Inverted Index) 구조 : 대량의 텍스트 데이터 검색 성능에 좋음
   - 데이터를 저장할 때, 역색인(Inverted Index) 방식을 사용
   - 데이터의 모든 단어를 인덱스에 저장하고 각 단어에 해당하는 위치를 기록
   - 특정 단어나 키워드에 대해 빠르게 검색가능
2. 분산 구조와 샤딩(Sharding)
3. 비동기 및 실시간 데이터 처리
4. 고급 검색 및 필터 기능
5. 캐싱 및 메모리 최적화
6. 수평 확장 가능 (Horizontal Scalability)
7. JSON 기반의 경량 프로토콜
   
