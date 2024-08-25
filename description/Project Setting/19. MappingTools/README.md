## MappingTool

## 1. Mapstruct
### 특징
- 컴파일 타임 매핑
- 자바객체 특화로 타입 안정성
- 명시적 매핑 가능
- 매핑 정의 인터페이스 생성 시 -> 자동으로 구현체 생성됨

### 장단점
- 장점 : 컴파일타입 오류 감지, 다양한 커스터마이징
- 단점 : 초기 설정 및 복잡한 매핑시 까다로움

### 활용
```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    // Entity -> 자바객체
    @Mapping(source = "userId", target = "id")
    UserVO toUserVO(UserEntity userEntity);
    // 자바객체 -> Entity
    @Mapping(target = "joinedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "modifiedAt", expression = "java(java.time.LocalDateTime.now())")
    UserEntity toUserEntity(UserRegisterServiceRequestVO userRegisterServiceRequestVO);
}
```

## 2. ObjectMApper
### 특징
- 런타임 시점 매핑
- Json처리에 특화(여러 포맷 지원)

### 장단점
- 장점 : 다양한 포맷지원, Json매핑 쉬움
- 단점 : 런타임 오류 및 오버헤드 발생, 복잡한 매핑은 어려움

### 활용
- source객체정보를 복사해 target Class생성 후 데이터 이식
```java
LoginReponse response = ObjectConvertUtil.copyVO(userVO, LoginReponse.class);
```

