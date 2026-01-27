package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.util.RandomCodeGenerator;

class AdminSessionTest {

    private RandomCodeGenerator randomCodeGenerator = new RandomCodeGenerator();

    @DisplayName("어드민 세션을 생성한다.")
    @Test
    void createAdminSession() {
        // given
        Long adminUserId = 1L;
        String username = "admin";

        // when
        AdminSession session = AdminSession.create(adminUserId, username, randomCodeGenerator);

        // then
        assertAll(
            () -> assertThat(session.getSessionIdValue()).isNotBlank(),
            () -> assertThat(session.getAdminUserId()).isEqualTo(adminUserId),
            () -> assertThat(session.getUsername()).isEqualTo(username),
            () -> assertThat(session.getCreatedAt()).isNotNull(),
            () -> assertThat(session.getLastAccessedAt()).isEqualTo(session.getCreatedAt()),
            () -> assertThat(session.getMaxInactiveIntervalSeconds()).isEqualTo(30 * 60)
        );
    }

    @DisplayName("세션 ID 값을 문자열로 반환한다.")
    @Test
    void getSessionIdValue() {
        // given
        AdminSession session = AdminSession.create(1L, "admin", randomCodeGenerator);

        // when
        String sessionIdValue = session.getSessionIdValue();

        // then
        assertThat(sessionIdValue).isEqualTo(session.getSessionId().getValue());
    }
}
