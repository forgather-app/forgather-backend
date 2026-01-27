package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.util.RandomCodeGenerator;

class SessionIdTest {

    private RandomCodeGenerator randomCodeGenerator = new RandomCodeGenerator();

    @DisplayName("랜덤 코드로 세션 ID를 생성한다.")
    @Test
    void generateSessionId() {
        // given
        SessionId sessionId = SessionId.generate(randomCodeGenerator);

        // when & then
        assertThat(sessionId.getValue()).isNotBlank();
    }

    @DisplayName("생성된 세션 ID는 매번 다른 값을 가진다.")
    @Test
    void generateUniqueSessionId() {
        // given
        SessionId first = SessionId.generate(randomCodeGenerator);
        SessionId second = SessionId.generate(randomCodeGenerator);

        // when & then
        assertThat(first.getValue()).isNotEqualTo(second.getValue());
    }
}
