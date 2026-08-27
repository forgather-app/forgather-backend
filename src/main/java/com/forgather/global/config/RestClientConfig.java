package com.forgather.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Boot 자동구성 RestClient.Builder를 주입받아 http.client.requests 메트릭이 기록되게 한다.
     * <p>
     * requestFactory 설정은 반드시 유지한다. 이 줄을 빼면 Boot이 클래스패스를 탐지해
     * 전송 계층을 고르는데, Apache HttpClient5·Jetty·Reactor Netty가 없으므로
     * JDK HttpClient가 선택되어 리다이렉트·HTTP 버전·풀링 동작이 조용히 바뀐다.
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return builder.requestFactory(requestFactory).build();
    }
}
