package org.payment.api.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "toss.payments")
@RequiredArgsConstructor
public class TossPaymentsConfig {

    private final String baseUrl;
    private final String secretKey;
    private final String authorizationType;
    private final String contentType;
}
