package com.forgather.domain.space.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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
            () -> new Space(spaceCode, name, "", false, "", "", "", "")
        ).doesNotThrowAnyException();
    }

    @DisplayName("설명, 인스타그램 아이디, 이메일, 링크는 공백인 경우 빈 문자열로 저장한다.")
    @Test
    void createSpaceWithBlank() {
        // given
        String description = "  ";
        String instagramUsername = "  ";
        String email = "  ";
        String linkUrl = "  ";
        String linkName = "  ";

        // when
        Space space = new Space("1234567890", "나의 졸업전시", description, false, instagramUsername, email, linkUrl,
            linkName);

        // then
        assertAll(
            () -> assertThat(space.getDescription()).isEmpty(),
            () -> assertThat(space.getInstagramUsername()).isEmpty(),
            () -> assertThat(space.getEmail()).isEmpty(),
            () -> assertThat(space.getLinkUrl()).isEmpty(),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("링크는 입력하지 않아도(null) 스페이스를 생성할 수 있고, 빈 문자열로 저장한다.")
    @Test
    void createSpaceWithoutLink() {
        // given & when
        Space space = new Space("1234567890", "나의 졸업전시", "설명", false, "forgather_official",
            "forgather@forgather.me", null, null);

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
        Space space = new Space("1234567890", "나의 졸업전시", "설명", false, "forgather_official",
            "forgather@forgather.me", "https://forgather.me", "포트폴리오");

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
            () -> new Space(null, name, null, false, null, null, null, null)
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
            () -> new Space(code, null, null, false, null, null, null, null)
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
        String instagramUsername = "forgather_official";
        String email = "forgather@forgather.me";

        // when & then
        assertThatCode(
            () -> new Space(spaceCode, name, description, false, instagramUsername, email, "", "")
        ).doesNotThrowAnyException();
    }

    @DisplayName("스페이스 이름이 비어있거나, 30자 초과면 예외를 던진다")
    @NullAndEmptySource
    @ParameterizedTest
    @ValueSource(strings = {" ", "a123456789012345678901234567890"})
    void spaceNameValidationTest(String invalidName) {
        // given
        String description = "스페이스 설명";
        String instagramUsername = "forgather_official";
        String email = "forgather@forgather.me";

        // when & then
        assertThatThrownBy(
            () -> new Space("1234567890", invalidName, description, false, instagramUsername, email, "", "")
        ).isInstanceOf(BaseException.class)
            .hasMessageContaining("스페이스 이름");
    }

    @DisplayName("스페이스 코드는 10자리여야 한다")
    @Test
    void spaceCodeValidationTest() {
        // given
        String name = "스페이스";
        String description = "스페이스 설명";
        String instagramUsername = "forgather_official";
        String email = "forgather@forgather.me";

        // when & then
        assertThatThrownBy(
            () -> new Space("123456789", name, description, false, instagramUsername, email, "", "")
        ).isInstanceOf(BaseException.class)
            .hasMessageContaining("스페이스 코드");
    }

    @DisplayName("스페이스 설명은 최대 200자까지 가능하다.")
    @Test
    void spaceDescriptionValidationTest() {
        // given
        String name = "스페이스";
        String description = getString(201); // 201자
        String instagramUsername = "forgather_official";
        String email = "forgather@forgather.me";

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", name, description, false, instagramUsername, email, "", ""))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("스페이스 설명");
    }

    @DisplayName("인스타그램 아이디는 최대 30자까지 가능하다.")
    @Test
    void spaceInstagramUsernameValidationTest() {
        // given
        String name = "스페이스";
        String description = "스페이스 설명";
        String instagramUsername = getString(31);
        String email = "forgather@forgather.me";

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", name, description, false, instagramUsername, email, "", ""))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("인스타그램 아이디");
    }

    @DisplayName("이메일은 최대 50자까지 가능하다.")
    @Test
    void spaceEmailValidationTest() {
        // given
        String name = "스페이스";
        String description = "스페이스 설명";
        String instagramUsername = "forgather_official";
        String email = getString(51);

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", name, description, false, instagramUsername, email, "", ""))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("이메일");
    }

    @DisplayName("이메일은 올바른 형식을 따라야한다.")
    @ValueSource(strings = {"invalid", "@invalid.com", "@", "invalid@invalid"})
    @ParameterizedTest
    void spaceEmailPatternValidationTest(String email) {
        // given
        String name = "스페이스";
        String description = "스페이스 설명";
        String instagramUsername = "forgather_official";

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", name, description, false, instagramUsername, email, "", ""))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("이메일 형식");
    }

    @DisplayName("링크 URL만 입력하고 표시 이름이 없으면 예외를 던진다.")
    @NullAndEmptySource
    @ParameterizedTest
    @ValueSource(strings = {" "})
    void spaceLinkWithoutNameValidationTest(String blankName) {
        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, "forgather_official",
            "forgather@forgather.me", "https://forgather.me", blankName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("함께 입력");
    }

    @DisplayName("표시 이름만 입력하고 링크 URL이 없으면 예외를 던진다.")
    @NullAndEmptySource
    @ParameterizedTest
    @ValueSource(strings = {" "})
    void spaceLinkWithoutUrlValidationTest(String blankUrl) {
        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, "forgather_official",
            "forgather@forgather.me", blankUrl, "포트폴리오"))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("함께 입력");
    }

    @DisplayName("링크 URL은 http/https 형식을 따라야한다.")
    @ValueSource(strings = {"forgather.me", "ftp://forgather.me", "httpx://forgather.me", "javascript:alert(1)"})
    @ParameterizedTest
    void spaceLinkUrlPatternValidationTest(String invalidUrl) {
        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, "forgather_official",
            "forgather@forgather.me", invalidUrl, "포트폴리오"))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("링크 URL 형식");
    }

    @DisplayName("링크 URL은 최대 2048자까지 가능하다.")
    @Test
    void spaceLinkUrlLengthValidationTest() {
        // given
        String tooLongUrl = "https://forgather.me/" + getString(2048);

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, "forgather_official",
            "forgather@forgather.me", tooLongUrl, "포트폴리오"))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("링크 URL은 최대");
    }

    @DisplayName("링크 표시 이름은 최대 30자까지 가능하다.")
    @Test
    void spaceLinkNameLengthValidationTest() {
        // given
        String tooLongName = getString(31);

        // when & then
        assertThatThrownBy(() -> new Space("1234567890", "스페이스", "설명", false, "forgather_official",
            "forgather@forgather.me", "https://forgather.me", tooLongName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("링크 표시 이름은 최대");
    }

    @DisplayName("스페이스 이름을 수정할 수 있다.")
    @Test
    void updateSpaceName() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when
        space.update("새로운 스페이스", null, null, null, null, null, null);

        // then
        assertAll(
            () -> assertThat(space.getName()).isEqualTo("새로운 스페이스"),
            () -> assertThat(space.getDescription()).isEqualTo("스페이스 설명"),
            () -> assertThat(space.isPublic()).isFalse(),
            () -> assertThat(space.getInstagramUsername()).isEqualTo("forgather_official"),
            () -> assertThat(space.getEmail()).isEqualTo("forgather@forgather.me")
        );
    }

    @DisplayName("스페이스 설명을 수정할 수 있다.")
    @Test
    void updateSpaceDescription() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when
        space.update(null, "새로운 스페이스 설명", null, null, null, null, null);

        // then
        assertAll(
            () -> assertThat(space.getName()).isEqualTo("스페이스"),
            () -> assertThat(space.getDescription()).isEqualTo("새로운 스페이스 설명"),
            () -> assertThat(space.isPublic()).isFalse(),
            () -> assertThat(space.getInstagramUsername()).isEqualTo("forgather_official"),
            () -> assertThat(space.getEmail()).isEqualTo("forgather@forgather.me")
        );
    }

    @DisplayName("스페이스 공개 여부를 수정할 수 있다.")
    @Test
    void updateSpaceIsPublic() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when
        space.update(null, null, true, null, null, null, null);

        // then
        assertAll(
            () -> assertThat(space.getName()).isEqualTo("스페이스"),
            () -> assertThat(space.getDescription()).isEqualTo("스페이스 설명"),
            () -> assertThat(space.isPublic()).isTrue(),
            () -> assertThat(space.getInstagramUsername()).isEqualTo("forgather_official"),
            () -> assertThat(space.getEmail()).isEqualTo("forgather@forgather.me")
        );
    }

    @DisplayName("스페이스 인스타그램 아이디를 수정할 수 있다.")
    @Test
    void updateSpaceInstagramUsername() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when
        space.update(null, null, null, "forgather_official_new", null, null, null);

        // then
        assertAll(
            () -> assertThat(space.getName()).isEqualTo("스페이스"),
            () -> assertThat(space.getDescription()).isEqualTo("스페이스 설명"),
            () -> assertThat(space.isPublic()).isFalse(),
            () -> assertThat(space.getInstagramUsername()).isEqualTo("forgather_official_new"),
            () -> assertThat(space.getEmail()).isEqualTo("forgather@forgather.me")
        );
    }

    @DisplayName("스페이스 이메일을 수정할 수 있다.")
    @Test
    void updateSpaceEmail() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when
        space.update(null, null, null, null, "forgather_new@forgather.me", null, null);

        // then
        assertAll(
            () -> assertThat(space.getName()).isEqualTo("스페이스"),
            () -> assertThat(space.getDescription()).isEqualTo("스페이스 설명"),
            () -> assertThat(space.isPublic()).isFalse(),
            () -> assertThat(space.getInstagramUsername()).isEqualTo("forgather_official"),
            () -> assertThat(space.getEmail()).isEqualTo("forgather_new@forgather.me")
        );
    }

    @DisplayName("링크 URL과 표시 이름을 함께 추가할 수 있다.")
    @Test
    void updateSpaceLink() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when
        space.update(null, null, null, null, null, "https://forgather.me", "포트폴리오");

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
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "https://forgather.me", "포트폴리오");

        // when
        space.update(null, null, null, null, null, "", "");

        // then
        assertAll(
            () -> assertThat(space.getLinkUrl()).isEmpty(),
            () -> assertThat(space.getLinkName()).isEmpty()
        );
    }

    @DisplayName("링크 URL만 수정하고 표시 이름을 누락하면 예외를 던진다.")
    @Test
    void updateSpaceLinkUrlOnly() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when & then
        assertThatThrownBy(() -> space.update(null, null, null, null, null, "https://forgather.me", null))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("함께 입력");
    }

    @DisplayName("스페이스를 생성하면 축하받는 스페이스로 지정되지 않은 상태다.")
    @Test
    void createSpaceIsNotCelebrating() {
        // given & when
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // then
        assertThat(space.isCelebrating()).isFalse();
    }

    @DisplayName("스페이스를 축하받는 스페이스로 지정한다.")
    @Test
    void celebrate() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when
        space.celebrate();

        // then
        assertThat(space.isCelebrating()).isTrue();
    }

    @DisplayName("이미 지정된 스페이스를 다시 지정해도 지정 상태를 유지한다.")
    @Test
    void celebrateIsIdempotent() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");
        space.celebrate();

        // when
        space.celebrate();

        // then
        assertThat(space.isCelebrating()).isTrue();
    }

    @DisplayName("축하받는 스페이스 지정을 해제한다.")
    @Test
    void stopCelebrating() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");
        space.celebrate();

        // when
        space.stopCelebrating();

        // then
        assertThat(space.isCelebrating()).isFalse();
    }

    @DisplayName("지정되지 않은 스페이스를 해제해도 예외 없이 미지정 상태를 유지한다.")
    @Test
    void stopCelebratingIsIdempotent() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");

        // when
        space.stopCelebrating();

        // then
        assertThat(space.isCelebrating()).isFalse();
    }

    /**
     * "호스트당 축하받는 스페이스 1개"는 DB 제약이 아니라 서비스 계층이 보장한다.
     * 스페이스 수정 경로로 지정 상태가 바뀌면 그 보장이 통째로 우회되므로, update()가
     * 이 값을 건드리지 않는다는 사실을 회귀 테스트로 고정한다.
     */
    @DisplayName("스페이스 정보를 수정해도 축하받는 스페이스 지정 상태는 바뀌지 않는다.")
    @Test
    void updateDoesNotChangeCelebrating() {
        // given
        Space space = new Space("1234567890", "스페이스", "스페이스 설명", false, "forgather_official",
            "forgather@forgather.me", "", "");
        space.celebrate();

        // when
        space.update("새 이름", "새 설명", true, "new_official", "new@forgather.me", "https://forgather.me", "포트폴리오");

        // then
        assertThat(space.isCelebrating()).isTrue();
    }

    private String getString(int length) {
        return "a".repeat(Math.max(0, length));
    }
}
