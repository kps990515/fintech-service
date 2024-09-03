## GetApi

### @ModelAttribute VS @RequestParam
1. @ModelAttribute
   - 여러 쿼리 파라미터를 하나로 묶어줌
   - VO이기때문에 객체 전체에 대해 유효성 검사 가능
  

2. @RequestParam
   - 단일 파라미터 처리에 유용
   - 기본값 설정 가능
   - 필수 여부 설정가능

### WeblClient URI설정
1. 단순 문자열을 사용한 URI 설정(Post에서 많이 사용)
```java
.uri(tossPaymentsConfig.getBaseUrl() + "/payments/confirm")
```
- 장점 : URI가 고정일떄 단순, 직관적
- 단점 : 동적 파라미터 처리 어려움
  
2. URI 빌더를 사용한 동적 URI 설정(Get에서 많이 사용)
```java
.uri(uriBuilder -> {
        uriBuilder
        .path("/v1/transactions")
        .queryParam("startDate", requestVO.getStartDate())
        .queryParam("endDate", requestVO.getEndDate());
     return uriBuilder.build();
}) 
```
- 장점 : 동적 파라미터를 유연하게 처리하기 쉬움
- 단점 : 코드가 복잡함