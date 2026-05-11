package com.forgather.domain.exhibition.dto;

import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.model.LocationType;

public record LocationResponse(
    LocationType locationType,
    String url,
    String baseAddress,
    String detailAddress
) {

    public static LocationResponse from(Exhibition exhibition) {
        if (exhibition.getLocationType() == null) {
            return null;
        }
        return new LocationResponse(
            exhibition.getLocationType(),
            exhibition.getOnlineUrl(),
            exhibition.getBaseAddress(),
            exhibition.getDetailAddress()
        );
    }
}
