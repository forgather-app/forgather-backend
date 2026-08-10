package com.forgather.domain.host.dto;

import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.global.auth.model.Host;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 비로그인 방문자에게 노출하는 호스트 프로필.
 * 개인정보(name, email)는 담지 않는다.
 */
public record PublicHostProfileResponse(

    @Schema(description = "호스트 공개 코드", example = "a3f2k9x1qz")
    String hostCode,

    @Schema(description = "닉네임", example = "포스티")
    String nickname,

    @Schema(description = "한 줄 소개", example = "안녕하세요, 포게더 작가입니다.")
    String introduction,

    @Schema(description = "링크 URL", example = "https://forgather.app/")
    String linkUrl,

    @Schema(description = "프로필 사진 경로 (미설정 시 null)", example = "photogather/v2/hosts/1/profile/UUID1.webp")
    String photoPath
) {

    public static PublicHostProfileResponse of(Host host, HostProfilePhoto photo) {
        return new PublicHostProfileResponse(
            host.getCode(),
            host.getNickname(),
            host.getIntroduction(),
            host.getLinkUrl(),
            photo == null ? null : photo.getPath()
        );
    }
}
