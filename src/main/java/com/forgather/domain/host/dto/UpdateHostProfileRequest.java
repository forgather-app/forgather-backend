package com.forgather.domain.host.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 수정 요청. null인 필드는 변경하지 않고, 빈 문자열은 값을 제거한다. (닉네임은 빈 문자열 불가)")
public record UpdateHostProfileRequest(

    @Schema(description = "닉네임 (최대 10자)", example = "포스티", nullable = true)
    String nickname,

    @Schema(description = "한 줄 소개 (최대 50자, 빈 문자열이면 제거)", example = "안녕하세요, 포게더 작가입니다.", nullable = true)
    String introduction,

    @Schema(description = "링크 URL (http(s), 최대 2048자, 빈 문자열이면 제거)", example = "https://forgather.app/", nullable = true)
    String linkUrl,

    @Schema(description = "프로필 사진 URL (http(s), 최대 255자, 빈 문자열이면 제거)",
        example = "https://cdn.forgather.app/hosts/1/profile/UUID1.webp", nullable = true)
    String pictureUrl
) {
}
