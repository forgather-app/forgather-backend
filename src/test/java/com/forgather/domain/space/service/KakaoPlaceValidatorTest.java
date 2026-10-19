package com.forgather.domain.space.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;

class KakaoPlaceValidatorTest {

    private final KakaoPlaceValidator validator = new KakaoPlaceValidator();

    @DisplayName("카카오 ID는 숫자 문자열이고 URL은 해당 ID의 정확한 HTTPS 주소여야 한다")
    @ParameterizedTest
    @MethodSource("invalidKakaoPlaces")
    void validateKakaoPlace(String id, String url) {
        // given: 최종 장소 값
        // when & then
        assertThatThrownBy(() -> validator.validate(id, url)).isInstanceOf(BaseException.class);
    }

    static Stream<Arguments> invalidKakaoPlaces() {
        return Stream.of(
            Arguments.of("12a", "https://place.map.kakao.com/12a"),
            Arguments.of("１２３", "https://place.map.kakao.com/１２３"),
            Arguments.of(" 123", "https://place.map.kakao.com/ 123"),
            Arguments.of("123", "http://place.map.kakao.com/123"),
            Arguments.of("123", "https://example.com/123"),
            Arguments.of("123", "https://place.map.kakao.com.evil.com/123"),
            Arguments.of("123", "https://place.map.kakao.com/456"),
            Arguments.of("123", "https://place.map.kakao.com/123?from=map"),
            Arguments.of("123", "https://place.map.kakao.com/123/"),
            Arguments.of("123", " https://place.map.kakao.com/123 ")
        );
    }

    @DisplayName("숫자 문자열 ID와 일치하는 카카오 URL을 허용한다")
    @ParameterizedTest
    @ValueSource(strings = {"123", "00123"})
    void allowsKakaoPlace(String id) {
        // given & when & then
        assertThatCode(() -> validator.validate(id, "https://place.map.kakao.com/" + id))
            .doesNotThrowAnyException();
    }

    @DisplayName("장소 정보가 없으면 카카오 형식 검증을 생략한다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void skipsAbsentPlace(String value) {
        // given & when & then
        assertThatCode(() -> validator.validate(value, value)).doesNotThrowAnyException();
    }
}
