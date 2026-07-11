package com.forgather.global.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final Duration APPLE_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration APPLE_READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    @Primary
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public RestClient appleRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(APPLE_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(APPLE_READ_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
