package com.forgather.domain.product.model;

import org.springframework.http.HttpStatus;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.domain.space.model.Space;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.util.TextLengthCounter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends SoftDeleteEntity {

    private static final int MAX_TITLE_LENGTH = 50;
    private static final int MAX_AUTHOR_NAME_LENGTH = 35;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_VIDEO_URL_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "description", length = MAX_DESCRIPTION_LENGTH, nullable = false)
    private String description;

    @Column(name = "video_url", nullable = false)
    private String videoUrl;

    @Column(name = "is_video_after_photo", nullable = false)
    private boolean isVideoAfterPhoto;

    /**
     * 임베드 영상 추가에 따른 생성자 오버라이드
     * <p>
     * 필수값은 스페이스와 작품명이며, 나머지 값은 생략하면 빈 문자열로 저장한다.
     *
     * @param space             작품이 속한 스페이스 (필수)
     * @param title             작품명 (필수, 최대 50자)
     * @param authorName        작가명 (선택, 최대 35자)
     * @param description       작품 설명 (선택, 최대 2000자)
     * @param videoUrl          임베드 영상 링크 (선택, 최대 255자)
     * @param isVideoAfterPhoto 영상이 사진 뒤에 오는지 여부 (선택, 기본 false)
     * @throws BaseNullPointerException 필수 필드가 null인 경우
     * @throws BaseException            필드 값이 유효하지 않은 경우
     */
    public Product(Space space, String title, String authorName, String description, String videoUrl,
        Boolean isVideoAfterPhoto) {
        validateRequiredFields(space, title);
        String newAuthorName = convertBlankToEmptyString(authorName);
        String newDescription = convertBlankToEmptyString(description);
        String newVideoUrl = convertBlankToEmptyString(videoUrl);

        validateTitle(title);
        validateAuthorName(newAuthorName);
        validateDescription(newDescription);
        validateVideoUrl(newVideoUrl);
        this.space = space;
        this.title = title;
        this.authorName = newAuthorName;
        this.description = newDescription;
        this.videoUrl = newVideoUrl;
        this.isVideoAfterPhoto = (isVideoAfterPhoto != null) ? isVideoAfterPhoto : false;
    }

    private void validateRequiredFields(Space space, String title) {
        if (space == null) {
            throw new BaseNullPointerException("스페이스는 null일 수 없습니다.");
        }
        if (title == null) {
            throw new BaseNullPointerException("작품명은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 임베드 영상 추가에 따른 오버라이드
     * <p>
     * 작품 정보를 부분 업데이트합니다.
     * null인 필드는 업데이트하지 않습니다.
     *
     * @param title             작품명 (선택)
     * @param authorName        작가명 (선택)
     * @param description       작품 설명 (선택)
     * @param videoUrl          임베드 영상 링크 (선택)
     * @param isVideoAfterPhoto 영상이 사진 뒤에 오는지 여부 (선택)
     * @throws BaseException 필드 값이 유효하지 않은 경우
     */
    public void update(String title, String authorName, String description, String videoUrl,
        Boolean isVideoAfterPhoto) {
        if (title != null) {
            validateTitle(title);
            this.title = title;
        }
        if (authorName != null) {
            validateAuthorName(authorName);
            this.authorName = convertBlankToEmptyString(authorName);
        }
        if (description != null) {
            validateDescription(description);
            this.description = convertBlankToEmptyString(description);
        }
        if (videoUrl != null) {
            validateVideoUrl(videoUrl);
            this.videoUrl = convertBlankToEmptyString(videoUrl);
        }
        if (isVideoAfterPhoto != null) {
            this.isVideoAfterPhoto = isVideoAfterPhoto;
        }
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new BaseException("작품명은 공백만 입력할 수 없습니다.");
        }
        if (TextLengthCounter.count(title) > MAX_TITLE_LENGTH) {
            throw new BaseException("작품명은 최대 %d자까지 입력 가능합니다.".formatted(MAX_TITLE_LENGTH));
        }
    }

    private void validateAuthorName(String authorName) {
        if (TextLengthCounter.count(authorName) > MAX_AUTHOR_NAME_LENGTH) {
            throw new BaseException("작가명은 최대 %d자까지 입력 가능합니다.".formatted(MAX_AUTHOR_NAME_LENGTH));
        }
    }

    private void validateDescription(String description) {
        if (TextLengthCounter.count(description) > MAX_DESCRIPTION_LENGTH) {
            throw new BaseException("작품 설명은 최대 %d자까지 입력 가능합니다.".formatted(MAX_DESCRIPTION_LENGTH));
        }
    }

    private void validateVideoUrl(String videoUrl) {
        if (TextLengthCounter.count(videoUrl) > MAX_VIDEO_URL_LENGTH) {
            throw new BaseException("임베드 영상 링크는 최대 %d자까지 입력 가능합니다.".formatted(MAX_VIDEO_URL_LENGTH));
        }
    }

    private String convertBlankToEmptyString(String string) {
        if (string == null || string.isBlank()) {
            return "";
        }
        return string;
    }
}
