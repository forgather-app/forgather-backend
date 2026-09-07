package com.forgather.domain.host.dto;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;

import io.swagger.v3.oas.annotations.media.Schema;

public record HostProfileResponse(

    @Schema(description = "닉네임", example = "포스티")
    String nickname,

    @Schema(description = "한 줄 소개", example = "안녕하세요, 포게더 작가입니다.")
    String introduction,

    @Schema(description = "링크 URL", example = "https://forgather.app/")
    String linkUrl,

    @Schema(description = "프로필 사진 경로 (미설정 시 null)", example = "images/prod/hosts/1/profile/UUID1.webp")
    String photoPath
) {

    public static HostProfileResponse of(Host host, HostProfilePhoto photo) {
        return new HostProfileResponse(
            host.getNickname(),
            host.getIntroduction(),
            host.getLinkUrl(),
            photo == null ? null : photo.getPath()
        );
    }
}
