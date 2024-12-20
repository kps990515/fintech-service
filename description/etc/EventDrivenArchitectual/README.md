## EDA
- 이벤트의 생성, 감지, 소비 및 반응을 중심으로 하는 소프트웨어 설계 접근 방식
- EDA는 서비스 간 결합도를 낮추고, 확장성을 높이며, 반응성을 개선
- 실시간 처리가 가능하며, 높은 데이터 볼륨을 효율적으로 처리가능

### 장점
- 향상된 내결함성 : 시스템의 일부 컴포넌트에 장애가 발생해도, 다른 부분은 계속 운영가능(비동기적으로 운영되기 때문에)
- 자원 활용도 개선 : 서비스가 독립적으로 확장할 수 있어, 수요에 따라 자원을 효율적으로 사용가능
- 동적이고 유연한 워크플로우 지원 : 워크플로우를 쉽게 변경하거나 새로운 이벤트를 추가하여 시스템을 업데이트가능

### 패턴
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2F0704e3e6-be46-4411-8c24-c72c77038d7e_1509x1600.png)
1. 경쟁 소비자 패턴 (Competing Consumer Pattern)
    - 여러 소비자(Consumer)가 동일한 메시지 큐를 구독하여 메시지를 처리하는 패턴
    - 메시지는 로드 밸런싱되어 각 소비자에게 분배
    - 장점 : 확장성이 뛰어나고 작업 부하를 효율적으로 분산
2. Consume and Project 패턴
    - 이벤트를 소비하고 이를 기반으로 새로운 Projection을 만들어내는 패턴
    - 예시 : Order Service에서 주문 생성 이벤트 게시 -> Order Projection Service가 구독 -> 고객 정보를 포함한 주문 데이터 생성 후 별도 저장
    - 장점 : 시스템의 일관성을 유지하면서 데이터를 빠르게 사용가능
3. 이벤트 소싱 패턴 (Event Sourcing)
    - 상태 변화를 이벤트 형태로 저장하여, 시스템의 상태를 과거로 되돌리거나 변경 이력을 추적할 수 있게 하는 패턴
    - 장점 : 데이터의 불변성과 추적을 지원하며, 복원력을 향상
4. 비동기 작업 실행 패턴 (Async Task Execution Pattern)
    - 작업을 비동기적으로 처리하여 시스템 성능을 최적화하는 패턴
    - 작업은 큐에 의해 처리되고, 여러 작업자가 작업을 수행
    - 장점 : 비동기 처리로 성능을 최적화하며, 시스템 병목을 줄임
5. 트랜잭션 아웃박스 패턴 (Transactional Outbox Pattern)
    - 트랜잭션을 통한 데이터베이스와 메시지 큐의 일관성을 보장하는 패턴
    - 트랜잭션 내에서 이벤트를 아웃박스 테이블에 저장한 후, 이를 메시지 큐로 전송
    - 예시 : Order Service가 주문 테이블과 아웃박스 테이블을 함께 업데이트한 후, 아웃박스 테이블을 읽어 메시지 큐로 전송합니다.
    - 장점 : 데이터 일관성과 메시지 전송의 신뢰성을 보장
6. 이벤트 집계 패턴 (Event Aggregation Pattern)
    - 작은 이벤트를 수집하여 더 큰 이벤트로 집계하는 패턴
    - 복잡한 비즈니스 로직을 단순화 가능
    - 예시: 고객 생성 이벤트는 여러 세부 이벤트(연락처 생성, 계정 생성, 주소 생성)를 합쳐 만들어집니다.
    - 장점: 복잡한 이벤트 처리를 단순화하고, 이벤트를 더 높은 수준으로 추상화
7. 사가 패턴 (Saga Pattern)
    - 분산 트랜잭션을 관리하기 위해 여러 서비스 간의 로컬 트랜잭션을 조정하는 패턴
    - 트랜잭션이 실패할 경우, 보상 작업을 수행하여 상태를 복원
    - 장점: 분산 시스템에서 데이터 일관성을 유지하면서 트랜잭션을 관리

### EDA의 핵심 구성 요소
- 이벤트 생성자 (Event Producers)
  - 이벤트 생성자는 시스템 내에서 특정 작업이나 상태 변화에 따라 이벤트를 생성
  - 생성된 이벤트는 이벤트 브로커에게 전달
- 이벤트 소비자 (Event Consumers):
  - 특정 유형의 이벤트를 구독하고, 이벤트를 수신하면 이를 처리하여 특정 작업을 수행
  - 이벤트 브로커로부터 이벤트를 받아 정의된 로직에 따라 작업을 수행
- 이벤트 브로커 (Event Broker)
  - 이벤트 브로커는 이벤트 생성자와 소비자 간의 중재자 역할
  - 이벤트를 수신하고, 각 소비자의 구독에 따라 적절한 이벤트를 라우팅
  - 이벤트가 신뢰성 있게 전달될 수 있도록 보장
- 이벤트 스트림 (Event Streams)
  - EDA에서는 이벤트가 이벤트 스트림으로 구성되며, 이벤트 생성자가 발생시키는 연속적인 이벤트의 시퀀스
  - 이벤트 스트림을 통해 실시간 데이터 처리가 가능하며, 시스템 내의 여러 컴포넌트 간의 비동기 통신 가능케 함

### EDA와 전통적인 요청-응답 아키텍처와의 차이점
- 동기식 vs. 비동기식
  - 요청-응답 모델에서는 클라이언트가 서버에 요청을 보내고 응답을 기다리는 동기식 통신
  - EDA에서는 이벤트가 비동기적으로 발생하며, 컴포넌트가 즉각적인 응답을 기다리지 않고 작업을 계속할 수 있음
  - 예시: Order Service가 주문 생성 이벤트를 발생시킨 후 즉시 다음 작업을 수행가능
- 강한 결합 vs. 느슨한 결합
  - 요청-응답 아키텍처는 클라이언트와 서버가 직접 통신하기 때문에 강한 결합
  - EDA는 이벤트 브로커를 통해 간접적으로 통신하므로, 서비스 간 느슨한 결합
- 확장성과 성능
  - 시스템이 복잡해질수록 요청-응답 아키텍처는 동기식 상호작용 때문에 확장성 문제가 발생
  - EDA는 비동기 처리와 컴포넌트의 독립적인 확장을 가능하게 하여, 더 나은 확장성과 성능을 제공
  - 예시: 각 서비스가 이벤트를 비동기적으로 처리하므로, Stock Service나 Shipping Service는 자신들의 페이스에 맞춰 확장가능

### 패턴
1. 경쟁 소비자 패턴(Competing Consumer Pattern)
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2Fdaec3dc5-7d4c-44c9-88a1-f615cf2b92f5_1600x970.png)
- 여러 소비자가 공유된 메시지 큐에서 메시지를 경쟁적으로 가져와 처리하는 방식
- 높은 처리량을 요구하는 시스템에서 확장성을 높이고 병렬 처리를 통해 성능을 향상시키는 데 유용
- 핵심개념
  - 공유 메시지 큐 (Shared Message Queue)
    - 메시지 큐는 중앙 저장소 역할을 하며, 메시지를 저장하고 소비자들이 이를 가져갈 수 있도록 관리
    - 이벤트 브로커(예: RabbitMQ)가 큐를 관리하며, 모든 이벤트가 이 큐에 쌓입니다
  - 경쟁 소비자 (Competing Consumers)
    - 여러 소비자(Consumer)들이 동일한 메시지 큐를 구독하여, 큐에 메시지가 도착할 때 이를 경쟁적으로 가져가 처리
    - 각 소비자는 메시지를 독립적으로 가져와서, 자신만의 로직에 따라 처리
  - 동적 로드 밸런싱 (Dynamic Load Balancing)
    - 메시지 큐에 있는 메시지 수가 증가하면 추가 소비자를 추가하여 부하를 분산가능
    - 반대로, 큐의 부하가 줄어들면 소비자를 제거하여 자원을 절약
  - 내결함성 (Fault Tolerance)
    - 한 소비자가 실패하거나 사용할 수 없게 되더라도, 다른 소비자들이 큐에서 메시지를 계속 처리가능
    - 시스템이 개별 컴포넌트의 장애에도 작동할 수 있으며, 시스템의 신뢰성이 높아짐
    
- 고려사항
  - 메시지 순서 : 바른 순서로 메시지를 처리할 수 있는 추가적인 메커니즘이 필요(파티셔닝, Sequence기법)
  - 멱등성 : 소비자 실패나 재시도로 인해 메시지가 중복 처리될 수 있으므로, **멱등성(idempotency)**을 보장 필요

2. Consume and Project 패턴
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2Fb67fcd55-f785-4172-9aac-023ec133ffa0_1600x970.png)
- 이벤트 스트림을 통해 데이터의 실물화된 뷰(Materialized View)를 생성하는 전략
- 데이터를 실물화된 뷰로 구성하여, 특정 쿼리에 맞춘 최적화된 데이터를 제공
- 이를 통해 시스템의 성능을 높이고, 복잡한 쿼리 작업을 원래 시스템에서 분리함으로써 레거시 시스템의 부하를 줄임
- 주요개념
  - 이벤트 스트리밍 (Event Streaming)
    - 주요 데이터베이스에서 변경된 데이터를 이벤트 스트림을 통해 이벤트 브로커로 전송하면서 시작
  - 쓰기 전용 서비스 (Write-Only Service)
    - **"쓰기 전용 서비스"**라고 불리는 서비스가 이러한 이벤트를 소비
    - 이벤트를 통해 특정 뷰를 생성하고, 데이터를 별도의 데이터베이스에 저장
  - 맞춤형 뷰 (Tailored Views)
    - 생성된 실물화된 뷰는 특정 클라이언트 서비스의 쿼리 요구에 맞게 최적화 -> 특정 쿼리 부하 감소
    - 예시: Order Projection Service는 고객 정보를 Customer Service에서 가져와 주문 데이터를 만들어, Customer-Order Materialized View로 저장
- 작동흐름
  - 데이터 변경 스트리밍 : 주 데이터베이스에서 발생한 데이터 변경이 이벤트 스트림을 통해 이벤트 브로커로 전달
  - 이벤트 소비 및 프로젝션 생성 : 쓰기 전용 서비스가 이벤트를 소비하고, 특정 데이터 뷰를 별도의 데이터베이스에 저장
  - 클라이언트 서비스의 쿼리 처리 : 클라이언트 서비스는 원래 시스템 대신, 최적화된 뷰를 쿼리
- 장점
  - 데이터 접근 최적화 : 특정 클라이언트 서비스의 쿼리 요구에 맞게 맞춤형 뷰를 생성
  - 레거시 시스템 부하 감소 : 읽기 요청을 실물화된 뷰에서 처리하므로, 레거시 시스템에 대한 직접 쿼리가 줄어들어 부하가 감소
  - 비동기적 데이터 처리 : 데이터 변경 사항이 이벤트로 처리되어 비동기적으로 처리되기 때문에, 시스템의 성능이 향상

3. 이벤트 소싱(Event Sourcing) 패턴
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2F6cacfb7d-7443-4af5-80ee-81b646587ed4_1600x1029.png)
- 애플리케이션의 현재 상태를 저장하는 대신, 상태 변화를 이벤트의 시퀀스로 캡처하여 관리하는 아키텍처 패턴
- 모든 상태 변경을 추적할 수 있으며, 시스템의 상태를 특정 시점으로 복원가능
- 감사 로그가 중요한 시스템, 상태 변화 추적이 필요한 시스템에서 매우 유용

- 핵심개념
  - 이벤트 스토어 (Event Store)
    - 시스템 내에서 발생한 모든 작업을 기록하는 내구성 있는 로그
    - 데이터베이스와 메시지 브로커의 역할을 하며, 이벤트를 저장하고 새로운 이벤트를 추가하거나 기존 이벤트를 검색할 수 있는 API를 제공
    - 예시 : Order Created, Order Shipped, Order Cancelled와 같은 이벤트가 이벤트 스토어에 저장
  - 이벤트 (Events)
    - 상태 변화의 개별적인 단위
  - 상태 재구성 (State Reconstruction)
    - 이벤트 시퀀스를 재생하여, 특정 시점에서의 엔터티의 현재 상태를 복원가능
    - **시간적 쿼리(Temporal Queries)**를 가능하게 하며, 상태 변화의 전체 이력을 제공
    - 예시 : 이미지에서 Order Created, Order Shipped, Order Cancelled 이벤트를 순차적으로 재생하여 주문의 현재 상태를 알 수 있음
  - 읽기 데이터베이스 (Read Database)
    - 쿼리 목적으로 최종적으로 일관된 읽기 전용 데이터베이스를 유지
    - 시스템의 현재 상태를 빠르게 조회할 수 있도록 최적화
    
- 작동흐름
  - 이벤트 저장 : 애플리케이션에서 상태 변화가 발생하면, 해당 변화를 나타내는 이벤트가 이벤트 스토어에 기록
  - 상태 재구성: 이벤트 시퀀스를 재생하여 특정 시점의 상태를 재구성. 이를 통해 시간적 쿼리를 수행하거나 특정 시점으로 되돌아갈 수 있음
  - 읽기 데이터베이스 : 읽기 데이터베이스를 통해 현재 상태를 효율적으로 조회할 수 있으며, 이는 최종적으로 일관성을 유지

- 장점
  - 신뢰할 수 있는 감사 추적 (Reliable Audit Trail) : 모든 상태 변화를 이벤트로 캡처
  - 시간적 쿼리 (Temporal Queries) : 엔터티의 상태가 시간에 따라 어떻게 변했는지에 대한 복잡한 쿼리를 수행가능
  - 동시성 및 충돌 해결 : 이벤트가 순차적으로 저장되기 때문에, 분산 시스템에서 동시성을 관리하고 충돌을 해결하는 메커니즘을 제공

4. 비동기 작업 실행 패턴 (Asynchronous Task Execution Pattern)
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2Fdaf12fd5-c31d-49fa-af40-5a91f86ededd_1600x970.png)
- 일정 작업을 처리하는 동안 발생할 수 있는 일시적인 장애에도 작업이 결국 완료되도록 보장하는 방법
- 대규모 트래픽 처리, 스케줄링 작업, 시스템 장애 내구성을 필요로 하는 환경에서 매우 유용

- 주요구성요소
  - 작업 스케줄러 (Job Scheduler)
    - 처리해야 할 작업을 이벤트 브로커에 메시지로 생성하는 역할
    - 각 메시지는 스케줄된 작업을 나타내며, 작업의 실행을 요청
  - 이벤트 브로커 (Event Broker)
    - 메시지 큐의 역할을 하며, 작업 메시지를 저장하고 관리
    - Apache Kafka와 같은 메시지 큐가 자주 사용되며, 큐에 저장된 메시지는 작업자(Worker)들이 처리
  - 작업자 (Workers)
    - 작업자 인스턴스는 이벤트 브로커에서 메시지를 가져와 해당 작업을 처리하는 역할
    - 각 작업자는 독립적으로 작업을 처리하며, 특정 작업이 실패할 경우 재시도 규칙에 따라 다시 시도
  - 재시도 정책 (Retry Policy)
    - 작업이 일시적인 문제로 실패했을 때, 작업을 다시 시도하는 규칙을 정의

- 작동흐름
  - 메시지 생성 : 작업 스케줄러가 이벤트 브로커에 스케줄된 작업을 나타내는 메시지를 생성
  - 작업 처리 : 작업자는 이벤트 브로커에서 메시지를 가져와 작업을 시도
  - 실패 시 재시도 : 작업이 일시적인 문제로 실패하면, 재시도 정책에 따라 작업이 다시 시도

- 패턴 구현 시 고려사항
  - 재시도 정책 설계
  - 멱등성 :작업이 중복 실행되더라도 동일한 결과를 생성할 수 있도록 작업을 멱등성 있게 설계]

5. 이벤트 집계(Event Aggregation) 패턴
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2F54cdf96e-fdd9-4a14-b47e-5e5f60107e4e_1600x970.png)
- 여러 개의 관련된 세밀한 이벤트를 하나의 더 포괄적인 이벤트로 결합하는 방식
- 네트워크 부하를 줄이고, 이벤트 소비자의 이벤트 처리 과정을 단순화

- 핵심개념
  - 세밀한 이벤트 (Fine-Grained Events)
    - 시스템 내에서 개별적으로 발생하는 세밀한 이벤트입니다. 이러한 이벤트는 특정 작업이나 상태 변경을 포함
  - 이벤트 집계 서비스 (Event Aggregator Service)
    - 세밀한 이벤트를 수집하고 처리하여 **포괄적인 이벤트(Coarse-Grained Event)**를 생성
    - 세밀한 이벤트 간의 관계를 유지하면서, 이를 기반으로 새로운 이벤트를 만들어 하위 소비자에게 전달
  - 포괄적인 이벤트 (Coarse-Grained Event)
    - 여러 개의 세밀한 이벤트를 결합하여 생성된 이벤트
    - 이벤트 소비자가 이를 쉽게 처리할 수 있도록 생성

- 작동흐름
  - 세밀한 이벤트 발생
  - 이벤트 수집 및 집계
  - 포괄적인 이벤트 전송

- 장점
  - 네트워크 부하 감소
  - 이벤트 처리 단순화

6. SAGA패턴
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2Fa0284f9d-ee2d-4003-8e1b-bcdb2134823c_1600x993.png)
- 트랜잭션을 여러 개의 작은 독립적인 단계로 분리
- 각 단계는 개별 서비스에 의해 관리되며, 실패 시에는 **보상 작업(Compensating Actions)**을 통해 이전 변경 사항을 취소
- 최종적으로 일관된 상태를 보장

- 주요개념
  - 로컬 트랜잭션 (Local Transactions)
    - 각 단계는 단일 서비스 내에서 상태를 업데이트하는 로컬 트랜잭션으로 구성
    - 독립적으로 수행되며, 다른 서비스와 상호작용하지 않고 자체적으로 상태를 변경
  - 이벤트 기반 통신 (Event-Driven Communication)
    - 로컬 트랜잭션이 완료되면, 해당 서비스는 다음 단계를 트리거하는 이벤트를 게시
  - 보상 작업 (Compensating Actions)
    - 사가의 단계 중 하나가 실패할 경우, 이전 단계에서 수행된 변경 사항을 되돌리는 보상 작업이 실행
    - 이를 통해 시스템은 최종적으로 일관된 상태로 롤백 가능

- 장점
  - 서비스의 독립성과 확장성
  - 이벤트 기반 통신
  - 최종 일관성 (Eventual Consistency)
  - 장기적인 비즈니스 프로세스나 여러 서비스가 관여하는 트랜잭션에서 매우 유용

7. 트랜잭셔널 아웃박스 패턴 (Transactional Outbox Pattern)
![img](https://substackcdn.com/image/fetch/f_auto,q_auto:good,fl_progressive:steep/https%3A%2F%2Fsubstack-post-media.s3.amazonaws.com%2Fpublic%2Fimages%2Fad950eb2-83ff-4040-976f-f6443d0d1d0c_1600x991.png)
- 분산 시스템에서 상태 변경과 이벤트 게시의 원자성을 보장하기 위한 방법

- 핵심개념
  - 로컬 트랜잭션 (Local Transactions)
    - 비즈니스 데이터와 아웃박스 테이블을 동일한 데이터베이스 트랜잭션 내에서 업데이트
    - 서비스의 내부 상태와 이벤트를 게시할 준비가 된 상태를 원자적으로 유지
  - 아웃박스 테이블 (Outbox Table)
    - 게시해야 할 이벤트를 임시로 저장하는 공간
    - 데이터베이스 내에서 다른 비즈니스 데이터와 함께 관리되어, 트랜잭션이 커밋될 때 원자적으로 처리
  - 이벤트 게시 (Event Publication)
    - 별도의 프로세스나 스레드가 주기적으로 아웃박스 테이블을 스캔하여 새 이벤트를 확인하고, 이를 이벤트 브로커에 게시
    - 게시가 완료되면 해당 이벤트는 아웃박스 테이블에서 "전송됨"으로 표시되어 다시 게시되지 않도록 함

- 작동흐름
  - 단일 트랜잭션 내 작업 
    - 서비스가 트랜잭션을 처리할 때, 비즈니스 데이터를 업데이트하고 아웃박스 테이블에 이벤트를 삽입하는 작업을 하나의 데이터베이스 트랜잭션 내에서 수행
  - 트랜잭션 커밋 후 이벤트 확인
    - 트랜잭션이 커밋되면, 별도의 프로세스가 아웃박스 테이블을 스캔하여 새 이벤트를 확인
  - 이벤트 게시 및 상태 업데이트
    - 확인된 이벤트는 이벤트 브로커에 게시되며, 성공적으로 전송된 이벤트는 아웃박스 테이블에서 "전송됨"으로 표시되어 다음 스캔 시 제외

- 장점
  - 일관성 유지 : 비즈니스 데이터와 이벤트 게시를 하나의 트랜잭션으로 관리하여, 데이터 일관성을 유지
  - 신뢰할 수 있는 이벤트 전달 : 아웃박스 테이블로 이벤트가 유실되거나 중복되지 않도록 보장
  - e-커머스 플랫폼과 같이 주문, 결제, 배송과 같은 중요한 비즈니스 프로세스에서 유용
