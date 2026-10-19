package com.forgather.domain.space.model;

import com.forgather.global.exception.BaseException;
import com.forgather.global.util.TextLengthCounter;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpacePlace {

    private static final int MAX_PLACE_NAME_LENGTH = 50;
    private static final int PLACE_NAME_COLUMN_LENGTH = 500;
    private static final int MAX_PLACE_ID_LENGTH = 255;
    private static final int MAX_PLACE_URL_LENGTH = 2048;

    @Column(name = "place_name", nullable = false, length = PLACE_NAME_COLUMN_LENGTH)
    private String placeName;

    @Column(name = "place_id", nullable = false, length = MAX_PLACE_ID_LENGTH)
    private String placeId;

    @Column(name = "place_url", nullable = false, length = MAX_PLACE_URL_LENGTH)
    private String placeUrl;

    public SpacePlace(String placeName, String placeId, String placeUrl) {
        String newPlaceName = convertBlankToEmptyString(placeName);
        String newPlaceId = convertBlankToEmptyString(placeId);
        String newPlaceUrl = convertBlankToEmptyString(placeUrl);
        if (newPlaceName.isEmpty() && (!newPlaceId.isEmpty() || !newPlaceUrl.isEmpty())) {
            throw new BaseException("장소 정보가 있으면 장소명을 입력해야 합니다.");
        }
        validatePlaceName(newPlaceName);
        validatePlace(newPlaceId, newPlaceUrl);
        this.placeName = newPlaceName;
        this.placeId = newPlaceId;
        this.placeUrl = newPlaceUrl;
    }

    private void validatePlaceName(String placeName) {
        if (TextLengthCounter.count(placeName) > MAX_PLACE_NAME_LENGTH) {
            throw new BaseException("장소명은 최대 %d자까지 가능합니다.".formatted(MAX_PLACE_NAME_LENGTH));
        }
    }

    private void validatePlace(String placeId, String placeUrl) {
        if (placeId.isEmpty() && placeUrl.isEmpty()) {
            return;
        }
        if (placeId.isEmpty() || placeUrl.isEmpty()) {
            throw new BaseException("장소 ID와 URL은 함께 입력해야 합니다.");
        }
        if (placeId.length() > MAX_PLACE_ID_LENGTH) {
            throw new BaseException("장소 ID는 최대 %d자까지 가능합니다.".formatted(MAX_PLACE_ID_LENGTH));
        }
        if (placeUrl.length() > MAX_PLACE_URL_LENGTH) {
            throw new BaseException("장소 URL은 최대 %d자까지 가능합니다.".formatted(MAX_PLACE_URL_LENGTH));
        }
    }

    private String convertBlankToEmptyString(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
