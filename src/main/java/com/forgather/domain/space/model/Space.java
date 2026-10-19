package com.forgather.domain.space.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.forgather.domain.model.SoftDeleteEntity;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.util.TextLengthCounter;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Space extends SoftDeleteEntity {

    private static final int CODE_LENGTH = 10;
    private static final int MAX_NAME_LENGTH = 30;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int DESCRIPTION_COLUMN_LENGTH = MAX_DESCRIPTION_LENGTH * 10;
    private static final int MAX_LINK_URL_LENGTH = 2048;
    private static final int MAX_LINK_NAME_LENGTH = 30;
    private static final Pattern LINK_URL_PATTERN = Pattern.compile(
        "^https?://[^\\s]+$", Pattern.CASE_INSENSITIVE
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false, length = DESCRIPTION_COLUMN_LENGTH)
    private String description;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "link_url", nullable = false, length = 2048)
    private String linkUrl;

    @Column(name = "link_name", nullable = false)
    private String linkName;

    @Embedded
    private SpaceExhibitionPeriod exhibitionPeriod;

    @Embedded
    private SpacePlace location;

    /**
     * 호스트가 지정한 '지금 축하받고 있는 스페이스'인지 여부.
     */
    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    /**
     * 스페이스를 생성한다.
     * 코드와 이름은 필수이며, 설명과 소개 링크는 생략하면 빈 문자열로 저장한다.
     * 장소 정보는 생략하면 빈 문자열로 저장한다.
     *
     * @param code        스페이스 코드 (필수, 10자)
     * @param name        스페이스 이름 (필수, 최대 30자)
     * @param description 스페이스 설명 (선택, 최대 500자)
     * @param isPublic    스페이스 공개 여부 (선택, 기본 false)
     * @param linkUrl     소개 링크 URL (선택, 최대 2048자)
     * @param linkName    소개 링크 표시 이름 (선택, 최대 30자, 링크 URL이 있을 때만 입력 가능)
     * @param exhibitionStartDate 전시 시작일 (종료일과 함께 입력)
     * @param exhibitionEndDate 전시 종료일 (시작일과 함께 입력)
     * @param placeName 장소명 (선택, 최대 50자)
     * @param placeId 카카오 장소 ID (선택, 장소명과 URL이 있을 때만 입력 가능)
     * @param placeUrl 카카오 장소 URL (선택, 장소명과 ID가 있을 때만 입력 가능)
     */
    public Space(String code, String name, String description, boolean isPublic, String linkUrl, String linkName,
        LocalDate exhibitionStartDate, LocalDate exhibitionEndDate,
        String placeName, String placeId, String placeUrl) {
        SpaceExhibitionPeriod newExhibitionPeriod = createExhibitionPeriod(exhibitionStartDate, exhibitionEndDate);
        validateRequiredFields(code, name);
        // 설명과 소개 링크의 '값 없음'은 빈 문자열로 표현한다. 검증 전에 먼저 정규화한다.
        String newDescription = convertBlankToEmptyString(description);
        String newLinkUrl = convertBlankToEmptyString(linkUrl);
        String newLinkName = convertBlankToEmptyString(linkName);

        validateCode(code);
        validateName(name);
        validateDescription(newDescription);
        validateLink(newLinkUrl, newLinkName);
        this.code = code;
        this.name = name;
        this.description = newDescription;
        this.isPublic = isPublic;
        this.linkUrl = newLinkUrl;
        this.linkName = newLinkName;
        this.exhibitionPeriod = newExhibitionPeriod;
        this.location = new SpacePlace(placeName, placeId, placeUrl);
    }

    public void updateLocation(String placeName, String placeId, String placeUrl) {
        this.location = new SpacePlace(placeName, placeId, placeUrl);
    }

    public String getPlaceName() {
        return location.getPlaceName();
    }

    public String getPlaceId() {
        return location.getPlaceId();
    }

    public String getPlaceUrl() {
        return location.getPlaceUrl();
    }

    private void validateRequiredFields(String code, String name) {
        if (code == null) {
            throw new BaseNullPointerException("스페이스 코드는 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (name == null) {
            throw new BaseNullPointerException("스페이스 이름은 null일 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    public void update(String name, String description, Boolean isPublic, String linkUrl, String linkName) {
        if (name != null) {
            validateName(name);
            this.name = name;
        }
        if (description != null) {
            validateDescription(description);
            this.description = description;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
        updateLink(linkUrl, linkName);
    }

    private void updateLink(String linkUrl, String linkName) {
        if (linkUrl == null && linkName == null) {
            return;
        }
        String newLinkUrl = (linkUrl != null) ? convertBlankToEmptyString(linkUrl) : this.linkUrl;
        String newLinkName = (linkName != null) ? convertBlankToEmptyString(linkName) : this.linkName;
        validateLink(newLinkUrl, newLinkName);
        this.linkUrl = newLinkUrl;
        this.linkName = newLinkName;
    }

    public void updateExhibitionPeriod(LocalDate startDate, LocalDate endDate) {
        this.exhibitionPeriod = createExhibitionPeriod(startDate, endDate);
    }

    private SpaceExhibitionPeriod createExhibitionPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        return new SpaceExhibitionPeriod(startDate, endDate);
    }

    public LocalDate getExhibitionStartDate() {
        return exhibitionPeriod == null ? null : exhibitionPeriod.getStartDate();
    }

    public LocalDate getExhibitionEndDate() {
        return exhibitionPeriod == null ? null : exhibitionPeriod.getEndDate();
    }

    public void feature() {
        this.isFeatured = true;
    }

    public void unfeature() {
        this.isFeatured = false;
    }

    public boolean isSameCode(String code) {
        return this.code.equals(code);
    }

    /**
     * 삭제되는 스페이스가 스스로 정리해야 할 상태를 초기화한 뒤 소프트 삭제한다.
     * 삭제 시 정리가 필요한 필드가 늘어나면 서비스가 아니라 이 메서드에 추가한다.
     */
    @Override
    public void delete() {
        unfeature();
        super.delete();
    }

    private void validateCode(String code) {
        if (code.length() != CODE_LENGTH) {
            throw new BaseException("스페이스 코드는 %d자여야 합니다.".formatted(CODE_LENGTH));
        }
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            throw new BaseException("스페이스 이름은 공백만 입력할 수 없습니다.");
        }
        if (TextLengthCounter.count(name) > MAX_NAME_LENGTH) {
            throw new BaseException("스페이스 이름은 최대 %d자까지 가능합니다.".formatted(MAX_NAME_LENGTH));
        }
    }

    private void validateDescription(String description) {
        if (TextLengthCounter.count(description) > MAX_DESCRIPTION_LENGTH) {
            throw new BaseException("스페이스 설명은 최대 %d자까지 가능합니다.".formatted(MAX_DESCRIPTION_LENGTH));
        }
    }

    private void validateLink(String linkUrl, String linkName) {
        boolean hasUrl = linkUrl != null && !linkUrl.isBlank();
        boolean hasName = linkName != null && !linkName.isBlank();
        // 표시 이름은 URL에 종속되는 선택 값이다. URL만 있는 링크는 허용하고, 이름만 있는 링크는 거부한다.
        if (!hasUrl && hasName) {
            throw new BaseException("링크 표시 이름은 링크 URL과 함께 입력해야 합니다.");
        }
        if (hasUrl) {
            validateLinkUrl(linkUrl);
        }
        if (hasName) {
            validateLinkName(linkName);
        }
    }

    private void validateLinkUrl(String linkUrl) {
        if (linkUrl.length() > MAX_LINK_URL_LENGTH) {
            throw new BaseException("링크 URL은 최대 %d자까지 가능합니다.".formatted(MAX_LINK_URL_LENGTH));
        }
        if (!LINK_URL_PATTERN.matcher(linkUrl).matches()) {
            throw new BaseException("링크 URL 형식이 올바르지 않습니다.");
        }
    }

    private void validateLinkName(String linkName) {
        if (TextLengthCounter.count(linkName) > MAX_LINK_NAME_LENGTH) {
            throw new BaseException("링크 표시 이름은 최대 %d자까지 가능합니다.".formatted(MAX_LINK_NAME_LENGTH));
        }
    }

    private String convertBlankToEmptyString(String string) {
        if (string == null || string.isBlank()) {
            return "";
        }
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Space)) {
            return false;
        }
        Space space = (Space)o;
        return id != null && Objects.equals(id, space.id);
    }

    @Override
    public int hashCode() {
        return Space.class.hashCode();
    }
}
