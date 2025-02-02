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

```java
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<MDCFilter> mdcFilter() {
        FilterRegistrationBean<MDCFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new MDCFilter());
        registrationBean.setOrder(1);  // 가장 먼저 실행되도록 설정
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<LoggerFilter> loggerFilter() {
        FilterRegistrationBean<LoggerFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LoggerFilter());
       // MDCFilter 다음에 실행되도록 설정(MDC 정보를 활용하기 위해)
        registrationBean.setOrder(2);  
        return registrationBean;
    }
}
```

```java
public class MDCFilter implements Filter {

    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final String CLIENT_IP_KEY = "clientIp";
    private static final String REQUEST_URI_KEY = "requestURI";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            // Unique transaction ID 생성 및 MDC에 추가
            String transactionId = "TX-" + UUID.randomUUID();
            MDC.put(TRANSACTION_ID_KEY, transactionId);
            MDC.put(CLIENT_IP_KEY, request.getRemoteAddr());
            MDC.put(REQUEST_URI_KEY, ((HttpServletRequest) request).getRequestURI());

            // 다음 필터 체인으로 요청 전달
            chain.doFilter(request, response);
        } finally {
            // 요청이 완료되면 MDC에서 값 제거
            MDC.clear();
        }
    }
}
```

```java
@Slf4j
public class LoggerFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 요청과 응답의 내용을 캐싱
        var req = new ContentCachingRequestWrapper((HttpServletRequest) request);
        var res = new ContentCachingResponseWrapper((HttpServletResponse) response);
 
        // 위쪽 코드의 필터 작업을 마친 후, 다음 필터 또는 최종 서블릿(Controller)로 전달
        // Client → Filter1 → Filter2 → Controller(Dispatcher Servlet)
        chain.doFilter(req, res);
        // 아래부터는 리턴된 값에 대한 처리

        // 1.request 정보
        var headerNames = req.getHeaderNames();
        var headerValues = new StringBuilder();

        // 1-1. 헤더의 정보 로깅
        headerNames.asIterator().forEachRemaining(headerKey -> {
            var headerValue = req.getHeader(headerKey);
            headerValues
                    .append("[")
                    .append(headerKey)
                    .append(" : ")
                    .append(headerValue)
                    .append("] ");
        });

        // 1-2. requestbody, uri, method 추출
        var requestBody = new String(req.getContentAsByteArray());
        var uri = req.getRequestURI();
        var method = req.getMethod();
        // 1-3. 로그로 남기기
        log.info(">>>>> uri : {}, method : {}, header : {}, body : {}", uri, method, headerValues, requestBody);

        // 2. response 정보
        var responseHeaderValues = new StringBuilder();
        res.getHeaderNames().forEach(headerKey -> {
            var headerValue = res.getHeader(headerKey);
            responseHeaderValues
                    .append("[")
                    .append(headerKey)
                    .append(" : ")
                    .append(headerValue)
                    .append("] ");
        });

        // 2-1. responsebody 내용 로깅
        var responseBody = new String(res.getContentAsByteArray());
        log.info("<<<<< uri : {}, method : {}, header : {}, body : {}", uri, method, responseHeaderValues, responseBody);

        res.copyBodyToResponse(); // 사용한 response다시 넣어주기
    }
}
```



