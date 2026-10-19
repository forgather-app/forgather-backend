package com.forgather.domain.space.model;

import java.util.regex.Pattern;

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
public class SpaceLocation {

    private static final int MAX_LOCATION_NAME_LENGTH = 50;
    private static final int LOCATION_NAME_COLUMN_LENGTH = 500;
    private static final int MAX_KAKAO_PLACE_ID_LENGTH = 255;
    private static final int MAX_KAKAO_PLACE_URL_LENGTH = 2048;
    private static final String KAKAO_PLACE_URL_PREFIX = "https://place.map.kakao.com/";
    private static final Pattern KAKAO_PLACE_ID_PATTERN = Pattern.compile("[0-9]+");

    @Column(name = "location_name", nullable = false, length = LOCATION_NAME_COLUMN_LENGTH)
    private String locationName;

    @Column(name = "kakao_place_id", nullable = false, length = MAX_KAKAO_PLACE_ID_LENGTH)
    private String kakaoPlaceId;

    @Column(name = "kakao_place_url", nullable = false, length = MAX_KAKAO_PLACE_URL_LENGTH)
    private String kakaoPlaceUrl;

    public SpaceLocation(String locationName, String kakaoPlaceId, String kakaoPlaceUrl) {
        String newLocationName = convertBlankToEmptyString(locationName);
        String newKakaoPlaceId = convertBlankToEmptyString(kakaoPlaceId);
        String newKakaoPlaceUrl = convertBlankToEmptyString(kakaoPlaceUrl);
        if (newLocationName.isEmpty() && (!newKakaoPlaceId.isEmpty() || !newKakaoPlaceUrl.isEmpty())) {
            throw new BaseException("카카오 장소 정보가 있으면 장소명을 입력해야 합니다.");
        }
        validateLocationName(newLocationName);
        validateKakaoPlace(newKakaoPlaceId, newKakaoPlaceUrl);
        this.locationName = newLocationName;
        this.kakaoPlaceId = newKakaoPlaceId;
        this.kakaoPlaceUrl = newKakaoPlaceUrl;
    }

    private void validateLocationName(String locationName) {
        if (TextLengthCounter.count(locationName) > MAX_LOCATION_NAME_LENGTH) {
            throw new BaseException("장소명은 최대 %d자까지 가능합니다.".formatted(MAX_LOCATION_NAME_LENGTH));
        }
    }

    private void validateKakaoPlace(String kakaoPlaceId, String kakaoPlaceUrl) {
        if (kakaoPlaceId.isEmpty() && kakaoPlaceUrl.isEmpty()) {
            return;
        }
        if (kakaoPlaceId.isEmpty() || kakaoPlaceUrl.isEmpty()) {
            throw new BaseException("카카오 장소 ID와 URL은 함께 입력해야 합니다.");
        }
        if (kakaoPlaceId.length() > MAX_KAKAO_PLACE_ID_LENGTH) {
            throw new BaseException("카카오 장소 ID는 최대 %d자까지 가능합니다.".formatted(MAX_KAKAO_PLACE_ID_LENGTH));
        }
        if (kakaoPlaceUrl.length() > MAX_KAKAO_PLACE_URL_LENGTH) {
            throw new BaseException("카카오 장소 URL은 최대 %d자까지 가능합니다.".formatted(MAX_KAKAO_PLACE_URL_LENGTH));
        }
        if (!KAKAO_PLACE_ID_PATTERN.matcher(kakaoPlaceId).matches()) {
            throw new BaseException("카카오 장소 ID는 숫자 문자열이어야 합니다. id: " + kakaoPlaceId);
        }
        if (!kakaoPlaceUrl.equals(KAKAO_PLACE_URL_PREFIX + kakaoPlaceId)) {
            throw new BaseException("카카오 장소 URL은 ID에 해당하는 정확한 주소여야 합니다. url: " + kakaoPlaceUrl);
        }
    }

    private String convertBlankToEmptyString(String value) {
        return value == null || value.isBlank() ? "" : value;
    }
}
