package org.payment.api.config.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

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