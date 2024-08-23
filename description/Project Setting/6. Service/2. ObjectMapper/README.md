## 객체변환

1. ObjectMapper
  - Jackson 라이브러리
  - JSON과 Java 객체 간의 직렬화 및 역직렬화
  - 장점 : 빠른개발속도, 범용성
  - 단점 : 런타임에 작업됨, 타입안정성부족
```java
ObjectMapper objectMapper = new ObjectMapper();
String jsonString = objectMapper.writeValueAsString(someObject); // Java 객체를 JSON 문자열로 변환
SomeObject obj = objectMapper.readValue(jsonString, SomeObject.class); // JSON 문자열을 Java 객체로 변환
```
- source data를 이식하면서 target Class를 new생성 
```java
public static <T> T copyVO(Object fromValue, Class<T> toValueType) {
    try {
        // 새로운 인스턴스를 생성
        T targetInstance = toValueType.getDeclaredConstructor().newInstance();
        // fromValue를 targetInstance로 복사
        objectMapper.updateValue(targetInstance, fromValue);

        return targetInstance;
    } catch (Exception e) {
        throw new RuntimeException("VO copy failed", e);
    }
```

2. Mapstruct
  - 타입 간의 매핑(예: DTO와 엔티티 간)을 처리하는 컴파일러 기반의 매핑 프레임워크
  - 장점 : 컴파일 타임 매핑, 타입 안정성
  - 단점 : 개발어려움
```java
@Mapper
public interface PersonMapper {
    PersonMapper INSTANCE = Mappers.getMapper(PersonMapper.class);

    // PersonDTO를 Person 엔티티로 변환
    Person toEntity(PersonDTO dto);

    // Person 엔티티를 PersonDTO로 변환
    PersonDTO toDto(Person entity);
}
```