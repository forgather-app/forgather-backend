package com.forgather.fixture;

import java.time.LocalDate;

import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.model.Location;
import com.forgather.domain.exhibition.model.LocationType;

public class ExhibitionFixture {

    public static final String DEFAULT_TITLE = "졸업 전시회";
    public static final LocalDate DEFAULT_START_DATE = LocalDate.of(2026, 6, 1);
    public static final LocalDate DEFAULT_END_DATE = LocalDate.of(2026, 6, 30);
    public static final String DEFAULT_DESCRIPTION = "전시 설명입니다.";
    public static final String DEFAULT_ONLINE_URL = "https://forgather.app";
    public static final String DEFAULT_BASE_ADDRESS = "서울특별시 송파구";
    public static final String DEFAULT_DETAIL_ADDRESS = "루터회관";

    public static Exhibition createOnlineExhibition() {
        return new Exhibition(
            DEFAULT_TITLE, DEFAULT_START_DATE, DEFAULT_END_DATE, DEFAULT_DESCRIPTION,
            null, new Location(LocationType.ONLINE, DEFAULT_ONLINE_URL, null, null)
        );
    }

    public static Exhibition createOfflineExhibition() {
        return new Exhibition(
            DEFAULT_TITLE, DEFAULT_START_DATE, DEFAULT_END_DATE, DEFAULT_DESCRIPTION,
            null, new Location(LocationType.OFFLINE, null, DEFAULT_BASE_ADDRESS, DEFAULT_DETAIL_ADDRESS)
        );
    }

    public static Exhibition createExhibitionWithoutLocationType() {
        return new Exhibition(
            DEFAULT_TITLE, DEFAULT_START_DATE, DEFAULT_END_DATE, DEFAULT_DESCRIPTION,
            null, null
        );
    }

    public static Exhibition createOnlineExhibitionWithTitle(String title) {
        return new Exhibition(
            title, DEFAULT_START_DATE, DEFAULT_END_DATE, DEFAULT_DESCRIPTION,
            null, new Location(LocationType.ONLINE, DEFAULT_ONLINE_URL, null, null)
        );
    }

    public static Exhibition createOnlineExhibitionWithDates(LocalDate startDate, LocalDate endDate) {
        return new Exhibition(
            DEFAULT_TITLE, startDate, endDate, DEFAULT_DESCRIPTION,
            null, new Location(LocationType.ONLINE, DEFAULT_ONLINE_URL, null, null)
        );
    }

    public static Exhibition createOnlineExhibitionWithStartDate(LocalDate startDate) {
        return createOnlineExhibitionWithDates(startDate, DEFAULT_END_DATE);
    }

    public static Exhibition createOnlineExhibitionWithEndDate(LocalDate endDate) {
        return createOnlineExhibitionWithDates(DEFAULT_START_DATE, endDate);
    }

    public static Exhibition createOnlineExhibitionWithDescription(String description) {
        return new Exhibition(
            DEFAULT_TITLE, DEFAULT_START_DATE, DEFAULT_END_DATE, description,
            null, new Location(LocationType.ONLINE, DEFAULT_ONLINE_URL, null, null)
        );
    }

    public static Exhibition createOnlineExhibitionWithOperationNotice(String operationNotice) {
        return new Exhibition(
            DEFAULT_TITLE, DEFAULT_START_DATE, DEFAULT_END_DATE, DEFAULT_DESCRIPTION,
            operationNotice, new Location(LocationType.ONLINE, DEFAULT_ONLINE_URL, null, null)
        );
    }

    public static Exhibition createOnlineExhibitionWithUrl(String onlineUrl) {
        return new Exhibition(
            DEFAULT_TITLE, DEFAULT_START_DATE, DEFAULT_END_DATE, DEFAULT_DESCRIPTION,
            null, new Location(LocationType.ONLINE, onlineUrl, null, null)
        );
    }

    public static Exhibition createOfflineExhibitionWithBaseAddress(String baseAddress) {
        return new Exhibition(
            DEFAULT_TITLE, DEFAULT_START_DATE, DEFAULT_END_DATE, DEFAULT_DESCRIPTION,
            null, new Location(LocationType.OFFLINE, null, baseAddress, null)
        );
    }
}
