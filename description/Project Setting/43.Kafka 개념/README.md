## Kafka : Distributed Message Platform

- 목적 : Event/Message 전송,소비(동시에 produce, consume가능)
- 장점 : 고가용성 / 빠른처리
- 단점 : 순서보장 어려움 / 아주 작게 사용이 어려움
- 특징
  1. Distributed : 나눠서 작업 -> 빠름
  2. pub/sub : pub은 sub을 신경쓸 필요 없음
  3. Producer, Consumer 존재
  4. 다 : 다의 구조
  5. Server : Broker, ZooKeeper(Broker간의 분산처리 정보 관리) meta, controller, topic, partition
  6. 3개 이상의 Broker로 구성
  7. Kraft를 통해 Zookeeper 제거 가능

## Broker : 실제 Data저장소
## Controller : Broker대장, topic생성, partition생성, 복제본 관리

## Zookeeper
- KafkaCluster 구성 시 
  - Cluster 및 하위요소에 대한 메타정보
  - Controller(Broker의 대장) 정보, 선출
  - Broker 정보 관리

## Kafka Object
- Topic : 폴더, 특정목적으로 생성된 Data집합
- Partition 
  - Topic의 하위 폴더 개념으로 분산을 위해 나누어짐
  - Leader/Follower가 존재하며, read/write는 Leader가 진행 -> Follower가 복사
- Replica : Leader장애 대응용 복사본 follower, pull방식으로 Leader 복제
  - ISR(In Sync Replica) : replica Group, Group에서 Leader가 commit, lag확인해서 group에서 제외하기도함
- Producer : data를 publishing 하는 주체
- Consumer : data를 subscribe하는 주체
- offset : Consumer가 어디까지 읽었는지 저장하는 값(Consumer Group마다 다름)
- lag : Consumer가 아직 읽지 않은 메시지의 수

## Kafka Cluster
1. Zookeeper Ensemble : Kafka Cluster 정보관리
   - Zookeeper들을 3개이상 생성 후 Kafka Instance들의 Meta정보 관리 
2. Kafka Manager : 2개이상 Instance로 생성 / Kafka Cluster 관리
3. Kafka Cluser : 3개이상 Kafka Instance위에 -> Kafka Broker 생성 그 위에 -> topic 저장
   - Kafka Instance 중에 하나는 Controller(리더)역할
4. 데이터 구조
  - Broker 0 : Topic A(Partition 1): Leader / TopicA(Partition 2) : Follower(Broker1의 리더 Follow)
  - Broker 1 : Topic A(Partition 2): Leader / TopicA(Partition 3) : Follower(Broker2의 리더 Follow)
  - Broker 2 : Topic A(Partition 3): Leader / TopicA(Partition 1) : Follower(Broker0의 리더 Follow)

## Kafka와 비슷한 애들
- Rabbit MQ : 장점(경량, 유연한방식), 단점(대규모 데이터 처리 어려움, 디스크에 데이터 저장 안함)
  - 방식
    - Kafka : Producer -> Broker -> Partition -> Consumer
    - RabbitMQ : Producer -> Exchange -> Building Rules -> Queue -> Consumer
  - 주체
    - Kafka : Pull(Consumer가 주체) : consumer가 자기 조건에 맞춰서 가져감
    - RabbitMQ : Push(Producer가 주체) : producer가 전달할 시기를 통제, 나머지는 메시지 전달 초점
    
| **특징**              | **Kafka**                                      | **RabbitMQ**                                 |
|-----------------------|------------------------------------------------|----------------------------------------------|
| **주요 사용 용도**     | 실시간 데이터 스트리밍, 로그 처리, 이벤트 소싱 | 메시지 큐, 작업 분산, 비동기 메시지 처리     |
| **메시지 저장**       | 디스크에 영구 저장 가능 (내구성)               | 메모리 중심, 디스크 사용 옵션 존재           |
| **처리 속도**         | 고속 대규모 데이터 처리 가능                   | 소규모 메시지 처리, 작업 분산에 적합         |
| **메시지 소비**       | 여러 소비자가 메시지를 여러 번 읽을 수 있음    | 큐에 있는 메시지를 한 번만 소비 가능         |
| **확장성**            | 매우 높은 확장성 (분산 아키텍처)               | 중간 정도의 확장성                          |
| **순서 보장**         | 파티션 내에서 메시지 순서 보장                  | 순서 보장은 특정 설정에 따름                 |
| **내구성**            | 메시지를 영구히 보관 (지정 기간 동안)           | 메시지의 내구성 보장이 상대적으로 약함       |




## 도움되는 툴
- CMAK(Kafka Manager) : Cluster 모니터링, Topic CRUD, partition추가/리밸런싱,Leader변경, Consumer조회

