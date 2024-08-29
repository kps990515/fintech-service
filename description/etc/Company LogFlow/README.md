## 로그적재/확인 flow

### 1. 순서
1. **로그 수집** : Common Library 함수로 정형화된 로그 생성


2. **로그 적재**
   - 수집된 로그는 NAS(Network Attached Storage)에 저장
   - NAS의 로그 변동을 감지해 Filebeat로 로그 수집


3. **로그 전송 및 처리**
   - Filebeat에서 Flunetd로 전송
   - Flunedtd는 ElasticSearch DB로 전송


4. **로그 저장**
   - ElasticSearch에 로그 저장


5. **로그 검색 및 시각화**
   - Kibana, Grafana

### 2. 역할
1. NAS : 중앙 집중식 스토리지로 로그 통합관리 및 보존/복구 가능
2. Filebeat : Elastic 로그 포워더로 실시간 로그 수집해 fluentd와 같은 로그 처리시스템 전달
3. Fluentd : 수집된 로그를 추출/정제 처리하여 ElasticSearch에 전송
4. ElasticSearch : 로그 데이터의 실시간 분석, 쿼리, 인덱싱
5. Kibana : 로그 데이터 시각화, 모니터링, 정보 검색

