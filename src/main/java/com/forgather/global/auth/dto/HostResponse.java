package com.forgather.global.auth.dto;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;

import io.swagger.v3.oas.annotations.media.Schema;

public record HostResponse(

    @Schema(description = "호스트 ID", example = "1")
    Long id,

    @Schema(description = "호스트 닉네임", example = "홍길동")
    String name,

    @Schema(description = "호스트 프로필 사진 경로 (미설정 시 null)",
        example = "images/prod/hosts/1/profile/UUID1.webp")
    String photoPath,

    @Schema(description = "온보딩 완료 여부", example = "true")
    boolean onboardingCompleted
) {

    public static HostResponse of(Host host, HostProfilePhoto photo, boolean onboardingCompleted) {
        return new HostResponse(
            host.getId(),
            host.getNickname(),
            photo == null ? null : photo.getPath(),
            onboardingCompleted
        );
    }
}
