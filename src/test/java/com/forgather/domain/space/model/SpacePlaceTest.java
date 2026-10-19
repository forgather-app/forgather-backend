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

class SpacePlaceTest {

    @DisplayName("장소명 없이 장소 정보가 있거나 장소 정보 한쪽만 있으면 거부한다")
    @ParameterizedTest
    @MethodSource("invalidPlaceCombinations")
    void validatePlaceCombinations(String name, String id, String url) {
        // given: 최종 장소 값
        // when & then
        assertThatThrownBy(() -> new SpacePlace(name, id, url))
            .isInstanceOfSatisfying(BaseException.class, exception -> assertThat(exception.getStatusCode()).isEqualTo(400));
    }

    static Stream<Arguments> invalidPlaceCombinations() {
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
    void normalizesAbsentPlaceToEmptyStrings(String value) {
        // given & when
        SpacePlace place = new SpacePlace(value, value, value);

        // then
        assertAll(
            () -> assertThat(place.getPlaceName()).isEmpty(),
            () -> assertThat(place.getPlaceId()).isEmpty(),
            () -> assertThat(place.getPlaceUrl()).isEmpty()
        );
    }

    @DisplayName("장소명만 저장하며 앞뒤 공백을 보존한다")
    @Test
    void allowsPlaceNameOnlyWithoutTrimming() {
        // given & when
        SpacePlace place = new SpacePlace(" 전시장 ", null, null);

        // then
        assertAll(
            () -> assertThat(place.getPlaceName()).isEqualTo(" 전시장 "),
            () -> assertThat(place.getPlaceId()).isEmpty(),
            () -> assertThat(place.getPlaceUrl()).isEmpty()
        );
    }

    @DisplayName("장소명은 기존 글자 수 기준 일반 문자, 이모지, 결합 문자 50자를 허용하고 51자는 거부한다")
    @ParameterizedTest
    @ValueSource(strings = {"가", "👨‍👩‍👧‍👦", "a\u0301"})
    void validatePlaceNameLength(String character) {
        // given: 글자 수 경계값
        // when & then
        assertThatCode(() -> new SpacePlace(character.repeat(50), null, null)).doesNotThrowAnyException();
        // given: 최종 장소 값
        // when & then
        assertThatThrownBy(() -> new SpacePlace(character.repeat(51), null, null))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("50");
    }

    @DisplayName("장소 저장 상한을 넘는 값은 400 예외를 던진다")
    @ParameterizedTest
    @MethodSource("placesExceedingStorageLength")
    void validatePlaceStorageLength(String name, String id, String url, String limit) {
        // given: 최종 장소 값
        // when & then
        assertThatThrownBy(() -> new SpacePlace(name, id, url))
            .isInstanceOfSatisfying(BaseException.class, exception -> assertThat(exception.getStatusCode()).isEqualTo(400))
            .hasMessageContaining(limit);
    }

    static Stream<Arguments> placesExceedingStorageLength() {
        return Stream.of(
            Arguments.of("전시장", "1".repeat(256), "https://place.map.kakao.com/" + "1".repeat(256), "255"),
            Arguments.of("전시장", "123", "https://place.map.kakao.com/" + "1".repeat(2023), "2048")
        );
    }

    @DisplayName("장소 객체는 특정 제공자의 ID와 URL 형식을 제한하지 않는다")
    @Test
    void allowsProviderIndependentPlace() {
        // given & when & then
        assertThatCode(() -> new SpacePlace("전시장", "naver-123", "https://map.naver.com/place/123"))
            .doesNotThrowAnyException();
    }
}
