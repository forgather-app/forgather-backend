package com.forgather.global.auth.model;

import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.HostFixture.createHostWithLegacyPictureUrl;
import static com.forgather.fixture.HostFixture.createHostWithoutEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class HostTest {

    @DisplayName("익명화하면 개인정보가 제거되고 익명화 시각이 기록된다")
    @Test
    void anonymize() {
        // given
        Host host = createHostWithLegacyPictureUrl("https://cdn.forgather.app/hosts/1/profile/a.webp");
        host.updateProfile(null, "안녕하세요, 포게더 작가입니다.", "https://forgather.app/");

        // when
        host.anonymize();

        // then
        assertAll(
            () -> assertThat(host.getName()).isEqualTo("탈퇴한 회원"),
            () -> assertThat(host.getNickname()).isNull(),
            () -> assertThat(host.getPictureUrl()).isNull(),
            () -> assertThat(host.getEmail()).isNull(),
            () -> assertThat(host.getIntroduction()).isNull(),
            () -> assertThat(host.getLinkUrl()).isNull(),
            () -> assertThat(host.getAnonymizedAt()).isNotNull()
        );
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @DisplayName("이름이 null이면 예외가 발생한다")
        @Test
        void rejectsNullName() {
            assertThatThrownBy(() -> new Host(null, "posty@forgather.app"))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("이름");
        }

        @DisplayName("이메일이 null이면 예외가 발생한다")
        @Test
        void rejectsNullEmail() {
            assertThatThrownBy(() -> new Host("포스티", null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("이메일");
        }

        @DisplayName("이름이 공백만이면 예외가 발생한다")
        @Test
        void rejectsBlankName() {
            assertThatThrownBy(() -> new Host(" ", "posty@forgather.app"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("이름은 공백만");
        }

        @DisplayName("이메일이 빈 문자열이면 예외가 발생한다")
        @Test
        void rejectsEmptyEmail() {
            assertThatThrownBy(() -> new Host("포스티", ""))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("이메일은 공백만");
        }

        @DisplayName("이메일이 공백만이면 예외가 발생한다")
        @Test
        void rejectsBlankEmail() {
            assertThatThrownBy(() -> new Host("포스티", " "))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("이메일은 공백만");
        }
    }

    @Nested
    @DisplayName("이메일 갱신")
    class UpdateEmail {

        @DisplayName("새 이메일이 들어오면 갱신한다")
        @Test
        void updatesEmail() {
            // given
            Host host = createHostWithoutEmail("포스티");

            // when
            host.updateEmail("posty@forgather.app");

            // then
            assertThat(host.getEmail()).isEqualTo("posty@forgather.app");
        }

        @DisplayName("이메일이 없으면 기존 이메일을 유지한다")
        @Test
        void keepsEmailWhenNotProvided() {
            // given
            Host host = new Host("포스티", "posty@forgather.app");

            // when
            host.updateEmail(null);
            host.updateEmail(" ");

            // then
            assertThat(host.getEmail()).isEqualTo("posty@forgather.app");
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfile {

        @DisplayName("닉네임, 한 줄 소개, 링크를 모두 변경한다")
        @Test
        void updateProfile() {
            // given
            Host host = createHost();

            // when
            host.updateProfile("새닉네임", "안녕하세요, 포게더 작가입니다.", "https://forgather.app/");

            // then
            assertAll(
                () -> assertThat(host.getNickname()).isEqualTo("새닉네임"),
                () -> assertThat(host.getIntroduction()).isEqualTo("안녕하세요, 포게더 작가입니다."),
                () -> assertThat(host.getLinkUrl()).isEqualTo("https://forgather.app/")
            );
        }

        @DisplayName("null로 전달된 필드는 변경하지 않는다")
        @Test
        void ignoresNullFields() {
            // given
            Host host = createHost();
            host.updateProfile("닉네임", "소개", "https://forgather.app/");

            // when
            host.updateProfile(null, null, null);

            // then
            assertAll(
                () -> assertThat(host.getNickname()).isEqualTo("닉네임"),
                () -> assertThat(host.getIntroduction()).isEqualTo("소개"),
                () -> assertThat(host.getLinkUrl()).isEqualTo("https://forgather.app/")
            );
        }

        @DisplayName("빈 문자열로 전달된 한 줄 소개, 링크는 제거된다")
        @Test
        void clearsBlankFields() {
            // given
            Host host = createHost();
            host.updateProfile("닉네임", "소개", "https://forgather.app/");

            // when
            host.updateProfile(null, " ", " ");

            // then
            assertAll(
                () -> assertThat(host.getNickname()).isEqualTo("닉네임"),
                () -> assertThat(host.getIntroduction()).isNull(),
                () -> assertThat(host.getLinkUrl()).isNull()
            );
        }

        @DisplayName("닉네임을 빈 문자열로 수정하면 예외가 발생한다")
        @Test
        void rejectsBlankNickname() {
            // given
            Host host = createHost();

            // when & then
            assertThatThrownBy(() -> host.updateProfile(" ", null, null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("닉네임은 공백만 입력할 수 없습니다");
        }

        @DisplayName("닉네임 10자는 허용된다")
        @Test
        void allowsNicknameAtMaxLength() {
            // given
            Host host = createHost();

            // when
            host.updateProfile("가나다라마바사아자차", null, null);

            // then
            assertThat(host.getNickname()).isEqualTo("가나다라마바사아자차");
        }

        @DisplayName("닉네임이 10자를 초과하면 예외가 발생한다")
        @Test
        void rejectsNicknameOverMaxLength() {
            // given
            Host host = createHost();

            // when & then
            assertThatThrownBy(() -> host.updateProfile("가나다라마바사아자차카", null, null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("닉네임은 최대 10자까지 입력 가능합니다");
        }

        @DisplayName("이모지 닉네임은 눈에 보이는 글자 수 기준으로 검증된다")
        @Test
        void countsNicknameByGrapheme() {
            // given
            Host host = createHost();
            String emojiNickname = "🧑‍🧑‍🧒‍🧒".repeat(10);

            // when
            host.updateProfile(emojiNickname, null, null);

            // then
            assertThat(host.getNickname()).isEqualTo(emojiNickname);
        }

        @DisplayName("한 줄 소개 50자는 허용된다")
        @Test
        void allowsIntroductionAtMaxLength() {
            // given
            Host host = createHost();

            // when
            host.updateProfile(null, "가".repeat(50), null);

            // then
            assertThat(host.getIntroduction()).isEqualTo("가".repeat(50));
        }

        @DisplayName("한 줄 소개가 50자를 초과하면 예외가 발생한다")
        @Test
        void rejectsIntroductionOverMaxLength() {
            // given
            Host host = createHost();

            // when & then
            assertThatThrownBy(() -> host.updateProfile(null, "가".repeat(51), null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("한 줄 소개는 최대 50자까지 입력 가능합니다");
        }

        @DisplayName("링크가 http(s) URL 형식이 아니면 예외가 발생한다")
        @Test
        void rejectsInvalidLinkUrl() {
            // given
            Host host = createHost();

            // when & then
            assertThatThrownBy(() -> host.updateProfile(null, null, "forgather.app"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("링크 URL 형식이 올바르지 않습니다");
        }

        @DisplayName("링크가 2048자를 초과하면 예외가 발생한다")
        @Test
        void rejectsLinkUrlOverMaxLength() {
            // given
            Host host = createHost();
            String longLinkUrl = "https://" + "a".repeat(2041);

            // when & then
            assertThatThrownBy(() -> host.updateProfile(null, null, longLinkUrl))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("링크 URL은 최대 2048자까지 가능합니다");
        }

    }

    @DisplayName("닉네임을 10자 초과로 변경하면 예외가 발생한다")
    @Test
    void updateNicknameRejectsOverMaxLength() {
        // given
        Host host = createHost();

        // when & then
        assertThatThrownBy(() -> host.updateNickname("가나다라마바사아자차카"))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("닉네임은 최대 10자까지 입력 가능합니다");
    }

    @DisplayName("이미 익명화된 회원을 다시 익명화해도 익명화 시각이 변경되지 않는다")
    @Test
    void anonymizeIsIdempotent() {
        // given
        Host host = createHost();
        host.anonymize();
        LocalDateTime firstAnonymizedAt = host.getAnonymizedAt();

        // when
        host.anonymize();

        // then
        assertThat(host.getAnonymizedAt()).isEqualTo(firstAnonymizedAt);
    }
}
