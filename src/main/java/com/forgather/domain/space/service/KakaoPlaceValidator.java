package com.forgather.domain.space.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.forgather.global.exception.BaseException;

@Component
public class KakaoPlaceValidator {

    private static final String HTTP_PLACE_URL_PREFIX = "http://place.map.kakao.com/";
    private static final String HTTPS_PLACE_URL_PREFIX = "https://place.map.kakao.com/";
    private static final Pattern PLACE_ID_PATTERN = Pattern.compile("[0-9]+");

    public void validate(String placeId, String placeUrl) {
        if (placeId == null || placeId.isBlank() || placeUrl == null || placeUrl.isBlank()) {
            return;
        }
        if (!PLACE_ID_PATTERN.matcher(placeId).matches()) {
            throw new BaseException("카카오 장소 ID는 숫자 문자열이어야 합니다. id: " + placeId);
        }
        if (!placeUrl.equals(HTTP_PLACE_URL_PREFIX + placeId)
            && !placeUrl.equals(HTTPS_PLACE_URL_PREFIX + placeId)) {
            throw new BaseException("카카오 장소 URL은 ID에 해당하는 정확한 주소여야 합니다. url: " + placeUrl);
        }
    }
}
