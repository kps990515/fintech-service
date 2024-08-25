package org.payment.db.config.rdb;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "org.payment.db")
@EntityScan(basePackages = "org.payment.db")
public class JpaConfig {
}
