package com.forgather.domain.exhibition.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseException;

class LocationTest {

    private static final String ONLINE_URL = "https://forgather.app";
    private static final String BASE_ADDRESS = "서울특별시 송파구";
    private static final String DETAIL_ADDRESS = "루터회관";

    @Nested
    @DisplayName("장소 유형 검증")
    class LocationTypeValidation {

        @DisplayName("장소 유형이 null이면 생성할 수 없다.")
        @Test
        void createWithNullType() {
            // when & then
            assertThatThrownBy(() -> new Location(null, ONLINE_URL, null, null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("장소 유형");
        }
    }

    @Nested
    @DisplayName("온라인 전시 검증")
    class OnlineLocation {

        @DisplayName("온라인 URL이 null이면 생성할 수 없다.")
        @Test
        void createWithoutOnlineUrl() {
            // when & then
            assertThatThrownBy(() -> new Location(LocationType.ONLINE, null, null, null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("온라인 URL");
        }

        @DisplayName("온라인 URL이 있으면 정상 생성된다.")
        @Test
        void createWithOnlineUrl() {
            // when
            Location location = new Location(LocationType.ONLINE, ONLINE_URL, null, null);

            // then
            assertAll(
                () -> assertThat(location.getType()).isEqualTo(LocationType.ONLINE),
                () -> assertThat(location.getOnlineUrl()).isEqualTo(ONLINE_URL)
            );
        }
    }

    @Nested
    @DisplayName("오프라인 전시 검증")
    class OfflineLocation {

        @DisplayName("기본 주소가 null이면 생성할 수 없다.")
        @Test
        void createWithoutBaseAddress() {
            // when & then
            assertThatThrownBy(() -> new Location(LocationType.OFFLINE, null, null, DETAIL_ADDRESS))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("기본 주소");
        }

        @DisplayName("기본 주소가 있고 상세 주소가 있으면 정상 생성된다.")
        @Test
        void createWithBaseAndDetailAddress() {
            // when
            Location location = new Location(LocationType.OFFLINE, null, BASE_ADDRESS, DETAIL_ADDRESS);

            // then
            assertAll(
                () -> assertThat(location.getType()).isEqualTo(LocationType.OFFLINE),
                () -> assertThat(location.getBaseAddress()).isEqualTo(BASE_ADDRESS),
                () -> assertThat(location.getDetailAddress()).isEqualTo(DETAIL_ADDRESS)
            );
        }

        @DisplayName("기본 주소만 있고 상세 주소가 없어도 정상 생성된다.")
        @Test
        void createWithoutDetailAddress() {
            // when
            Location location = new Location(LocationType.OFFLINE, null, BASE_ADDRESS, null);

            // then
            assertAll(
                () -> assertThat(location.getBaseAddress()).isEqualTo(BASE_ADDRESS),
                () -> assertThat(location.getDetailAddress()).isNull()
            );
        }
    }

    @DisplayName("타입과 필수 주소/URL이 모두 충족되면 예외가 발생하지 않는다.")
    @Test
    void createValidLocation() {
        // when & then
        assertThatCode(() -> new Location(LocationType.ONLINE, ONLINE_URL, null, null))
            .doesNotThrowAnyException();
    }
}
