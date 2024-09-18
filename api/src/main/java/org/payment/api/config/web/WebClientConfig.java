package org.payment.api.config.web;

import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.payment.api.payments.service.pg.PGConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(PGConfig pgConfig) {
        // Connection Provider 설정
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(500) // 최대 연결 수 설정
                .pendingAcquireTimeout(Duration.ofSeconds(60)) // 응답 대기 시간 설정
                .maxIdleTime(Duration.ofSeconds(20)) // 유휴 시간 설정
                .build();

        // HttpClient 설정
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 서버 연결 타임아웃 설정
                .responseTimeout(Duration.ofSeconds(5)) // 서버 응답 타임아웃 설정
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5)) // 데이터 read
                                .addHandlerLast(new WriteTimeoutHandler(5))) // 데이터 write
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL); // 요청/응답 로그 출력 (디버깅용)

        // WebClient 빌더에 HttpClient 적용
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(pgConfig.getBaseUrl())
                .build();
    }
}

