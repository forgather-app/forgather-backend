package com.forgather.global.external.place;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.forgather.global.exception.BaseException;
import com.forgather.global.external.ExternalCalls;
import com.forgather.global.external.ExternalOperation;
import com.forgather.global.external.exception.ExternalApiException;
import com.forgather.global.external.exception.ExternalFailureType;
import com.forgather.global.external.place.config.KakaoPlaceProperties;
import com.forgather.global.external.place.dto.KakaoPlaceSearchResponse;
import com.forgather.global.external.place.dto.KakaoPlaceSearchResponse.Document;
import com.forgather.global.external.place.dto.PlaceResult;
import com.forgather.global.external.place.dto.PlaceSearchResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KakaoPlaceSearchClient implements PlaceSearchClient {

    private static final int PAGE_SIZE = 15;

    private final RestClient restClient;
    private final KakaoPlaceProperties properties;

    @Override
    public PlaceSearchResult search(String keyword, int page) {
        if (page < 1 || page > 45) {
            throw new BaseException("카카오 장소 검색 페이지는 1부터 45까지 가능합니다. page: " + page);
        }
        URI uri = UriComponentsBuilder.fromUriString(properties.placeSearchUrl())
            .queryParam("query", "{keyword}")
            .queryParam("page", page)
            .queryParam("size", PAGE_SIZE)
            .encode()
            .buildAndExpand(keyword)
            .toUri();

        KakaoPlaceSearchResponse response = ExternalCalls.execute(ExternalOperation.KAKAO_PLACE_SEARCH, () ->
            restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
                .retrieve()
                .body(KakaoPlaceSearchResponse.class));
        validateResponse(response);

        List<PlaceResult> places = response.documents().stream()
            .map(document -> new PlaceResult(document.id(), document.placeName(), document.placeUrl(),
                document.addressName(), document.roadAddressName()))
            .toList();
        return new PlaceSearchResult(places, !places.isEmpty() && !response.meta().isEnd());
    }

    private void validateResponse(KakaoPlaceSearchResponse response) {
        if (response == null || response.meta() == null || response.meta().isEnd() == null
            || response.documents() == null || response.documents().stream().anyMatch(this::isInvalidDocument)) {
            throw new ExternalApiException(ExternalOperation.KAKAO_PLACE_SEARCH, ExternalFailureType.MALFORMED_RESPONSE,
                "카카오 장소 검색 응답 형식이 올바르지 않습니다.");
        }
    }

    private boolean isInvalidDocument(Document document) {
        return document == null || !StringUtils.hasText(document.id())
            || !StringUtils.hasText(document.placeName()) || !StringUtils.hasText(document.placeUrl());
    }
}
