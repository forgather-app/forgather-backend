package com.forgather.global.auth.dev;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 개발용 임시 로그인이 운영 환경에 노출되지 않는지 검증한다.
 * @Profile을 화이트리스트({"local","dev","test"})가 아닌 블랙리스트(!prod)로 바꾸는 사고를 막는 회귀 테스트다.
 */
@DisplayName("프로파일 가드: 개발용 임시 로그인")
class DevAuthProfileGuardTest {

    @DisplayName("prod 프로파일에서는 임시 로그인 빈이 등록되지 않는다")
    @Test
    void doNotRegisterDevLoginBeansWithProdProfile() {
        // given
        ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DevAuthController.class, DevAuthService.class)
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"));

        // when & then
        contextRunner.run(context -> assertThat(context)
            .doesNotHaveBean(DevAuthController.class)
            .doesNotHaveBean(DevAuthService.class));
    }
}
