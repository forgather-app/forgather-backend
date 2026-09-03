package com.forgather.global.external.social.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * https://developers.kakao.com/docs/ko/kakaologin/rest-api#request-token-response-id-token
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoIdToken(
    String iss,
    Object aud,
    String sub,
    Long iat,
    Long exp,
    Long authTime,
    String nonce,
    String nickname,
    String email
) {
}
