package com.forgather.domain.export.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.forgather.global.exception.InvalidConfigurationException;

class SpacePdfLambdaConfigTest {

    @DisplayName("PDF 람다 설정이 누락되면 InvalidConfigurationException으로 빈 생성이 실패한다")
    @Test
    void failsBeanCreationWithInvalidConfigurationAsRootCause() {
        new ApplicationContextRunner()
            .withUserConfiguration(SpacePdfLambdaConfig.class)
            .withBean(SpacePdfExportProperties.class, () -> new SpacePdfExportProperties(null, null))
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasRootCauseExactlyInstanceOf(InvalidConfigurationException.class);
            });
    }
}
