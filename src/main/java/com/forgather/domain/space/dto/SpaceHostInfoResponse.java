package com.forgather.domain.space.dto;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SpaceHostInfoResponse(

    @Schema(description = "호스트 공개 코드", example = "a3f2k9x1qz")
    String code,

    @Schema(description = "호스트 닉네임", example = "포스티")
    String nickname,

    @Schema(description = "호스트 프로필 사진 경로 (미설정 시 null)",
        example = "images/prod/hosts/1/profile/UUID1.webp", nullable = true)
    String photoPath
) {

    public static SpaceHostInfoResponse of(Host host, HostProfilePhoto photo) {
        return new SpaceHostInfoResponse(
            host.getCode(),
            host.getNickname(),
            (photo == null) ? null : photo.getPath()
        );
    }
}
