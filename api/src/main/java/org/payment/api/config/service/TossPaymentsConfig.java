package org.payment.api.config.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.payment.api.payments.service.pg.PGConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "toss.payments")
@RequiredArgsConstructor
public class TossPaymentsConfig implements PGConfig {

    private final String baseUrl;
    private final String secretKey;
    private final String authorizationType;
    private final String contentType;
}
