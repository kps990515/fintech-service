## Filter
- 동작위치 : Spring Context 외부에서 동작(Web서버쪽)
- 스프링부트부터는 내장 웹서버가 있어 관리가 가능해짐
- 용도 : 스프링과 무관한 작업 & request/reponse 처리작업 & 전역처리필요작업
- 함수 : init(), doFilter(), destry()

### LoggerFilter
1. 용도 
   - HTTP 요청과 응답의 내용을 로깅  
  

2. 과정
   1. ContentCachingRequestWrapper와 ContentCachingResponseWrapper를 사용하여 요청과 응답의 내용을 캐싱
   2. Request Header 정보 : req.getHeaderNames() 정보들을 headerValues에 넣어서 노출
   3. Request 정보 : requestbody, uri, method req.get...으로 가져와서 노출
   4. Request와 같이 Response Header, 본문 추출 / 노출 



