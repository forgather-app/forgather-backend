package com.forgather.global.external.place.dto;

import java.util.List;

public record PlaceSearchResult(
    List<PlaceResult> places,

    boolean hasNext
) {
}
