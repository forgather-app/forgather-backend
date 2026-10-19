package com.forgather.global.external.place;

import com.forgather.global.external.place.dto.PlaceSearchResult;

public interface PlaceSearchClient {

    PlaceSearchResult search(String keyword, int page);
}
