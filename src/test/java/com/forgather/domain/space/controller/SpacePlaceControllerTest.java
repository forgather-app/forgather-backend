package com.forgather.domain.space.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.space.service.PlaceSearchService;
import com.forgather.fake.FakePlaceSearchClient;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.config.JwtProperties;
import com.forgather.global.auth.resolver.LoginHostArgumentResolver;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.exception.GlobalExceptionHandler;
import com.forgather.global.external.ExternalOperation;
import com.forgather.global.external.exception.ExternalApiException;
import com.forgather.global.external.exception.ExternalFailureType;
import com.forgather.global.external.place.dto.PlaceSearchResult;

import jakarta.servlet.http.Cookie;

class SpacePlaceControllerTest {

    private FakePlaceSearchClient client;
    private MockMvc mockMvc;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        client = new FakePlaceSearchClient();
        tokenProvider = new JwtTokenProvider(new JwtProperties(3600L, 86400L, "test-secret-key-for-place-search-at-least-32-bytes"));
        HostRepository hosts = mock(HostRepository.class);
        when(hosts.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(HostFixture.createHostWithId(1L)));
        mockMvc = MockMvcBuilders.standaloneSetup(new SpacePlaceController(new PlaceSearchService(client)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new LoginHostArgumentResolver(tokenProvider, hosts))
            .build();
    }

    @DisplayName("로그인한 호스트는 중립 필드명과 원본 URL로 장소 후보를 조회한다")
    @Test
    void searchWithDefaultPage() throws Exception {
        // given
        MockHttpServletRequestBuilder request = authenticated().param("keyword", "성수 갤러리");

        // when & then
        mockMvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.hasNext").value(true))
            .andExpect(jsonPath("$.data.places[0].placeId").value("26338954"))
            .andExpect(jsonPath("$.data.places[0].placeName").value("성수 갤러리"))
            .andExpect(jsonPath("$.data.places[0].placeUrl").value("http://place.map.kakao.com/26338954"))
            .andExpect(jsonPath("$.data.places[0].address").value("서울 성동구 성수동"))
            .andExpect(jsonPath("$.data.places[0].roadAddress").value("서울 성동구 연무장길 1"))
            .andExpect(jsonPath("$.data.places[0].kakaoPlaceId").doesNotExist());
        assertThat(client.keyword).isEqualTo("성수 갤러리");
        assertThat(client.page).isEqualTo(1);
    }

    @DisplayName("요청한 페이지와 마지막 페이지 여부를 반환한다")
    @Test
    void searchNextPage() throws Exception {
        // given
        client.result = new PlaceSearchResult(client.result.places(), false);

        // when & then
        mockMvc.perform(authenticated().param("keyword", "갤러리").param("page", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.hasNext").value(false));
        assertThat(client.page).isEqualTo(2);
    }

    @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
    @Test
    void searchWithoutResults() throws Exception {
        // given
        client.result = new PlaceSearchResult(List.of(), false);

        // when & then
        mockMvc.perform(authenticated().param("keyword", "없는 장소"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.places").isEmpty())
            .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @DisplayName("쿠키가 없거나 유효하지 않으면 외부 호출 없이 인증 오류를 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-token"})
    void rejectUnauthenticated(String token) throws Exception {
        // given
        MockHttpServletRequestBuilder request = get("/spaces/places").param("keyword", "갤러리");
        if (!token.isEmpty()) {
            request.cookie(new Cookie("access_token", token));
        }

        // when & then
        mockMvc.perform(request).andExpect(status().isUnauthorized());
        assertThat(client.callCount).isZero();
    }

    @DisplayName("관리자 토큰이나 존재하지 않는 호스트의 토큰으로는 검색할 수 없다")
    @Test
    void rejectNonHost() throws Exception {
        // given
        List<String> tokens = List.of(tokenProvider.generateAdminAccessToken(1L), tokenProvider.generateAccessToken(2L));

        // when & then
        for (String token : tokens) {
            mockMvc.perform(get("/spaces/places").param("keyword", "갤러리").cookie(new Cookie("access_token", token)))
                .andExpect(status().isUnauthorized());
        }
        assertThat(client.callCount).isZero();
    }

    @DisplayName("비어 있거나 공백인 키워드는 외부 호출 없이 400을 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void rejectBlankKeyword(String keyword) throws Exception {
        // given
        MockHttpServletRequestBuilder request = authenticated().param("keyword", keyword);

        // when & then
        mockMvc.perform(request).andExpect(status().isBadRequest());
        assertThat(client.callCount).isZero();
    }

    @DisplayName("키워드를 누락하면 외부 호출 없이 400을 반환한다")
    @Test
    void rejectMissingKeyword() throws Exception {
        // given
        MockHttpServletRequestBuilder request = authenticated();

        // when & then
        mockMvc.perform(request).andExpect(status().isBadRequest());
        assertThat(client.callCount).isZero();
    }

    @DisplayName("페이지가 숫자가 아니면 외부 호출 없이 400을 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"abc"})
    void rejectInvalidPage(String page) throws Exception {
        // given
        MockHttpServletRequestBuilder request = authenticated().param("keyword", "갤러리").param("page", page);

        // when & then
        mockMvc.perform(request).andExpect(status().isBadRequest());
        assertThat(client.callCount).isZero();
    }

    @DisplayName("외부 장애는 기존 오류 규약으로 반환하며 검색 결과 없음과 구분한다")
    @ParameterizedTest
    @CsvSource({"RATE_LIMITED,503,EXTERNAL_API_UNAVAILABLE", "UPSTREAM_ERROR,503,EXTERNAL_API_UNAVAILABLE",
        "READ_TIMEOUT,503,EXTERNAL_API_UNAVAILABLE", "CONNECT_TIMEOUT,503,EXTERNAL_API_UNAVAILABLE",
        "CALLER_ERROR,500,INTERNAL_ERROR", "MALFORMED_RESPONSE,500,INTERNAL_ERROR"})
    void propagateExternalFailure(ExternalFailureType type, int statusCode, String code) throws Exception {
        // given
        client.failure = new ExternalApiException(ExternalOperation.KAKAO_PLACE_SEARCH, type, "secret upstream body");

        // when & then
        mockMvc.perform(authenticated().param("keyword", "갤러리"))
            .andExpect(status().is(statusCode))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @DisplayName("서비스는 검색 구현체에 페이지를 전달하고 제공자의 상한을 직접 제한하지 않는다")
    @Test
    void delegateProviderPagePolicy() throws Exception {
        // given
        MockHttpServletRequestBuilder request = authenticated().param("keyword", "갤러리").param("page", "46");

        // when & then
        mockMvc.perform(request).andExpect(status().isOk());
        assertThat(client.page).isEqualTo(46);
    }

    private MockHttpServletRequestBuilder authenticated() {
        return get("/spaces/places").cookie(new Cookie("access_token", tokenProvider.generateAccessToken(1L)));
    }
}
