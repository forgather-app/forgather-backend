package com.forgather.global.auth.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.forgather.domain.model.SoftDeleteEntity;
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

    private static final int MAX_NICKNAME_LENGTH = 10;
    private static final int MAX_INTRODUCTION_LENGTH = 50;
    private static final int MAX_LINK_URL_LENGTH = 2048;
    private static final int MAX_PICTURE_URL_LENGTH = 255;
    // 검증은 grapheme(눈에 보이는 글자) 기준, VARCHAR는 코드포인트 기준이라
    // 컬럼은 검증 한도 × 10(RGI 표준 최장 이모지 = 10 코드포인트)으로 잡는다
    private static final int NICKNAME_COLUMN_LENGTH = MAX_NICKNAME_LENGTH * 10;
    private static final int INTRODUCTION_COLUMN_LENGTH = MAX_INTRODUCTION_LENGTH * 10;
    private static final String ANONYMIZED_NAME = "탈퇴한 회원";
    private static final Pattern PROFILE_URL_PATTERN = Pattern.compile(
        "^https?://[^\\s]+$"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Host(String name, String pictureUrl) {
        this(name, pictureUrl, null);
    }

    public Host(String name, String pictureUrl, String email) {
        validateName(name);
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.email = email;
    }

    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    /**
     * 프로필을 수정한다. null로 전달된 필드는 변경하지 않는다.
     *
     * @param nickname     닉네임 (최대 10자, 공백만은 불가)
     * @param introduction 한 줄 소개 (최대 50자, 공백만 전달 시 제거)
     * @param linkUrl      링크 URL (http(s), 최대 2048자, 공백만 전달 시 제거)
     * @param pictureUrl   프로필 사진 URL (http(s), 최대 255자, 공백만 전달 시 제거)
     */
    public void updateProfile(String nickname, String introduction, String linkUrl, String pictureUrl) {
        if (nickname != null) {
            validateNickname(nickname);
            this.nickname = nickname;
        }
        if (introduction != null) {
            this.introduction = normalizeIntroduction(introduction);
        }
        if (linkUrl != null) {
            this.linkUrl = normalizeLinkUrl(linkUrl);
        }
        if (pictureUrl != null) {
            this.pictureUrl = normalizePictureUrl(pictureUrl);
        }
    }

    private String normalizeIntroduction(String introduction) {
        if (introduction.isBlank()) {
            return null;
        }
        int length = TextLengthCounter.count(introduction);
        if (length > MAX_INTRODUCTION_LENGTH) {
            throw new BaseException("한 줄 소개는 최대 %d자까지 입력 가능합니다. introduction.length: %d"
                .formatted(MAX_INTRODUCTION_LENGTH, length));
        }
        return introduction;
    }

    private String normalizeLinkUrl(String linkUrl) {
        if (linkUrl.isBlank()) {
            return null;
        }
        if (linkUrl.length() > MAX_LINK_URL_LENGTH) {
            throw new BaseException("링크 URL은 최대 %d자까지 가능합니다.".formatted(MAX_LINK_URL_LENGTH));
        }
        if (!PROFILE_URL_PATTERN.matcher(linkUrl).matches()) {
            throw new BaseException("링크 URL 형식이 올바르지 않습니다.");
        }
        return linkUrl;
    }

    private String normalizePictureUrl(String pictureUrl) {
        if (pictureUrl.isBlank()) {
            return null;
        }
        if (pictureUrl.length() > MAX_PICTURE_URL_LENGTH) {
            throw new BaseException("프로필 사진 URL은 최대 %d자까지 가능합니다.".formatted(MAX_PICTURE_URL_LENGTH));
        }
        if (!PROFILE_URL_PATTERN.matcher(pictureUrl).matches()) {
            throw new BaseException("프로필 사진 URL 형식이 올바르지 않습니다.");
        }
        return pictureUrl;
    }

    private void validateName(String name) {
        if (name == null) {
            throw new BaseNullPointerException("이름은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (name.isBlank()) {
            throw new BaseException("이름은 공백만 입력할 수 없습니다.");
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
