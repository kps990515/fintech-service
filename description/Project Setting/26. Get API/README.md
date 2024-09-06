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
  return Mono.defer(() -> webClient.get()
                  .uri(uriBuilder -> {
                      uriBuilder
                              .path(tossPaymentsConfig.getBaseUrl() + "/v1/transactions")
                              .queryParam("startDate", requestVO.getStartDate())
                              .queryParam("endDate", requestVO.getEndDate());
                      // Optional parameters 처리
                      if (requestVO.getStartingAfter() != null) {
                          uriBuilder.queryParam("startingAfter", requestVO.getStartingAfter());
                      }
                      if (requestVO.getLimit() > 0) { // 기본값을 0으로 가정하고 양수인 경우에만 설정
                          uriBuilder.queryParam("limit", requestVO.getLimit());
                      }
                      return uriBuilder.build();
                  })
                  .header("Authorization", tossPaymentsConfig.getAuthorizationType() + " " + encodedAuth)
                  .retrieve()
                  .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), this::handleError)
                  .bodyToMono(new ParameterizedTypeReference<List<TransactionVO>>() {})
          )
          .transformDeferred(RetryOperator.of(retry)) // Retry 적용 (먼저)
          .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)) // Circuit Breaker 적용 (나중)
          .subscribeOn(Schedulers.boundedElastic()) // 비동기 작업을 boundedElastic 스케줄러에서 처리
          .doOnError(throwable -> {
              if (!(throwable instanceof RuntimeException)) {
                  log.error("Transaction API 에러 발생", throwable); // handleError에서 처리하지 않은 에러 처리
              }
          });
}
```
- 장점 : 동적 파라미터를 유연하게 처리하기 쉬움
- 단점 : 코드가 복잡함