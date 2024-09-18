package org.payment.api.payments.service.pg;

public interface PGConfig {
    String getBaseUrl();
    String getSecretKey();
    String getAuthorizationType();
    String getContentType();
}
