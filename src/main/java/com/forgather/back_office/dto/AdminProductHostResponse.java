package com.forgather.back_office.dto;

import com.forgather.domain.host.model.Host;

public record AdminProductHostResponse(
    Long id,
    String name,
    String nickname
) {

    public static AdminProductHostResponse from(Host host) {
        return new AdminProductHostResponse(host.getId(), host.getName(), host.getNickname());
    }
}
