package com.forgather.back_office.dto;

import java.time.LocalDateTime;

import com.forgather.global.auth.model.Host;

public record HostDetailResponse(
    Long id,
    String name,
    LocalDateTime createdAt,
    long spaceCount
) {

    public static HostDetailResponse of(Host host, long spaceCount) {
        return new HostDetailResponse(host.getId(), host.getName(), host.getCreatedAt(), spaceCount);
    }
}
