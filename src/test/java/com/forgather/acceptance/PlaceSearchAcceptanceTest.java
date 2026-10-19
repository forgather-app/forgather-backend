package com.forgather.acceptance;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.external.place.PlaceSearchClient;
import com.forgather.global.external.place.dto.PlaceResult;
import com.forgather.global.external.place.dto.PlaceSearchResult;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@DisplayName("인수 테스트: 장소 검색")
@AutoConfigureMockMvc
class PlaceSearchAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private PlaceSearchClient placeSearchClient;

    private String token;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        token = jwtTokenProvider.generateAccessToken(host.getId());
    }

    @DisplayName("키워드를 누락하면 클라이언트 호출 없이 400 오류를 반환한다")
    @Test
    void rejectMissingKeyword() {
        // given
        // 로그인 쿠키만 전달하고 키워드는 생략한다.

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
            .when()
            .get("/spaces/places")
            .then()
            .statusCode(400)
            .body("code", equalTo("BAD_REQUEST"))
            .body("data", nullValue());
        verifyNoInteractions(placeSearchClient);
    }

    @DisplayName("입력한 키워드로 검색한 장소 목록과 다음 페이지 여부를 반환한다")
    @Test
    void searchWithGivenKeyword() {
        // given
        String keyword = "성수 갤러리";
        when(placeSearchClient.search(keyword, 1)).thenReturn(new PlaceSearchResult(List.of(
            new PlaceResult("26338954", "성수 갤러리", "http://place.map.kakao.com/26338954",
                "서울 성동구 성수동", "서울 성동구 연무장길 1")
        ), true));

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
            .queryParam("keyword", keyword)
            .when()
            .get("/spaces/places")
            .then()
            .statusCode(200)
            .body("code", equalTo("SUCCESS"))
            .body("data.places", hasSize(1))
            .body("data.places[0].placeId", equalTo("26338954"))
            .body("data.places[0].placeName", equalTo("성수 갤러리"))
            .body("data.places[0].placeUrl", equalTo("http://place.map.kakao.com/26338954"))
            .body("data.places[0].address", equalTo("서울 성동구 성수동"))
            .body("data.places[0].roadAddress", equalTo("서울 성동구 연무장길 1"))
            .body("data.hasNext", equalTo(true));
        verify(placeSearchClient).search(keyword, 1);
    }
}
