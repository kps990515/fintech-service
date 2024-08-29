## 로그 알람

### 순서
1. Elasticsearch에서 로그 수집
2. Prometheus로 Elasticsearch 데이터를 일정 간격으로 메트릭(시계열 데이터)으로 저장
3. Prometheus 사전 정의 규칙 알림 설정
4. Prometheus 알람 트리거가 된 경우 Grafana에도 정보 전달
5. Grafana에서 Prometheus 발생한 경고 + 시각화
6. 메신저에 경고와 Grafana 대시보드를 전송

   

