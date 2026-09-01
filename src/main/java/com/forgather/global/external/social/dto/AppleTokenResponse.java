package com.forgather.global.external.social.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AppleTokenResponse(
    String accessToken,
    String tokenType,
    Long expiresIn,
    String refreshToken,
    String idToken
) {
}
