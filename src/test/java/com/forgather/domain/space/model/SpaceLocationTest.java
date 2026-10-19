package com.forgather.domain.space.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;

class SpaceLocationTest {

    @DisplayName("장소명 없이 카카오 정보가 있거나 카카오 정보 한쪽만 있으면 거부한다")
    @ParameterizedTest
    @MethodSource("invalidLocationCombinations")
    void validateLocationCombinations(String name, String id, String url) {
        // given: 최종 장소 값
        // when & then
        assertThatThrownBy(() -> new SpaceLocation(name, id, url))
            .isInstanceOfSatisfying(BaseException.class, exception -> assertThat(exception.getStatusCode()).isEqualTo(400));
    }

    static Stream<Arguments> invalidLocationCombinations() {
        return Stream.of(
            Arguments.of(null, "123", "https://place.map.kakao.com/123"),
            Arguments.of(null, "123", null),
            Arguments.of(null, null, "https://place.map.kakao.com/123"),
            Arguments.of("전시장", "123", null),
            Arguments.of("전시장", null, "https://place.map.kakao.com/123"),
            Arguments.of("전시장", "", "https://place.map.kakao.com/123"),
            Arguments.of("전시장", "  ", "https://place.map.kakao.com/123"),
            Arguments.of("전시장", "123", ""),
            Arguments.of("전시장", "123", "  ")
        );
    }

    @DisplayName("선택 장소 문자열은 null 또는 공백이면 빈 문자열로 보관한다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void normalizesAbsentLocationToEmptyStrings(String value) {
        // given & when
        SpaceLocation location = new SpaceLocation(value, value, value);

        // then
        assertAll(
            () -> assertThat(location.getLocationName()).isEmpty(),
            () -> assertThat(location.getKakaoPlaceId()).isEmpty(),
            () -> assertThat(location.getKakaoPlaceUrl()).isEmpty()
        );
    }

    @DisplayName("장소명만 저장하며 앞뒤 공백을 보존한다")
    @Test
    void allowsLocationNameOnlyWithoutTrimming() {
        // given & when
        SpaceLocation location = new SpaceLocation(" 전시장 ", null, null);

        // then
        assertAll(
            () -> assertThat(location.getLocationName()).isEqualTo(" 전시장 "),
            () -> assertThat(location.getKakaoPlaceId()).isEmpty(),
            () -> assertThat(location.getKakaoPlaceUrl()).isEmpty()
        );
    }

    @DisplayName("장소명은 기존 글자 수 기준 일반 문자, 이모지, 결합 문자 50자를 허용하고 51자는 거부한다")
    @ParameterizedTest
    @ValueSource(strings = {"가", "👨‍👩‍👧‍👦", "a\u0301"})
    void validateLocationNameLength(String character) {
        // given: 글자 수 경계값
        // when & then
        assertThatCode(() -> new SpaceLocation(character.repeat(50), null, null)).doesNotThrowAnyException();
        // given: 최종 장소 값
        // when & then
        assertThatThrownBy(() -> new SpaceLocation(character.repeat(51), null, null))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("50");
    }

    @DisplayName("장소 저장 상한을 넘는 값은 400 예외를 던진다")
    @ParameterizedTest
    @MethodSource("locationsExceedingStorageLength")
    void validateLocationStorageLength(String name, String id, String url, String limit) {
        // given: 최종 장소 값
        // when & then
        assertThatThrownBy(() -> new SpaceLocation(name, id, url))
            .isInstanceOfSatisfying(BaseException.class, exception -> assertThat(exception.getStatusCode()).isEqualTo(400))
            .hasMessageContaining(limit);
    }

    static Stream<Arguments> locationsExceedingStorageLength() {
        return Stream.of(
            Arguments.of("전시장", "1".repeat(256), "https://place.map.kakao.com/" + "1".repeat(256), "255"),
            Arguments.of("전시장", "123", "https://place.map.kakao.com/" + "1".repeat(2023), "2048")
        );
    }

    @DisplayName("카카오 ID는 숫자 문자열이고 URL은 해당 ID의 정확한 HTTPS 주소여야 한다")
    @ParameterizedTest
    @MethodSource("invalidKakaoPlaces")
    void validateKakaoPlace(String id, String url) {
        // given: 최종 장소 값
        // when & then
        assertThatThrownBy(() -> new SpaceLocation("전시장", id, url)).isInstanceOf(BaseException.class);
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
}
