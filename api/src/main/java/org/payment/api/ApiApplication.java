package org.payment.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"org.payment.api", "org.payment.db", "org.payment.alarm",
                                            "org.payment.batch", "org.payment.common"})
@ConfigurationPropertiesScan
@EnableRedisRepositories(basePackages = "org.payment.db")
@EnableAsync
public class ApiApplication {
    public static void main(String[] args){
        SpringApplication.run(ApiApplication.class, args);
    }
}
