package com.forgather.domain.export.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class SpacePdfLambdaConfig {

    @Bean
    public LambdaClient spacePdfLambdaSdkClient(SpacePdfExportProperties properties) {
        properties.validateConfigured();
        return LambdaClient.builder()
            .region(Region.of(properties.region()))
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(Duration.ofSeconds(3))
                .apiCallTimeout(Duration.ofSeconds(10))
                .build())
            .build();
    }
}
