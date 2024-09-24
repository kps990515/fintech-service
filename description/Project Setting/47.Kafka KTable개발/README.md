## Kafka KTable 개발

### KStream VS KTable
- KStream(실시간이벤트스트림) : 각 레코드는 독립적으로, 모든 변경사항이 추가로 처리됨
- KTable(상태저장 및 집계) : 키-값 형태로 저장되며, 키에 대한 최종 값만을 유지

| **특징**               | **KStream**                                                      | **KTable**                                                |
|------------------------|------------------------------------------------------------------|------------------------------------------------------------|
| **데이터 처리 방식**    | 연속적인 **이벤트 스트림** 처리                                  | **상태 기반**으로 최종 값만 유지                           |
| **레코드 성격**         | 각 레코드는 **독립적인 이벤트**로 처리                            | **최종 상태**만 유지되며, 같은 키의 데이터는 덮어써짐       |
| **저장 방식**           | **모든 이벤트**를 그대로 유지                                     | **키-값 구조**로 상태를 저장하며, 최신 값만 유지            |
| **유형**                | **무한한 데이터 스트림**                                          | **유한한 상태 테이블**                                      |
| **사용 예시**           | 실시간 로그, 트랜잭션 스트림, 이벤트 기반 처리                     | 데이터 집계, 상태 저장, 최종 값 관리                        |
| **주요 사용 시나리오**  | 독립적인 이벤트 처리가 필요한 실시간 스트리밍                    | 키 기반 상태 저장, 변경 사항의 최종 결과만 유지해야 할 때   |

### 서비스(KStream과 유사)
```java
@Service
public class KTableService {

    @Autowired
    public void buildPipeline(StreamsBuilder sb) {

        KTable<String, String> leftTable = sb.stream("leftTopic", Consumed.with(Serdes.String(),Serdes.String())).toTable();
        KTable<String, String> rightTable = sb.stream("rightTopic", Consumed.with(Serdes.String(),Serdes.String())).toTable();
        //Inner Join
        ValueJoiner<String, String, String> stringJoiner = (leftValue, rightValue) -> {
            return "[StringJoiner]" + leftValue + "=" + rightValue;
        };

        KTable<String, String> joinedTable = leftTable.join(rightTable, stringJoiner);
        joinedTable.toStream().to("joinedMsg");
    }
}
``` 