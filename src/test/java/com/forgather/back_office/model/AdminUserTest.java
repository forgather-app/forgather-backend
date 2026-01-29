package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.forgather.fixture.AdminUserFixture;

class AdminUserTest {

    @DisplayName("평문 비밀번호와 암호화된 비밀번호 일치 여부를 확인한다.")
    @CsvSource(value = {"password, true", "PASSWORD, false"})
    @ParameterizedTest
    void checkPassword(String password, boolean expected) {
        // given
        AdminUser adminUser = AdminUserFixture.createAdminUser();

        // when
        boolean result = adminUser.checkPassword(password, new BCryptPasswordEncoder());

        // then
        assertThat(result).isEqualTo(expected);
    }

}
