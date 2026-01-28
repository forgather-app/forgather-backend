package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;
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

    @DisplayName("값이 null이거나 공백이면 세션 ID를 생성할 수 없다.")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @ParameterizedTest
    void fromInvalidValue(String value) {
        // when & then
        assertThatThrownBy(() -> SessionId.from(value))
            .isInstanceOf(BaseException.class)
            .hasMessage("세션 ID 값이 존재하지 않습니다.");
    }
}
