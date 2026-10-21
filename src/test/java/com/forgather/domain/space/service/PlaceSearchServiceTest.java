package com.forgather.domain.space.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.forgather.global.exception.BaseException;
import com.forgather.global.external.place.PlaceSearchClient;
import com.forgather.global.external.place.dto.PlaceSearchResult;

@ExtendWith(MockitoExtension.class)
class PlaceSearchServiceTest {

    @Mock
    private PlaceSearchClient placeSearchClient;

    private PlaceSearchService placeSearchService;

    @BeforeEach
    void setUp() {
        placeSearchService = new PlaceSearchService(placeSearchClient);
    }

    @DisplayName("키워드가 없거나 공백이면 클라이언트 호출 없이 예외를 던진다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void rejectMissingKeyword(String keyword) {
        // given
        int page = 1;

        // when & then
        assertThatThrownBy(() -> placeSearchService.search(keyword, page))
            .isInstanceOf(BaseException.class);
        verifyNoInteractions(placeSearchClient);
    }

    @DisplayName("키워드가 주어지면 해당 키워드와 페이지로 클라이언트를 호출한다")
    @Test
    void searchWithGivenKeyword() {
        // given
        String keyword = "성수 갤러리";
        int page = 2;
        when(placeSearchClient.search(keyword, page)).thenReturn(new PlaceSearchResult(List.of(), false));

        // when
        placeSearchService.search(keyword, page);

        // then
        verify(placeSearchClient).search(keyword, page);
    }
}
