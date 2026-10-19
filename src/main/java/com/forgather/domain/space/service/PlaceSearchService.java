package com.forgather.domain.space.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.forgather.domain.space.dto.PlaceSearchResponse;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.place.PlaceSearchClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private final PlaceSearchClient placeSearchClient;

    public PlaceSearchResponse search(String keyword, int page) {
        if (!StringUtils.hasText(keyword)) {
            throw new BaseException("검색 키워드는 필수입니다.");
        }
        return PlaceSearchResponse.from(placeSearchClient.search(keyword, page));
    }
}
