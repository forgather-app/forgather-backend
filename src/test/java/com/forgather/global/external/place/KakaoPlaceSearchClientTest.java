package com.forgather.global.external.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.forgather.global.exception.BaseException;
import com.forgather.global.external.exception.ExternalApiException;
import com.forgather.global.external.exception.ExternalFailureType;
import com.forgather.global.external.place.config.KakaoPlaceProperties;
import com.forgather.global.external.place.dto.PlaceResult;
import com.forgather.global.external.place.dto.PlaceSearchResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class KakaoPlaceSearchClientTest {

    private WireMockServer wireMock;
    private PlaceSearchClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        client = new KakaoPlaceSearchClient(RestClient.create(), kakaoPlaceProperties());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @DisplayName("키워드를 인코딩하고 페이지와 15건 고정 조건으로 요청해 중립 결과로 변환한다")
    @Test
    void searchWithEncodedKeyword() {
        // given
        String url = "/v2/local/search/keyword.json?query=%EC%84%B1%EC%88%98%20%26%20%EA%B0%A4%EB%9F%AC%EB%A6%AC&page=2&size=15";
        wireMock.stubFor(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(response(false, "http://place.map.kakao.com/26338954"))));

        // when
        PlaceSearchResult result = client.search("성수 & 갤러리", 2);

        // then
        assertAll(
            () -> assertThat(result.hasNext()).isTrue(),
            () -> assertThat(result.places()).containsExactly(new PlaceResult("26338954", "성수 갤러리",
                "http://place.map.kakao.com/26338954", "서울 성동구 성수동", ""))
        );
        wireMock.verify(getRequestedFor(urlEqualTo(url))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo("KakaoAK test-rest-key")));
    }

    @DisplayName("카카오가 마지막 페이지이면 다음 페이지가 없음을 반환한다")
    @Test
    void lastPage() {
        // given
        wireMock.stubFor(get(urlEqualTo(searchUrl()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(response(true, "https://place.map.kakao.com/26338954"))));

        // when
        PlaceSearchResult result = client.search("gallery", 1);

        // then
        assertThat(result.hasNext()).isFalse();
    }

    @DisplayName("카카오 결과가 없으면 빈 목록과 다음 페이지 없음을 반환한다")
    @Test
    void noResults() {
        // given
        wireMock.stubFor(get(urlEqualTo(searchUrl()))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"meta\":{\"is_end\":true},\"documents\":[]}")));

        // when
        PlaceSearchResult result = client.search("gallery", 1);

        // then
        assertAll(
            () -> assertThat(result.places()).isEmpty(),
            () -> assertThat(result.hasNext()).isFalse()
        );
    }

    @DisplayName("카카오 오류 상태를 기존 외부 오류 규약으로 변환한다")
    @ParameterizedTest
    @CsvSource({"401,CALLER_ERROR,500", "403,CALLER_ERROR,500", "429,RATE_LIMITED,503", "500,UPSTREAM_ERROR,503"})
    void httpFailure(int status, String type, int expectedStatus) {
        // given
        wireMock.stubFor(get(urlEqualTo(searchUrl())).willReturn(aResponse().withStatus(status)));

        // when & then
        assertThatThrownBy(() -> client.search("gallery", 1))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException error = (ExternalApiException)thrown;
                assertAll(
                    () -> assertThat(error.getType().name()).isEqualTo(type),
                    () -> assertThat(error.getStatusCode()).isEqualTo(expectedStatus)
                );
            });
    }

    @DisplayName("응답 대기 시간 초과를 503으로 변환한다")
    @Test
    void readTimeout() {
        // given
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(500));
        requestFactory.setReadTimeout(Duration.ofMillis(500));
        client = new KakaoPlaceSearchClient(RestClient.builder().requestFactory(requestFactory).build(), kakaoPlaceProperties());
        wireMock.stubFor(get(urlEqualTo(searchUrl()))
            .willReturn(aResponse()
                .withFixedDelay(1_500)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(response(true, "http://place.map.kakao.com/26338954"))));

        // when & then
        assertThatThrownBy(() -> client.search("gallery", 1))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException error = (ExternalApiException)thrown;
                assertAll(
                    () -> assertThat(error.getType()).isEqualTo(ExternalFailureType.READ_TIMEOUT),
                    () -> assertThat(error.getStatusCode()).isEqualTo(503)
                );
            });
        wireMock.verify(getRequestedFor(urlEqualTo(searchUrl())));
    }

    @DisplayName("성공 상태라도 응답 구조가 잘못되었으면 장애로 처리한다")
    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{bad json", "{\"meta\":{},\"documents\":[]}",
        "{\"meta\":{\"is_end\":true}}", "{\"meta\":{\"is_end\":true},\"documents\":[{}]}"})
    void malformedResponse(String body) {
        // given
        wireMock.stubFor(get(urlEqualTo(searchUrl()))
            .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(body)));

        // when & then
        assertThatThrownBy(() -> client.search("gallery", 1))
            .isInstanceOf(ExternalApiException.class)
            .extracting("type").asString().isEqualTo("MALFORMED_RESPONSE");
    }

    @DisplayName("카카오 페이지 범위를 벗어나면 외부 호출 없이 400 예외를 반환한다")
    @ParameterizedTest
    @ValueSource(ints = {0, -1, 46})
    void rejectInvalidPage(int page) {
        // given

        // when & then
        assertThatThrownBy(() -> client.search("gallery", page))
            .isInstanceOf(BaseException.class)
            .extracting("statusCode").isEqualTo(400);
        wireMock.verify(0, anyRequestedFor(anyUrl()));
    }

    @DisplayName("카카오가 허용하는 페이지 경계에서는 검색을 요청한다")
    @ParameterizedTest
    @ValueSource(ints = {1, 45})
    void searchAtPageBoundary(int page) {
        // given
        String url = "/v2/local/search/keyword.json?query=gallery&page=" + page + "&size=15";
        wireMock.stubFor(get(urlEqualTo(url))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"meta\":{\"is_end\":true},\"documents\":[]}")));

        // when
        PlaceSearchResult result = client.search("gallery", page);

        // then
        assertThat(result.places()).isEmpty();
        wireMock.verify(getRequestedFor(urlEqualTo(url)));
    }

    @DisplayName("카카오가 주소를 null로 반환하면 변환 없이 그대로 전달한다")
    @Test
    void preserveNullAddresses() {
        // given
        String body = response(true, "http://place.map.kakao.com/26338954")
            .replace("\"address_name\":\"서울 성동구 성수동\"", "\"address_name\":null")
            .replace("\"road_address_name\":\"\"", "\"road_address_name\":null");
        wireMock.stubFor(get(urlEqualTo(searchUrl()))
            .willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(body)));

        // when
        PlaceResult place = client.search("gallery", 1).places().getFirst();

        // then
        assertAll(
            () -> assertThat(place.address()).isNull(),
            () -> assertThat(place.roadAddress()).isNull()
        );
    }

    private KakaoPlaceProperties kakaoPlaceProperties() {
        return new KakaoPlaceProperties("test-rest-key", wireMock.baseUrl() + "/v2/local/search/keyword.json");
    }

    private String searchUrl() {
        return "/v2/local/search/keyword.json?query=gallery&page=1&size=15";
    }

    private String response(boolean isEnd, String url) {
        return """
            {"meta":{"total_count":16,"pageable_count":16,"is_end":%s},"documents":[
              {"id":"26338954","place_name":"성수 갤러리","place_url":"%s",
               "address_name":"서울 성동구 성수동","road_address_name":"","category_name":"문화,예술",
               "category_group_code":"CT1","category_group_name":"문화시설","phone":"","x":"127.0","y":"37.5"}
            ]}
            """.formatted(isEnd, url);
    }
}
