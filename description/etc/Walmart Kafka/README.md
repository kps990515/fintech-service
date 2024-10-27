## Walmark Kafka

### Kafka Consumer Group 개요
- Consumer Group : Kafka 토픽에서 읽기 위해 협력하는 소비자(Consumer)들의 집합
- Kafka Topic : 파티션으로 구분
- 각 파티션은 하나의 Consumer Group 내에서 하나의 소비자만이 소비가능. 이를 통해 메시지가 중복 처리되지 않도록 보장

### Walmart에서 발생한 Kafka의 문제들
#### 1. Consumer Rebalancing 문제
- Consumer Group 내의 소비자 수가 변경될때 발생. 
- Consumer Rebalancing 과정에서 지연, 중단 발생
- 주요 발생상황
  - 소비자가 그룹에 들어오거나 나갈 때
    - Kubernetes 배포, 롤링 재시작, 자동 확장 등으로 인해 Consumer Pod가 그룹에 추가되거나 제거될 때 발생
  - Consumer가 실패했다고 Kafka 브로커가 판단할 때
    - 세션 타임아웃 내에 브로커가 소비자로부터 하트비트를 받지 못하면, 브로커는 해당 소비자가 죽었다고 판단
    - 소비자의 JVM이 종료되거나, 긴 **GC(Garbage Collection)**로 인해 일시 중지된 경우 발생가능
  - Consumer가 응답이 없다고 Kafka 브로커가 판단할 때
    - Consumer가 다음 배치를 가져오는 데 시간이 오래 걸리면 브로커는 해당 Consumer가 응답이 지연되었다고 간주하고 재균형을 시작
- Consumer Rebalancing의 필요성 및 문제점
  - 필요성 : 재균형은 파티션이 모든 소비자에게 고르게 분배되도록 보장하기 위해 필요
  - 문제점 : 지연이나 중단이 발생가능

#### 2. Poison Pill 메시지 : Consumer의 실패를 유발하는 메시지
- 주요원인
  - 잘못된 데이터: 페이로드, JSON 형식이 잘못된 경우
  - 예상치 못한 데이터 : 로직에 맞지 않는 값 포함, 정의 규칙에 어긋난 메시지
  - 소비자 코드의 버그 : Null값 등
- Poison Pill 메시지가 발생했을 때의 문제점
  - 무한루프 : 소비자는 매번 동일한 예외를 만나게 되고, 무한 루프에 빠지게 됨
  - 처리 정체: 하나의 잘못된 메시지 때문에 소비자는 파티션 내의 다른 메시지를 처리하지 못하게됨
    - 네트워크의 헤드 오브 라인 블로킹과 비슷 : 킷의 첫 번째가 막히면서 뒤에 있는 패킷들까지 영향을 받는 상황
- 영향도
  - 소비자 장애
  - 시스템 정체
- 해결법
  - DLQ(Dead Letter Queue) 사용 : 특정 횟수 이상 재시도 후에도 처리할 수 없을 때, DLQ에 전달
  - 예외 처리 개선
  - 메시지 유효성 검사 추가

#### 3. Kafka 파티션과 소비자 간의 강한 결합
- Kafka는 하나의 파티션을 동일한 Consumer Group 내의 하나의 소비자에게만 할당
- 따라서, 병렬로 메시지를 처리하려면 파티션의 수가 소비자 인스턴스 수를 결정
- 소비자 애플리케이션의 확장을 시도하면 문제가 발생

- 소비자 애플리케이션의 확장 시 문제점
  - 파티션 수와 소비자 수의 제약 : 각 파티션은 하나의 소비자에게 할당, 소비자를 추가해도 할당될 파티션이 없음 -> 파티션을 늘려야함
  - 파티션 수 증가의 문제점
    - Kafka 브로커의 파티션 제한 : 브로커당 권장 파티션수는 4000개, 계속 증가시키면 제한 도달
    - 더 큰 브로커 인스턴스로 확장 필요 : 비용증가 문제 발생
    - 조정 비용 증가 : 파티션 수를 늘리면 Kafka 팀, 프로듀서 팀, 소비자 팀 간의 조정필요 -> 관리 오버헤드 발생
  - 더 많은 파티션으로 인해 리소스 사용량 증가
    - 더 많은 파일 핸들필요
    - 메모리 사용량이 증가하며, 파티션당 스레드 수 증가

- 해결책
  - 파티션 수 최적화
  - 소비자 처리 최적화 : 파티션 수 늘리지 않고 배치 처리, 멀티 스레딩 사용
  - 브로커 리소스 모니터링 : 파티션 수 증가가 브로커의 리소스 사용량에 미치는 영향 모니터링

### MPS(Message Proxy Service)로의 Kafka 문제 해결
- MPS : Kafka 메시지 소비와 Kafka의 파티션 기반 모델 제약을 분리하기 위해 설계된 서비스
  - 동작방식
    - MPS는 Kafka 브로커와 소비자 애플리케이션 간의 프록시 역할을 수행
      - MPS는 Kafka의 각 파티션에서 메시지를 읽어와 별도의 메모리 큐에 저장
      - 소비자 애플리케이션이 Kafka의 파티션에 직접 연결되지 않고, Kafka와의 강한 결합을 피할 수 있음
    - 소비자 애플리케이션은 Kafka에서 직접 메시지를 읽지 않음
      - MPS를 통해 HTTP/REST API를 사용하여 메시지를 수신
      - 소비자 애플리케이션이 Kafka 파티션 수와 독립적으로 확장가능
    - MPS 주요기능
      - 키(key) 단위로 메시지의 순서 유지
      - 소비자 애플리케이션 장애 처리
      - 오프셋 커밋 관리

  - 장점
    - 확장성
      - 소비자 애플리케이션은 Kafka의 파티션 수에 제한받지 않고 독립적으로 확장가능
      - MPS가 Kafka와 소비자 애플리케이션 사이의 브리지 역할을 하기 때문에, Kafka 파티션 수를 늘리지 않고도 더 많은 소비자 인스턴스를 추가가능
    - 유연한 메시지 처리
      - MPS는 메모리 큐를 사용하여 메시지를 관리하기 때문에, 특정 상황에서 메시지 처리 순서를 유연하게 조정가능
      - 소비자 애플리케이션이 과도한 메시지 처리 부하로부터 보호
    - 소비자 애플리케이션의 실패 복구
      - MPS는 메시지를 메모리에 저장하여 소비자 애플리케이션의 실패를 대비합니다. 만약 소비자가 실패하더라도 MPS는 해당 메시지를 보존하고 재전송가능

### MPS 상세구조
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2F2d8ac999-99eb-4672-ae8b-0bf3193ad818_1600x1091.png)
- 전체구조
  - 메시지의 순서 보장 : 동일한 키에 대해 메시지 순차 처리
  - 메모리 관리 및 부하 분산 : PendingQueue의 크기를 제한하여 메모리 사용을 관리하고, 백프레셔를 적용해 부하를 조절
  - 오프셋 관리 및 커밋: 오프셋을 추적하고 주기적으로 Kafka에 커밋하여 중복 처리를 방지
  - DLQ 관리: 실패한 메시지는 DLQ에 저장되어 후속 분석이나 재처리


1. Message Reader Thread
    - 역할 : Kafka 브로커에서 메시지를 읽어와 PendingQueue라는 대기 큐에 저장
    - 동작
      - 메시지를 Kafka 브로커에서 가져옵니다.
      - PendingQueue가 최대 용량에 도달하면 Kafka에서 메시지 읽기를 일시 중지(Backpressure)합니다. 이는 큐가 무한정 커지지 않도록 메모리 사용량을 조절
2. PendingQueue (Bounded Buffer Queue)
    - 역할 : Reader Thread와 Writer Threads 사이에서 메시지를 저장하는 버퍼 역할
    - 특징
      - 큐의 크기는 제한적(Bounded)이며, 메모리 사용을 관리하는 데 도움
      - Reader는 Kafka에서 빠르게 메시지를 가져올 수 있고, Writer는 각기 다른 속도로 메시지를 처리가능
3. Order Iterator
   - 역할 : 동일한 키(key)를 가진 메시지가 순서대로 처리되도록 보장
   - 동작
     - PendingQueue에서 메시지를 확인하고, 이미 동일한 키를 가진 이전 메시지가 처리 중이라면 이를 건너뜁니다.
     - 이로 인해 한 번에 하나의 키에 대해서만 메시지가 처리
4. Writer Threads
    - 역할 : PendingQueue에서 메시지를 가져와 소비자 애플리케이션에 HTTP POST 요청을 통해 전달
    - 동작
      - POST 요청이 실패하면 재시도를 수행
      - 재시도가 모두 실패하거나 특정 HTTP 코드를 받으면 해당 메시지를 **DLQ(Dead Letter Queue)**에 넣습니다.
      - 성공적으로 처리된 오프셋을 추적하고, 오프셋을 커밋하기 위해 공유 데이터 구조를 업데이트
5. Offset Commit Thread
    - 역할 : Kafka에서 처리된 메시지의 오프셋을 주기적으로 커밋
    - 동작
      - 일정 주기마다 깨워져(예: 1분마다) Writer Threads가 업데이트한 공유 데이터 구조를 확인
      - 각 파티션에서 연속적으로 처리된 최신 오프셋을 커밋합니다. 예를 들어, 오프셋 1, 2, 3, 5가 처리되었다면 오프셋 3까지만 커밋합니다(오프셋 4가 누락됨).
    - 효과 : MPS가 처리한 메시지를 Kafka에 알리고, MPS가 중단되거나 재시작될 때 마지막 커밋된 오프셋부터 메시지 처리를 재개하여 중복 처리를 방지
6. Consumer Service REST API
    - 역할 : 실제 소비자 애플리케이션이 MPS로부터 메시지를 수신하기 위한 인터페이스를 정의
    - 포맷 : MPS의 Writer Threads가 HTTP POST 요청을 보내기 위해 필요한 헤더, 본문 형식 등을 정의

### Kafka Connect를 활용한 MPS 구현
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2F27f0fe55-d1e0-4e2b-92a4-6cfd2236fd75_1600x999.png)
- Kafka Connect : Kafka와 외부 시스템(DB, Key-Value, 검색, 파일시스템)을 연결하기 위한 프레임워크

#### 주요컴포넌트
1. Connector Instance
    - Source Connector: 외부 시스템에서 데이터를 읽어와 Kafka로 보냅니다.
    - Sink Connector: Kafka에서 데이터를 읽어 외부 시스템으로 보냅니다.
    - MPS는 Sink Connector로 구현되어, Kafka에서 메시지를 읽어 소비자 애플리케이션으로 전달
2. Transform : 데이터 변환을 담당하는 컴포넌트입니다. Kafka에서 들어오거나 나가는 데이터에 대해 형식을 변환하거나 가공(JSON 등)
3. Converter: 데이터의 직렬화 및 역직렬화를 처리

#### MPS를 Kafka Connect(Sink Connector)로 구현한 이유와 이점
1. 멀티 테넌시 (Multi-tenancy) 지원(하나의 시스템이나 애플리케이션을 여러 사용자가 공유하는 개념)
    - Kafka Connect는 하나의 클러스터에서 여러 개의 커넥터(Connector)를 실행가능
    - 이를 통해 하나의 MPS 인스턴스가 여러 개의 소비자 애플리케이션에 데이터를 제공가능
2. DLQ (Dead Letter Queue) 처리
    - Kafka Connect는 처리할 수 없는 메시지에 대한 내장 DLQ 처리를 지원
3. 오프셋 커밋 (Offset Commits) : 오프셋을 지원해서 중복 처리 방지
4. 확장성 : Kafka의 소비자 인스턴스와 별도로 MPS와 소비자 애플리케이션을 확장가능

#### 소비자 애플리케이션의 설계 원칙 : Stateless
- MPS와 연결된 소비자 애플리케이션은 상태를 로컬에 저장하지 않으며, 필요한 모든 상태 정보는 메시지와 함께 전달되거나 외부 데이터베이스에 저장
- 이로 인해 소비자 애플리케이션은 쉽게 확장이 가능하며, 메시지 양이 증가하면 Kubernetes가 더 많은 인스턴스를 자동으로 시작가능
- 반대로, 메시지 양이 줄어들면 Kubernetes가 인스턴스를 종료하여 리소스를 절약

#### 독립적인 확장성
- 소비자 애플리케이션의 인스턴스 수는 MPS와 독립적으로 확장가능
- MPS는 소비자 애플리케이션의 수와 관계없이 Kafka에서 메시지를 계속 수신하고 전달하기 때문에, 전체 시스템이 유연하고 효율적으로 확장


### 추가적 고려사항
1. MPS의 리밸런싱 문제 (Rebalancing of the MPS)
    - MPS는 Kafka 소비자 역할을 하기 때문에, Kafka의 일반 소비자처럼 **리밸런싱(rebalancing)**의 영향을 받음
    - MPS 리밸런싱 처리방법
      - Reader Thread(Kafka 메시지읽기)와 Writer Threads(Consumer 전달)를 분리하여 리밸런싱을 보다 안정적으로 처리
      - PendingQueue라는 버퍼를 사용하여 Reader Thread와 Writer Threads 사이의 속도 차이를 완화
2. REST API 선택 이유
    - MPS는 소비자 인스턴스가 노출한 REST API를 호출하여 메시지를 전달
    - 단순성, 호환성
3. 복잡성 증가

### MPS 도입효과
1. 리밸런싱 문제 해결
    - Kafka 브로커가 소비자가 응답이 없는 상태로 인식할 때 발생하는 리밸런싱을 방지하는 데 효과적
    - Reader Thread는 할당된 시간 내에 모든 메시지를 PendingQueue에 저장하므로, Kafka 브로커가 소비자가 멈춘 것으로 간주하지 않음
2. Poison Pill 메시지 처리 개선
    - 소비자 서비스는 특정 HTTP 응답 코드(600 및 700)를 사용하여 Poison Pill 메시지를 MPS에 전달
    - MPS는 문제가 있는 메시지를 식별하고 **DLQ(Dead Letter Queue)**로 이동
3. 비용 절감 효과
    - 소비자 서비스의 상태 비저장 : 소비자 서비스는 상태를 유지하지 않기 때문에 Kubernetes에서 수요에 따라 쉽게 확장가능
    - Kafka 클러스터 확장 기준의 변화 : 파티션 수가 아닌 처리량에 맞춰 확장가능