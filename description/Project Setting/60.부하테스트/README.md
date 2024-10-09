## 부하테스트(Locust)

### 세팅
1. docker-compose
```yml
version: '3.7'
services:
  master:
    image: locustio/locust
    ports:
      - "8089:8089"
    volumes:
      - ./:/mnt/locust
    command: -f /mnt/locust/locustfile-issue-asyncV2.py --master -H http://host.docker.internal:8080

  worker:
    image: locustio/locust
    volumes:
      - ./:/mnt/locust
    command: -f /mnt/locust/locustfile-issue-asyncV2.py --worker --master-host master 
```

2. locustfile
```python
from locust import task, FastHttpUser, stats

stats.PERCENTILES_TO_CHART = [0.95, 0.99]

class HelloWorld(FastHttpUser):
    connection_timeout = 10.0
    network_timeout = 10.0

    @task
    def hello(self):
        response = self.client.get("/hello")
```

3. localhost:8089에 들어가 테스트하기
- 총 몇명할건지 / 초당 몇명씩 인입시킬건지 / 어디서버에 연결할건지
- 함수에 Thread.sleep(500); 걸면 
  - 초당 2건 처리 * 서버 쓰레드 풀(기본 200) = RPS400
- 처리량을 넘어서 요청오면 모든 API요청이 fail로 뜸(터짐)

4. WORKER늘리기
```shell
docker-compose up -d --scale worker=3
```