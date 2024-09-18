## MDC(Mapped Diagnostic Context)
- 로그 메시지에 특정한 컨텍스트 정보를 추가하기 위해 사용되는 기능
- 사용자 요청에 고유ID를 넣어 로그메시지에 추가 -> 추적
- MSA, 멀티스레드 환경에서 유용

### LoggerFilter VS MDC Filter(MDC가 먼저되야함)
- MDC : 각 요청에 고유한 식별정보를 설정(클라이언트ID, IP 등)
- Logger : 요청, 응답에 대한 세부정보 기록 후 로그로 처리
- 즉 MDC는 로그에 포함될 정보를 설정 -> Logger가 해당 정보를 기록
- 순서의 중요성
  - MDC는 설정의 역할
  - LoggerFilter는 기록의 역할
  - LoggerFilter가 먼저되면 MDC 정보의 설정이 빠진채로 기록되게 됨

## 방법
1. logback.xml 패턴 추가
- transactionId, clientIp, requestURI
- thread별로 할당, ThreadLocal변수로 사용, Thread종료 시 MDC가 다 사라짐
- clear를 하는 이유는 threadPool을 사용해서 thread 재사용시 같은 데이터를 사용가능성 있음, Memory누수 방지
```xml
<encoder>
    <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36}
        - %msg [transactionId=%X{transactionId}] [clientIp=%X{clientIp}] [requestURI=%X{requestURI}]%n</pattern>
    <charset>UTF-8</charset> <!-- 인코딩 설정 추가 -->
</encoder>
```

  
2. Filter에 MDC 설정
```java
public class MDCFilter implements Filter {

    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final String CLIENT_IP_KEY = "clientIp";
    private static final String REQUEST_URI_KEY = "requestURI";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

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
3. FilterConfig로 순서 조정
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
    registrationBean.setOrder(2);  // MDCFilter 다음에 실행되도록 설정
    return registrationBean;
  }
} 
```