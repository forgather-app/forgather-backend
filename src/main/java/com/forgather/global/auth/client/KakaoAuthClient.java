package com.forgather.global.auth.client;

import org.springframework.stereotype.Component;

import com.forgather.global.config.KakaoProperties;

import lombok.RequiredArgsConstructor;

/**
 * 카카오 인증 관련 클라이언트
 * https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#request-token
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("removal")
@Component
@RequiredArgsConstructor
public class KakaoAuthClient {

    private final KakaoProperties kakaoProperties;

    public String getKakaoClientId() {
        return kakaoProperties.getClientId();
    }
}
