package com.forgather.domain.exhibition.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgather.fixture.ExhibitionFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class ExhibitionTest {

    @Nested
    @DisplayName("필수 필드 검증")
    class RequiredFields {

        @DisplayName("전시 이름이 null이면 생성할 수 없다.")
        @Test
        void createWithNullTitle() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithTitle(null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("전시 이름");
        }

        @DisplayName("전시 시작 날짜가 null이면 생성할 수 없다.")
        @Test
        void createWithNullStartDate() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithStartDate(null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("시작 날짜");
        }

        @DisplayName("전시 종료 날짜가 null이면 생성할 수 없다.")
        @Test
        void createWithNullEndDate() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithEndDate(null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("종료 날짜");
        }
    }

    @Nested
    @DisplayName("전시 이름 검증")
    class TitleValidation {

        @DisplayName("전시 이름이 빈 문자열이면 생성할 수 없다.")
        @Test
        void createWithEmptyTitle() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithTitle(""))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("전시 이름");
        }

        @DisplayName("전시 이름이 공백 문자열이면 생성할 수 없다.")
        @Test
        void createWithWhitespaceTitle() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithTitle("   "))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("전시 이름");
        }

        @DisplayName("전시 이름이 100자를 초과하면 생성할 수 없다.")
        @Test
        void createWithTooLongTitle() {
            // given
            String longTitle = "가".repeat(101);

            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithTitle(longTitle))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("100자");
        }

        @DisplayName("전시 이름이 정확히 100자이면 생성할 수 있다.")
        @Test
        void createWithMaxLengthTitle() {
            // given
            String maxTitle = "가".repeat(100);

            // when
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibitionWithTitle(maxTitle);

            // then
            assertThat(exhibition.getTitle()).isEqualTo(maxTitle);
        }
    }

    @Nested
    @DisplayName("전시 운영 기간 검증")
    class OperationDateValidation {

        @DisplayName("전시 시작 날짜가 종료 날짜보다 이후이면 생성할 수 없다.")
        @Test
        void createWithStartDateAfterEndDate() {
            // given
            LocalDate startDate = LocalDate.of(2026, 7, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 1);

            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithDates(startDate, endDate))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("시작 날짜");
        }

        @DisplayName("전시 시작 날짜와 종료 날짜가 같으면 생성할 수 있다.")
        @Test
        void createWithSameStartAndEndDate() {
            // given
            LocalDate sameDate = LocalDate.of(2026, 6, 1);

            // when
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibitionWithDates(sameDate, sameDate);

            // then
            assertThat(exhibition.getStartDate()).isEqualTo(exhibition.getEndDate());
        }
    }

    @Nested
    @DisplayName("전시 설명 검증")
    class DescriptionValidation {

        @DisplayName("전시 설명이 빈 문자열이면 생성할 수 없다.")
        @Test
        void createWithBlankDescription() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithDescription(""))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("전시 설명");
        }

        @DisplayName("전시 설명이 200자를 초과하면 생성할 수 없다.")
        @Test
        void createWithTooLongDescription() {
            // given
            String longDescription = "가".repeat(201);

            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithDescription(longDescription))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("200자");
        }

        @DisplayName("전시 설명이 정확히 200자이면 생성할 수 있다.")
        @Test
        void createWithMaxLengthDescription() {
            // given
            String maxDescription = "가".repeat(200);

            // when
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibitionWithDescription(maxDescription);

            // then
            assertThat(exhibition.getDescription()).isEqualTo(maxDescription);
        }
    }

    @Nested
    @DisplayName("전시 운영 공지사항 검증")
    class OperationNoticeValidation {

        @DisplayName("전시 운영 공지사항이 null이면 생성할 수 있다.")
        @Test
        void createWithNullOperationNotice() {
            // when
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibitionWithOperationNotice(null);

            // then
            assertThat(exhibition.getOperationNotice()).isNull();
        }

        @DisplayName("전시 운영 공지사항이 빈 문자열이면 생성할 수 없다.")
        @Test
        void createWithBlankOperationNotice() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithOperationNotice(""))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("운영 공지사항");
        }

        @DisplayName("전시 운영 공지사항이 200자를 초과하면 생성할 수 없다.")
        @Test
        void createWithTooLongOperationNotice() {
            // given
            String longNotice = "가".repeat(201);

            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithOperationNotice(longNotice))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("200자");
        }

        @DisplayName("전시 운영 공지사항이 정확히 200자이면 생성할 수 있다.")
        @Test
        void createWithMaxLengthOperationNotice() {
            // given
            String maxNotice = "가".repeat(200);

            // when
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibitionWithOperationNotice(maxNotice);

            // then
            assertThat(exhibition.getOperationNotice()).isEqualTo(maxNotice);
        }
    }

    @Nested
    @DisplayName("전시 장소 유형 검증")
    class LocationTypeValidation {

        @DisplayName("장소 유형이 null이면 URL, 주소 없이 생성할 수 있다.")
        @Test
        void createWithNullLocationType() {
            // when
            Exhibition exhibition = ExhibitionFixture.createExhibitionWithoutLocationType();

            // then
            assertThat(exhibition.getLocationType()).isNull();
        }

        @DisplayName("온라인 전시는 온라인 URL이 없으면 생성할 수 없다.")
        @Test
        void createOnlineExhibitionWithoutUrl() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOnlineExhibitionWithUrl(null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("온라인 URL");
        }

        @DisplayName("온라인 전시는 온라인 URL이 있으면 생성할 수 있다.")
        @Test
        void createOnlineExhibitionWithUrl() {
            // when
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();

            // then
            assertAll(
                () -> assertThat(exhibition.getLocationType()).isEqualTo(LocationType.ONLINE),
                () -> assertThat(exhibition.getOnlineUrl()).isEqualTo(ExhibitionFixture.DEFAULT_ONLINE_URL)
            );
        }

        @DisplayName("오프라인 전시는 기본 주소가 없으면 생성할 수 없다.")
        @Test
        void createOfflineExhibitionWithoutBaseAddress() {
            // when & then
            assertThatThrownBy(() -> ExhibitionFixture.createOfflineExhibitionWithBaseAddress(null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("기본 주소");
        }

        @DisplayName("오프라인 전시는 기본 주소가 있으면 생성할 수 있다.")
        @Test
        void createOfflineExhibitionWithBaseAddress() {
            // when
            Exhibition exhibition = ExhibitionFixture.createOfflineExhibition();

            // then
            assertAll(
                () -> assertThat(exhibition.getLocationType()).isEqualTo(LocationType.OFFLINE),
                () -> assertThat(exhibition.getBaseAddress()).isEqualTo(ExhibitionFixture.DEFAULT_BASE_ADDRESS)
            );
        }

        @DisplayName("오프라인 전시는 상세 주소 없이도 생성할 수 있다.")
        @Test
        void createOfflineExhibitionWithoutDetailAddress() {
            // when
            Exhibition exhibition = ExhibitionFixture.createOfflineExhibitionWithBaseAddress(
                ExhibitionFixture.DEFAULT_BASE_ADDRESS
            );

            // then
            assertThat(exhibition.getDetailAddress()).isNull();
        }
    }
}
