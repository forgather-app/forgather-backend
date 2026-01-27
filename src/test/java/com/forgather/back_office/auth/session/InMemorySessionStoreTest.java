package com.forgather.back_office.auth.session;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.back_office.model.AdminSession;
import com.forgather.global.util.RandomCodeGenerator;

class InMemorySessionStoreTest {

    @DisplayName("인메모리 기반의 세션 저장소에 어드민 세션 정보를 저장한다.")
    @Test
    void saveAdminSession() {
        // given
        SessionStore sessionStore = new InMemorySessionStore();
        AdminSession adminSession = AdminSession.create(1L, "username", new RandomCodeGenerator());

        // when & then
        assertThatCode(() -> sessionStore.save(adminSession))
            .doesNotThrowAnyException();
    }
}
