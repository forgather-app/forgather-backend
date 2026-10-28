package com.forgather.domain.space.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.fixture.SpaceFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class SpaceTest {

    @DisplayName("스페이스 생성에 코드와 이름은 필수값이다.")
    @Test
    void createSpaceWithRequiredFields() {
        // given
        String spaceCode = "1234567890";
        String name = "나의 졸업전시";

        // when & then
        assertThatCode(
            () -> new Space(spaceCode, name, "", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS)
        ).doesNotThrowAnyException();
    }

    @DisplayName("설명, 링크는 공백인 경우 빈 문자열로 저장한다.")
    @Test
    void createSpaceWithBlank() {
        // given
        String description = "  ";
        String linkUrl = "  ";
        String linkName = "  ";

        // when
        Space space = new Space("1234567890", "나의 졸업전시", description, false, linkUrl, linkName, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertAll(
            () -> assertThat(space.getDescription()).isEmpty(),
            () -> assertThat(space.getLinkUrl()).isEmpty(),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("설명은 입력하지 않아도(null) 스페이스를 생성할 수 있고, 빈 문자열로 저장한다.")
    @Test
    void createSpaceWithoutDescription() {
        // given & when
        Space space = new Space("1234567890", "나의 졸업전시", null, false, null, null, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertThat(space.getDescription()).isEmpty();
    }

    @DisplayName("링크는 입력하지 않아도(null) 스페이스를 생성할 수 있고, 빈 문자열로 저장한다.")
    @Test
    void createSpaceWithoutLink() {
        // given & when
        Space space = new Space("1234567890", "나의 졸업전시", "설명", false, null, null, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEmpty(),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("링크 URL과 표시 이름을 함께 입력하면 스페이스를 생성할 수 있다.")
    @Test
    void createSpaceWithLink() {
        // given & when
        Space space = new Space("1234567890", "나의 졸업전시", "설명", false, "https://forgather.me", "포트폴리오", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEqualTo("https://forgather.me"),
            () -> assertThat(space.getLinkName()).isEqualTo("포트폴리오")
        );
    }

    @DisplayName("스페이스 코드가 존재하지 않으면 스페이스를 생성할 수 없다.")
    @Test
    void createSpaceWithoutCode() {
        // given
        String name = "나의 졸업전시";

        // when & then
        assertThatThrownBy(
            () -> new Space(null, name, null, false, null, null, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS)
        ).isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("스페이스 코드");
    }

    @DisplayName("스페이스 이름이 존재하지 않으면 스페이스를 생성할 수 없다.")
    @Test
    void createSpaceWithoutName() {
        // given
        String code = "1234567890";

        // when & then
        assertThatThrownBy(
            () -> new Space(code, null, null, false, null, null, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS)
        ).isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("스페이스 이름");
    }

    @DisplayName("스페이스 이름 이모지 1글자 처리")
    @Test
    void createSpaceWithEmoji() {
        // given
        String spaceCode = "1234567890";
        // 가족 이모지, length 11
        String name = "👨‍👩‍👧‍👦".repeat(30);
        String description = "스페이스 설명";

        // when & then
        assertThatCode(
            () -> new Space(spaceCode, name, description, false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS)
        ).doesNotThrowAnyException();
    }

    @DisplayName("스페이스 이름이 비어있거나, 30자 초과면 예외를 던진다")
    @NullAndEmptySource
    @ParameterizedTest
    @ValueSource(strings = {" ", "a123456789012345678901234567890"})
    void spaceNameValidationTest(String invalidName) {
        // given
        String description = "스페이스 설명";

        // when & then
        assertThatThrownBy(
            () -> new Space("1234567890", invalidName, description, false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS)
        ).isInstanceOf(BaseException.class)
            .hasMessageContaining("스페이스 이름");
    }

    @DisplayName("스페이스 코드는 10자리여야 한다")
    @Test
    void spaceCodeValidationTest() {
        // given
        String name = "스페이스";
        String description = "스페이스 설명";

        // when & then
        assertThatThrownBy(
            () -> new Space("123456789", name, description, false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS)
        ).isInstanceOf(BaseException.class)
            .hasMessageContaining("스페이스 코드");
    }

    @DisplayName("스페이스 설명이 500자를 초과하면 예외를 던진다.")
    @Test
    void spaceDescriptionValidationTest() {
        // given
        String name = "스페이스";
        String description = getString(501);

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", name, description, false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("스페이스 설명");
    }

    @DisplayName("일반 문자와 복합 이모지 설명 500자로 스페이스를 생성한다")
    @ValueSource(strings = {"가", "🧑‍🧑‍🧒‍🧒"})
    @ParameterizedTest
    void createsSpaceWithDescriptionAtMaxLength(String character) {
        // given
        String description = character.repeat(500);

        // when
        Space space = new Space("1234567890", "스페이스", description, false, null, null, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertThat(space.getDescription()).isEqualTo(description);
    }

    @DisplayName("설명을 501자로 수정하면 예외를 던지고 기존 설명을 유지한다")
    @ParameterizedTest
    @ValueSource(strings = {"가", "🧑‍🧑‍🧒‍🧒"})
    void rejectsDescriptionUpdateOverMaxLength(String character) {
        // given
        Space space = SpaceFixture.createSpace();
        String originalDescription = space.getDescription();

        // when & then
        assertThatThrownBy(() -> space.update(null, character.repeat(501), null, null, null))
            .isInstanceOf(BaseException.class);
        assertThat(space.getDescription()).isEqualTo(originalDescription);
    }

    @DisplayName("링크 URL만 입력하고 표시 이름이 없어도 스페이스를 생성할 수 있고, 표시 이름은 빈 문자열로 저장한다.")
    @NullAndEmptySource
    @ParameterizedTest
    @ValueSource(strings = {" "})
    void createSpaceWithOnlyLinkUrl(String blankName) {
        // given & when
        Space space = new Space("1234567890", "스페이스", "설명", false, "https://forgather.me", blankName, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEqualTo("https://forgather.me"),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("표시 이름만 입력하고 링크 URL이 없으면 스페이스를 생성할 수 없다.")
    @NullAndEmptySource
    @ParameterizedTest
    @ValueSource(strings = {" "})
    void spaceLinkWithoutUrlValidationTest(String blankUrl) {
        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, blankUrl, "포트폴리오", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("함께 입력");
    }

    @DisplayName("링크 URL은 http/https 형식을 따라야한다.")
    @ValueSource(strings = {"forgather.me", "ftp://forgather.me", "httpx://forgather.me", "javascript:alert(1)"})
    @ParameterizedTest
    void spaceLinkUrlPatternValidationTest(String invalidUrl) {
        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, invalidUrl, "포트폴리오", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("링크 URL 형식");
    }

    @DisplayName("표시 이름 없이 링크 URL만 입력해도 http/https 형식 검증을 적용한다.")
    @ValueSource(strings = {"forgather.me", "ftp://forgather.me", "javascript:alert(1)"})
    @ParameterizedTest
    void spaceLinkUrlPatternValidationWithoutName(String invalidUrl) {
        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, invalidUrl, null, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("링크 URL 형식");
    }

    @DisplayName("링크 URL의 scheme은 대소문자를 구분하지 않는다.")
    @ValueSource(strings = {"HTTPS://forgather.me", "Http://forgather.me", "hTtPs://forgather.me"})
    @ParameterizedTest
    void createSpaceWithCaseInsensitiveSchemeLink(String linkUrl) {
        // given & when
        Space space = new Space("1234567890", "나의 졸업전시", "설명", false, linkUrl, "포트폴리오", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertThat(space.getLinkUrl()).isEqualTo(linkUrl);
    }

    @DisplayName("링크 URL은 최대 2048자까지 가능하다.")
    @Test
    void spaceLinkUrlLengthValidationTest() {
        // given
        String tooLongUrl = "https://forgather.me/" + getString(2048);

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, tooLongUrl, "포트폴리오", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("링크 URL은 최대");
    }

    @DisplayName("링크 표시 이름은 최대 30자까지 가능하다.")
    @Test
    void spaceLinkNameLengthValidationTest() {
        // given
        String tooLongName = getString(31);

        // when & then
        assertThatThrownBy(
            () -> new Space("1234567890", "스페이스", "설명", false, "https://forgather.me", tooLongName, null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("링크 표시 이름은 최대");
    }

    @DisplayName("스페이스 이름을 수정할 수 있다.")
    @Test
    void updateSpaceName() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // when
        space.update("새로운 스페이스", null, null, null, null);

        // then
        assertAll(
            () -> assertThat(space.getName()).isEqualTo("새로운 스페이스"),
            () -> assertThat(space.getDescription()).isEqualTo("스페이스 설명"),
            () -> assertThat(space.isPublic()).isFalse()
        );
    }

    @DisplayName("스페이스 설명을 일반 문자와 복합 이모지 500자로 수정하고 다른 필드는 유지한다")
    @ValueSource(strings = {"가", "🧑‍🧑‍🧒‍🧒"})
    @ParameterizedTest
    void updateSpaceDescription(String character) {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);
        String description = character.repeat(500);

        // when
        space.update(null, description, null, null, null);

        // then
        assertAll(
            () -> assertThat(space.getName()).isEqualTo("스페이스"),
            () -> assertThat(space.getDescription()).isEqualTo(description),
            () -> assertThat(space.isPublic()).isFalse()
        );
    }

    @DisplayName("스페이스 공개 여부를 수정할 수 있다.")
    @Test
    void updateSpaceIsPublic() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // when
        space.update(null, null, true, null, null);

        // then
        assertAll(
            () -> assertThat(space.getName()).isEqualTo("스페이스"),
            () -> assertThat(space.getDescription()).isEqualTo("스페이스 설명"),
            () -> assertThat(space.isPublic()).isTrue()
        );
    }

    @DisplayName("링크 URL과 표시 이름을 함께 추가할 수 있다.")
    @Test
    void updateSpaceLink() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // when
        space.update(null, null, null, "https://forgather.me", "포트폴리오");

        // then
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEqualTo("https://forgather.me"),
            () -> assertThat(space.getLinkName()).isEqualTo("포트폴리오")
        );
    }

    @DisplayName("기존 링크를 빈 쌍으로 수정하면 링크를 삭제할 수 있다.")
    @Test
    void updateSpaceLinkToEmpty() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "https://forgather.me", "포트폴리오", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // when
        space.update(null, null, null, "", "");

        // then
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEmpty(),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("링크 URL만 수정해도 표시 이름 없이 링크를 추가할 수 있다.")
    @Test
    void updateSpaceLinkUrlOnly() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // when
        space.update(null, null, null, "https://forgather.me", null);

        // then
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEqualTo("https://forgather.me"),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("링크 URL이 없는 스페이스에 표시 이름만 수정하면 예외를 던지고 기존 값을 유지한다.")
    @Test
    void rejectsUpdateLinkNameWithoutUrl() {
        // given
        Space space = SpaceFixture.createSpace();

        // when & then
        assertThatThrownBy(() -> space.update(null, null, null, null, "포트폴리오"))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("함께 입력");
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEmpty(),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("링크 URL과 표시 이름이 있는 스페이스에서 표시 이름만 비우면 URL은 유지하고 이름만 삭제한다.")
    @Test
    void updateSpaceLinkNameToEmptyKeepsUrl() {
        // given
        Space space = SpaceFixture.createSpaceWithLink("https://forgather.me", "포트폴리오");

        // when
        space.update(null, null, null, null, "");

        // then
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEqualTo("https://forgather.me"),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("링크 URL과 표시 이름이 있는 스페이스에서 URL만 비우면 예외를 던지고 기존 링크를 유지한다.")
    @Test
    void rejectsClearingLinkUrlWhileNameRemains() {
        // given
        Space space = SpaceFixture.createSpaceWithLink("https://forgather.me", "포트폴리오");

        // when & then
        assertThatThrownBy(() -> space.update(null, null, null, "", null))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("함께 입력");
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEqualTo("https://forgather.me"),
            () -> assertThat(space.getLinkName()).isEqualTo("포트폴리오")
        );
    }

    @DisplayName("전시 날짜를 전달하면 스페이스에 기간 값 객체를 설정한다")
    @Test
    void createSpaceWithExhibitionPeriod() {
        // given
        LocalDate startDate = LocalDate.of(2026, 10, 19);
        LocalDate endDate = LocalDate.of(2026, 10, 20);

        // when
        Space space = new Space("1234567890", "스페이스", "설명", false, null, null, startDate, endDate, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertAll(
            () -> assertThat(space.getExhibitionPeriod()).isNotNull(),
            () -> assertThat(space.getExhibitionStartDate()).isEqualTo(startDate),
            () -> assertThat(space.getExhibitionEndDate()).isEqualTo(endDate)
        );
    }

    @DisplayName("전시 날짜 없이 스페이스를 생성하면 기간 값 객체는 없다")
    @Test
    void createSpaceWithoutExhibitionPeriod() {
        // given & when
        Space space = SpaceFixture.createSpace();

        // then
        assertAll(
            () -> assertThat(space.getExhibitionPeriod()).isNull(),
            () -> assertThat(space.getExhibitionStartDate()).isNull(),
            () -> assertThat(space.getExhibitionEndDate()).isNull()
        );
    }

    @DisplayName("새 전시 날짜를 전달하면 기존 기간을 교체한다")
    @Test
    void updateExhibitionPeriod() {
        // given
        Space space = SpaceFixture.createSpace();
        space.updateExhibitionPeriodAndGuestBookWritingPolicy(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), null);
        LocalDate startDate = LocalDate.of(2026, 10, 19);
        LocalDate endDate = LocalDate.of(2026, 10, 20);

        // when
        space.updateExhibitionPeriodAndGuestBookWritingPolicy(startDate, endDate, null);

        // then
        assertAll(
            () -> assertThat(space.getExhibitionStartDate()).isEqualTo(startDate),
            () -> assertThat(space.getExhibitionEndDate()).isEqualTo(endDate)
        );
    }

    @DisplayName("새 전시 기간 생성에 실패하면 기존 기간 객체를 유지한다")
    @Test
    void keepsExhibitionPeriodWhenReplacementFails() {
        // given
        Space space = SpaceFixture.createSpace();
        space.updateExhibitionPeriodAndGuestBookWritingPolicy(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), null);
        SpaceExhibitionPeriod previousPeriod = space.getExhibitionPeriod();

        // when & then
        assertThatThrownBy(() -> space.updateExhibitionPeriodAndGuestBookWritingPolicy(
            LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 19), null))
            .isInstanceOf(BaseException.class);
        assertThat(space.getExhibitionPeriod()).isSameAs(previousPeriod);
    }

    @DisplayName("시작일과 종료일을 모두 null로 변경하면 기존 전시 기간을 삭제한다")
    @Test
    void clearExhibitionPeriod() {
        // given
        Space space = SpaceFixture.createSpace();
        space.updateExhibitionPeriodAndGuestBookWritingPolicy(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), null);

        // when
        space.updateExhibitionPeriodAndGuestBookWritingPolicy(null, null, null);

        // then
        assertAll(
            () -> assertThat(space.getExhibitionStartDate()).isNull(),
            () -> assertThat(space.getExhibitionEndDate()).isNull(),
            () -> assertThat(space.getExhibitionPeriod()).isNull()
        );
    }

    @DisplayName("스페이스를 생성하면 축하받는 스페이스로 지정되지 않은 상태다.")
    @Test
    void createSpaceIsNotFeatured() {
        // given & when
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertThat(space.isFeatured()).isFalse();
    }

    @DisplayName("스페이스를 축하받는 스페이스로 지정한다.")
    @Test
    void feature() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // when
        space.feature();

        // then
        assertThat(space.isFeatured()).isTrue();
    }

    @DisplayName("이미 지정된 스페이스를 다시 지정해도 지정 상태를 유지한다.")
    @Test
    void featureIsIdempotent() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);
        space.feature();

        // when
        space.feature();

        // then
        assertThat(space.isFeatured()).isTrue();
    }

    @DisplayName("축하받는 스페이스 지정을 해제한다.")
    @Test
    void unfeature() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);
        space.feature();

        // when
        space.unfeature();

        // then
        assertThat(space.isFeatured()).isFalse();
    }

    @DisplayName("지정되지 않은 스페이스를 해제해도 예외 없이 미지정 상태를 유지한다.")
    @Test
    void unfeatureIsIdempotent() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);

        // when
        space.unfeature();

        // then
        assertThat(space.isFeatured()).isFalse();
    }

    /**
     * 삭제 시 정리해야 할 자기 상태는 서비스가 아니라 스페이스가 책임진다.
     * 정리 대상 필드가 늘어날 때 서비스에서 호출을 빠뜨리는 것을 막기 위한 회귀 테스트다.
     */
    @DisplayName("스페이스를 삭제하면 축하받는 스페이스 지정도 함께 해제된다.")
    @Test
    void deleteUnfeatures() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);
        space.feature();

        // when
        space.delete();

        // then
        assertAll(
            () -> assertThat(space.isFeatured()).isFalse(),
            () -> assertThat(space.getDeletedAt()).isNotNull()
        );
    }

    @DisplayName("이미 삭제된 스페이스를 다시 삭제해도 삭제 시각은 바뀌지 않는다.")
    @Test
    void deleteIsIdempotent() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);
        space.feature();
        space.delete();
        var deletedAt = space.getDeletedAt();

        // when
        space.delete();

        // then
        assertAll(
            () -> assertThat(space.getDeletedAt()).isEqualTo(deletedAt),
            () -> assertThat(space.isFeatured()).isFalse()
        );
    }

    /**
     * "호스트당 축하받는 스페이스 1개"는 DB 제약이 아니라 서비스 계층이 보장한다.
     * 스페이스 수정 경로로 지정 상태가 바뀌면 그 보장이 통째로 우회되므로, update()가
     * 이 값을 건드리지 않는다는 사실을 회귀 테스트로 고정한다.
     */
    @DisplayName("스페이스 정보를 수정해도 축하받는 스페이스 지정 상태는 바뀌지 않는다.")
    @Test
    void updateDoesNotChangeFeatured() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "", "", null, null, null, null, null,
            GuestBookWritingPolicy.ALWAYS);
        space.feature();

        // when
        space.update("새 이름", "새 설명", true, "https://forgather.me", "포트폴리오");

        // then
        assertThat(space.isFeatured()).isTrue();
    }

    @DisplayName("스페이스 코드가 같으면 true를 반환한다.")
    @Test
    void isSameCode() {
        // given
        Space space = SpaceFixture.createSpaceWithCode("1234567890");

        // when
        boolean result = space.isSameCode("1234567890");

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("스페이스 코드가 다르면 false를 반환한다.")
    @Test
    void isSameCodeWithDifferentCode() {
        // given
        Space space = SpaceFixture.createSpaceWithCode("1234567890");

        // when
        boolean result = space.isSameCode("0987654321");

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("비교할 스페이스 코드가 null이면 예외 없이 false를 반환한다.")
    @Test
    void isSameCodeWithNull() {
        // given
        Space space = SpaceFixture.createSpaceWithCode("1234567890");

        // when
        boolean result = space.isSameCode(null);

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("스페이스 코드 비교는 대소문자를 구분한다.")
    @Test
    void isSameCodeIsCaseSensitive() {
        // given
        Space space = SpaceFixture.createSpaceWithCode("abcdefghij");

        // when
        boolean result = space.isSameCode("ABCDEFGHIJ");

        // then
        assertThat(result).isFalse();
    }

    @DisplayName("스페이스 생성 시 전달한 장소 정보를 제공한다")
    @Test
    void createWithPlace() {
        // given & when
        Space space = new Space("1234567890", "스페이스", null, false, null, null, null, null,
            "전시장", "123", "https://place.map.kakao.com/123",
            GuestBookWritingPolicy.ALWAYS);

        // then
        assertPlace(space, "전시장", "123", "https://place.map.kakao.com/123");
    }

    @DisplayName("장소를 수정하면 새로운 장소 정보로 변경된다")
    @Test
    void updatePlace() {
        // given
        Space space = new Space("1234567890", "스페이스", null, false, null, null, null, null,
            "전시장", "123", "https://place.map.kakao.com/123",
            GuestBookWritingPolicy.ALWAYS);

        // when
        space.updatePlace("다른 전시장", "456", "https://place.map.kakao.com/456");

        // then
        assertPlace(space, "다른 전시장", "456", "https://place.map.kakao.com/456");
    }

    @DisplayName("잘못된 장소로 수정하면 기존 장소 정보를 유지한다")
    @Test
    void preservePlaceAfterInvalidUpdate() {
        // given
        Space space = new Space("1234567890", "스페이스", null, false, null, null, null, null,
            "전시장", "123", "https://place.map.kakao.com/123",
            GuestBookWritingPolicy.ALWAYS);

        // when & then
        assertThatThrownBy(() -> space.updatePlace(null, "123", "https://place.map.kakao.com/123"))
            .isInstanceOf(BaseException.class);
        assertPlace(space, "전시장", "123", "https://place.map.kakao.com/123");
    }

    @DisplayName("전시 기간 없이 기간 제한 정책으로 스페이스를 생성할 수 없다")
    @Test
    void rejectsCreationWithPeriodPolicyWithoutPeriod() {
        // given
        GuestBookWritingPolicy policy = GuestBookWritingPolicy.EXHIBITION_PERIOD;

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", true, "", "",
            null, null, null, null, null, policy))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("전시 기간 제한 정책에는 전시 기간이 필요합니다.");
    }

    @DisplayName("기간 없는 스페이스에 기간 제한 정책을 설정하면 거절하고 기존 상태를 유지한다")
    @Test
    void rejectsPeriodPolicyWithoutPeriodAndPreservesState() {
        // given
        Space space = SpaceFixture.createSpace();

        // when & then
        assertThatThrownBy(() -> space.updateExhibitionPeriodAndGuestBookWritingPolicy(
            null, null, GuestBookWritingPolicy.EXHIBITION_PERIOD))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("전시 기간 제한 정책에는 전시 기간이 필요합니다.");
        assertAll(
            () -> assertThat(space.getGuestBookWritingPolicy()).isEqualTo(GuestBookWritingPolicy.ALWAYS),
            () -> assertThat(space.getExhibitionPeriod()).isNull()
        );
    }

    @DisplayName("기간 제한 정책에서 기간만 삭제하려 하면 정책과 기간을 모두 유지한다")
    @Test
    void preservesPolicyAndPeriodAfterRejectedDeletion() {
        // given
        Space space = SpaceFixture.createSpace();
        LocalDate startDate = LocalDate.of(2040, 10, 20);
        LocalDate endDate = LocalDate.of(2040, 10, 22);
        space.updateExhibitionPeriodAndGuestBookWritingPolicy(startDate, endDate,
            GuestBookWritingPolicy.EXHIBITION_PERIOD);

        // when & then
        assertThatThrownBy(() -> space.updateExhibitionPeriodAndGuestBookWritingPolicy(null, null, null))
            .isInstanceOf(BaseException.class);
        assertAll(
            () -> assertThat(space.getGuestBookWritingPolicy()).isEqualTo(GuestBookWritingPolicy.EXHIBITION_PERIOD),
            () -> assertThat(space.getExhibitionStartDate()).isEqualTo(startDate),
            () -> assertThat(space.getExhibitionEndDate()).isEqualTo(endDate)
        );
    }

    private void assertPlace(Space space, String name, String id, String url) {
        assertAll(
            () -> assertThat(space.getPlaceName()).isEqualTo(name),
            () -> assertThat(space.getPlaceId()).isEqualTo(id),
            () -> assertThat(space.getPlaceUrl()).isEqualTo(url)
        );
    }

    private String getString(int length) {
        return "a".repeat(Math.max(0, length));
    }
}
