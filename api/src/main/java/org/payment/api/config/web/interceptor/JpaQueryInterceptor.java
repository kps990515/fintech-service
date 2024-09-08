package org.payment.api.config.web.interceptor;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JpaQueryInterceptor implements StatementInspector {
    private static final Logger log = LoggerFactory.getLogger(JpaQueryInterceptor.class);

    @Override
    public String inspect(String sql) {
        long startTime = System.currentTimeMillis();  // SQL 시작 시간 기록
        String result = sql;  // SQL 문을 그대로 사용

        // SQL 실행 시간 측정
        long executionTime = System.currentTimeMillis() - startTime;
        if (executionTime > 2000) {  // 2초 이상 걸리는 쿼리 로깅
            log.warn("Slow Query detected: {} executed in {} ms", sql, executionTime);
        }

        return result;
    }
}
