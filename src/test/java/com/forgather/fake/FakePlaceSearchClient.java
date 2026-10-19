package com.forgather.fake;

import java.util.List;

import com.forgather.global.external.place.PlaceSearchClient;
import com.forgather.global.external.place.dto.PlaceResult;
import com.forgather.global.external.place.dto.PlaceSearchResult;

public class FakePlaceSearchClient implements PlaceSearchClient {

    public PlaceSearchResult result = new PlaceSearchResult(List.of(
        new PlaceResult("26338954", "성수 갤러리", "http://place.map.kakao.com/26338954", "서울 성동구 성수동", "서울 성동구 연무장길 1")
    ), true);
    public RuntimeException failure;
    public String keyword;
    public int page;
    public int callCount;

    @Override
    public PlaceSearchResult search(String keyword, int page) {
        this.keyword = keyword;
        this.page = page;
        callCount++;
        if (failure != null) {
            throw failure;
        }
        return result;
    }
}
