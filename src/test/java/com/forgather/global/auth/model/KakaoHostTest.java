package com.forgather.global.auth.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseException;

class KakaoHostTest {

    @DisplayName("카카오 닉네임이 null이면 생성할 수 없다")
    @Test
    void throwExceptionWhenNameIsNull() {
        assertThatThrownBy(() -> new KakaoHost(new Host(null, "pictureUrl"), "kakao-user-id", null))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("카카오 닉네임");
    }
}
