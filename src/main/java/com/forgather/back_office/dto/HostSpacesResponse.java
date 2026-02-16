package com.forgather.back_office.dto;

import java.util.List;

import com.forgather.domain.space.model.Space;
import com.forgather.global.auth.model.Host;

public record HostSpacesResponse(
    Long hostId,
    String hostName,
    List<SimpleSpaceResponse> spaces
) {

    public static HostSpacesResponse of(Host host, List<Space> spaces) {
        return new HostSpacesResponse(
            host.getId(),
            host.getName(),
            spaces.stream()
                .map(SimpleSpaceResponse::from)
                .toList()
        );
    }
}
