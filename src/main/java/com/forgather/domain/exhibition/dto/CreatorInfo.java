package com.forgather.domain.exhibition.dto;

import com.forgather.domain.host.model.Host;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreatorInfo(

    @Schema(description = "전시 호스트(작가) ID", example = "10")
    Long id,

    @Schema(description = "전시 호스트(작가) 이름", example = "김작가")
    String name
) {

    public static CreatorInfo from(Host host) {
        return new CreatorInfo(host.getId(), host.getNickname());
    }
}
