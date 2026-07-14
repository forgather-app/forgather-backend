package com.forgather.global.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AppleLoginConfirmRequest(

    @Schema(description = "클라이언트가 받은 Apple identity token",
        example = "your-apple-id-token")
    String idToken,

    @Schema(description = "Apple authorization code", example = "your-apple-authorization-code")
    String authorizationCode,

    @Schema(description = "Apple 요청에 사용한 nonce의 원본 문자열", example = "client-generated-random-nonce")
    String rawNonce,

    @Schema(description = "Apple 최초 동의 시 받은 이름 구성요소를 클라이언트에서 조합한 문자열", example = "홍길동")
    String fullName
) {
}
