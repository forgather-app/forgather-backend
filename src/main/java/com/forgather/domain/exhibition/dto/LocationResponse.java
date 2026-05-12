package com.forgather.domain.exhibition.dto;

import com.forgather.domain.exhibition.model.Location;
import com.forgather.domain.exhibition.model.LocationType;

public record LocationResponse(
    LocationType locationType,
    String url,
    String baseAddress,
    String detailAddress
) {

    public static LocationResponse from(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationResponse(
            location.getType(),
            location.getOnlineUrl(),
            location.getBaseAddress(),
            location.getDetailAddress()
        );
    }
}
