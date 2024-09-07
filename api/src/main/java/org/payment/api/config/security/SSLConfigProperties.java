package org.payment.api.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server.ssl")
@Getter
@Setter
public class SSLConfigProperties {
    private String keyStore;
    private String keyStorePassword;
    private String keyAlias;
    private String keyPassword;
    private String keyStoreType;
}
