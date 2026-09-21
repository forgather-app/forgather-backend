package com.forgather.back_office.dto;

import java.util.List;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.space.model.Space;

public record HostSpacesResponse(
    Long hostId,
    String hostName,
    List<SimpleSpaceResponse> spaces
) {

    public static HostSpacesResponse of(Host host, List<Space> spaces) {
        return new HostSpacesResponse(
            host.getId(),
            host.getNickname(),
            spaces.stream()
                .map(SimpleSpaceResponse::from)
                .toList()
        );
    }
}
