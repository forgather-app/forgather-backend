package com.forgather.domain.exhibition.dto;

import com.forgather.global.auth.model.Host;

public record CreatorInfo(Long id, String name) {

    public static CreatorInfo from(Host host) {
        return new CreatorInfo(host.getId(), host.getName());
    }
}
