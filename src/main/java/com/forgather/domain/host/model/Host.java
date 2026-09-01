package com.forgather.domain.host.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.domain.term.model.TermType;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.util.TextLengthCounter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Host extends SoftDeleteEntity {
    public static final int CODE_LENGTH = 10;
    private static final int MAX_NICKNAME_LENGTH = 10;
    private static final int MAX_INTRODUCTION_LENGTH = 50;
    private static final int MAX_LINK_URL_LENGTH = 2048;
    private static final int NICKNAME_COLUMN_LENGTH = MAX_NICKNAME_LENGTH * 10;
    private static final int INTRODUCTION_COLUMN_LENGTH = MAX_INTRODUCTION_LENGTH * 10;
    private static final String ANONYMIZED_NAME = "탈퇴한 회원";
    private static final Pattern LINK_URL_PATTERN = Pattern.compile(
        "^https?://[^\\s]+$"
    );
    private static final Pattern CODE_PATTERN = Pattern.compile(
        "^[0-9a-z]{" + CODE_LENGTH + "}$"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 외부에 노출하는 공개 식별자. 순차 PK(id) 대신 이 값을 URL에 사용한다.
     */
    @Column(name = "code", nullable = false, length = CODE_LENGTH)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "nickname", length = NICKNAME_COLUMN_LENGTH)
    private String nickname;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "email")
    private String email;

    @Column(name = "introduction", length = INTRODUCTION_COLUMN_LENGTH)
    private String introduction;

    @Column(name = "link_url", length = MAX_LINK_URL_LENGTH)
    private String linkUrl;

    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    public Host(String code, String name, String email) {
        validateRequiredFields(code, name, email);
        validateCode(code);
        validateName(name);
        validateEmail(email);
        this.code = code;
        this.name = name;
        this.email = email;
    }

    /**
     * 익명화 이후나 이메일 저장 이전에 가입한 회원은 email이 비어 있을 수 있어 컬럼은 nullable이지만,
     * 신규 생성 시점에는 소셜 로그인이 항상 이메일을 제공하므로 필수로 받는다.
     */
    private void validateRequiredFields(String code, String name, String email) {
        if (code == null) {
            throw new BaseNullPointerException("호스트 코드는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (name == null) {
            throw new BaseNullPointerException("이름은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (email == null) {
            throw new BaseNullPointerException("이메일은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * RandomCodeGenerator가 만드는 형식과 동일한 계약을 강제한다.
     * DB 컬럼에는 CHECK 제약이 없어 이 검증이 유일한 방어선이다.
     */
    private void validateCode(String code) {
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BaseException("호스트 코드는 영문 소문자와 숫자 %d자여야 합니다.".formatted(CODE_LENGTH));
        }
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    /**
     * 소셜 로그인 시 전달된 이메일로 갱신한다. 이메일이 없으면 기존 값을 유지한다.
     */
    public void updateEmail(String email) {
        if (email == null || email.isBlank() || email.equals(this.email)) {
            return;
        }
        this.email = email;
    }

    /**
     * 프로필을 수정한다. null로 전달된 필드는 변경하지 않는다.
     *
     * @param nickname     닉네임 (최대 10자, 공백만은 불가)
     * @param introduction 한 줄 소개 (최대 50자, 공백만 전달 시 제거)
     * @param linkUrl      링크 URL (http(s), 최대 2048자, 공백만 전달 시 제거)
     */
    public void updateProfile(String nickname, String introduction, String linkUrl) {
        if (nickname != null) {
            validateNickname(nickname);
            this.nickname = nickname;
        }
        if (introduction != null) {
            validateIntroduction(introduction);
            this.introduction = introduction.isBlank() ? null : introduction;
        }
        if (linkUrl != null) {
            validateLinkUrl(linkUrl);
            this.linkUrl = linkUrl.isBlank() ? null : linkUrl;
        }
    }

    private void validateIntroduction(String introduction) {
        int length = TextLengthCounter.count(introduction);
        if (length > MAX_INTRODUCTION_LENGTH) {
            throw new BaseException("한 줄 소개는 최대 %d자까지 입력 가능합니다. introduction.length: %d"
                .formatted(MAX_INTRODUCTION_LENGTH, length));
        }
    }

    private void validateLinkUrl(String linkUrl) {
        if (linkUrl.isBlank()) {
            return;
        }
        if (linkUrl.length() > MAX_LINK_URL_LENGTH) {
            throw new BaseException("링크 URL은 최대 %d자까지 가능합니다.".formatted(MAX_LINK_URL_LENGTH));
        }
        if (!LINK_URL_PATTERN.matcher(linkUrl).matches()) {
            throw new BaseException("링크 URL 형식이 올바르지 않습니다.");
        }
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            throw new BaseException("이름은 공백만 입력할 수 없습니다.");
        }
    }

    private void validateEmail(String email) {
        if (email.isBlank()) {
            throw new BaseException("이메일은 공백만 입력할 수 없습니다.");
        }
    }

    private void validateNickname(String nickname) {
        if (nickname == null) {
            throw new BaseNullPointerException("닉네임은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (nickname.isBlank()) {
            throw new BaseException("닉네임은 공백만 입력할 수 없습니다.");
        }
        int length = TextLengthCounter.count(nickname);
        if (length > MAX_NICKNAME_LENGTH) {
            throw new BaseException("닉네임은 최대 %d자까지 입력 가능합니다. nickname.length: %d"
                .formatted(MAX_NICKNAME_LENGTH, length));
        }
    }

    public void anonymize() {
        if (anonymizedAt != null) {
            return;
        }
        this.name = ANONYMIZED_NAME;
        this.nickname = null;
        this.pictureUrl = null;
        this.email = null;
        this.introduction = null;
        this.linkUrl = null;
        this.anonymizedAt = LocalDateTime.now();
    }

    public boolean hasValidNickname() {
        return nickname != null
            && !nickname.isBlank()
            && TextLengthCounter.count(nickname) <= MAX_NICKNAME_LENGTH;
    }

    /**
     * 온보딩 완료 여부를 판정한다. 닉네임이 유효하고 필수 약관에 모두 동의한 상태여야 한다.
     */
    public boolean isOnboardingCompleted(Set<TermType> agreedTermTypes) {
        return hasValidNickname() && agreedTermTypes.containsAll(TermType.requiredTypes());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Host))
            return false;
        Host host = (Host)o;
        return id != null && Objects.equals(id, host.id);
    }

    @Override
    public int hashCode() {
        return Host.class.hashCode();
    }
}
