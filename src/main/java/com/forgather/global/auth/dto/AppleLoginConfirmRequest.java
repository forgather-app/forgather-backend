package com.forgather.global.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;

public record AppleLoginConfirmRequest(

    @JsonAlias("id_token")
    @Schema(description = "Apple identity token", example = "your-apple-id-token")
    String idToken,

    @Schema(description = "Apple 최초 동의 시 내려주는 user JSON 문자열")
    String user,

    @JsonAlias("raw_nonce")
    @Schema(description = "Apple 요청에 사용한 nonce의 원본 문자열", example = "client-generated-random-nonce")
    String rawNonce
) {
}
