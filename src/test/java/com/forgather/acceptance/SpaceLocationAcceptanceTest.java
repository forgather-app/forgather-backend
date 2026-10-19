package com.forgather.acceptance;

import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.global.auth.util.JwtTokenProvider;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@DisplayName("인수 테스트: 스페이스 장소 HTTP 계약")
@AutoConfigureMockMvc
class SpaceLocationAcceptanceTest extends AcceptanceTest {

    private static final String PLACE_URL = "https://place.map.kakao.com/00123";
    private static final String FULL_LOCATION = """
        {"name":"원래 전시", "placeName":"전시관", "placeId":"00123",
         "placeUrl":"https://place.map.kakao.com/00123"}
        """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUpLocation() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(createHost());
        token = jwtTokenProvider.generateAccessToken(host.getId());
    }

    @DisplayName("생성 요청에서 장소를 생략하거나 null로 보내면 상세 응답에 빈 문자열을 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {
        "{\"name\":\"전시\"}",
        "{\"name\":\"전시\",\"placeName\":null,\"placeId\":null,\"placeUrl\":null}"
    })
    void createsWithoutLocation(String request) throws Exception {
        // given
        // when
        JsonNode created = post(request, 201);
        JsonNode detail = get(created.path("data").path("spaceCode").asText());

        // then
        assertThat(created.path("data").size()).isEqualTo(1);
        assertLocation(detail, "", "", "");
    }

    @DisplayName("장소명만 또는 카카오 정보까지 생성하면 상세 조회에 장소 정보가 반영된다")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createsAndReadsLocation(boolean withKakao) throws Exception {
        // given
        String request = withKakao ? FULL_LOCATION : "{\"name\":\"전시\",\"placeName\":\"전시관\"}";

        // when
        String code = create(request);

        // then
        assertLocation(get(code), "전시관",
            withKakao ? "00123" : "", withKakao ? PLACE_URL : "");
    }

    @DisplayName("장소 수정 요청에서 생략과 명시적 null은 기존 값을 유지한다")
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"placeName\":null,\"placeId\":null,\"placeUrl\":null}"})
    void preservesOmittedAndNullLocation(String request) throws Exception {
        // given
        String code = create(FULL_LOCATION);

        // when
        JsonNode updated = patch(code, request, 200);

        // then
        assertLocation(updated, "전시관", "00123", PLACE_URL);
        assertLocation(get(code), "전시관", "00123", PLACE_URL);
    }

    @DisplayName("장소명만 수정하면 기존 카카오 연결을 보존한다")
    @Test
    void changesOnlyPlaceName() throws Exception {
        // given
        String code = create(FULL_LOCATION);

        // when
        JsonNode updated = patch(code, "{\"placeName\":\"새 전시관\"}", 200);

        // then
        assertLocation(updated, "새 전시관", "00123", PLACE_URL);
        assertLocation(get(code), "새 전시관", "00123", PLACE_URL);
    }

    @DisplayName("장소가 없는 스페이스에도 부분 수정으로 카카오 장소를 추가한다")
    @Test
    void addsLocationThroughPatch() throws Exception {
        // given
        String code = create("{\"name\":\"전시\"}");

        // when
        patch(code, FULL_LOCATION, 200);

        // then
        assertLocation(get(code), "전시관", "00123", PLACE_URL);
    }

    @DisplayName("카카오 두 값을 함께 비우면 장소명은 유지하고 연결만 삭제한다")
    @Test
    void removesKakaoPair() throws Exception {
        // given
        String code = create(FULL_LOCATION);

        // when
        JsonNode updated = patch(code, "{\"placeId\":\"\",\"placeUrl\":\"  \"}", 200);

        // then
        assertLocation(updated, "전시관", "", "");
        assertLocation(get(code), "전시관", "", "");
    }

    @DisplayName("장소 세 값을 공백으로 비우면 전체 장소를 삭제한다")
    @Test
    void removesEntireLocation() throws Exception {
        // given
        String code = create(FULL_LOCATION);

        // when
        JsonNode updated = patch(code,
            "{\"placeName\":\"  \",\"placeId\":\"\",\"placeUrl\":\"  \"}", 200);

        // then
        assertLocation(updated, "", "", "");
        assertLocation(get(code), "", "", "");
    }

    @DisplayName("장소명 없이 카카오 정보를 보내면 생성과 수정 모두 400으로 응답한다")
    @Test
    void rejectsInvalidLocationOnCreateAndPatch() throws Exception {
        // given
        String code = create("{\"name\":\"전시\"}");
        String request = """
            {"name":"전시", "placeId":"00123", "placeUrl":"https://place.map.kakao.com/00123"}
            """;

        // when
        post(request, 400);
        patch(code, request, 400);

        // then
        assertLocation(get(code), "", "", "");
    }

    @DisplayName("기존 장소의 일부만 삭제해 최종 상태가 잘못되면 일반 이름 수정까지 롤백한다")
    @Test
    void rollsBackGeneralNameAndLocationAfterInvalidPatch() throws Exception {
        // given
        String code = create(FULL_LOCATION);
        String request = "{\"name\":\"바뀌면 안 되는 이름\",\"placeName\":\"\"}";

        // when
        patch(code, request, 400);

        // then
        JsonNode detail = get(code);
        assertThat(detail.path("data").path("name").asText()).isEqualTo("원래 전시");
        assertLocation(detail, "전시관", "00123", PLACE_URL);
    }

    @DisplayName("장소가 있는 스페이스도 나의 목록 응답에는 장소 정보를 포함하지 않는다")
    @Test
    void keepsHostListContract() throws Exception {
        // given
        String code = create(FULL_LOCATION);

        // when
        String response = RestAssuredMockMvc.given().postProcessors(withAccessToken(token))
            .when().get("/spaces/me").then().statusCode(200).extract().asString();
        JsonNode item = objectMapper.readTree(response).path("data").path("spaces").get(0);

        // then
        assertThat(item.path("spaceCode").asText()).isEqualTo(code);
        assertThat(item.has("placeName")).isFalse();
        assertThat(item.has("placeId")).isFalse();
        assertThat(item.has("placeUrl")).isFalse();
    }

    private String create(String request) throws Exception {
        return post(request, 201).path("data").path("spaceCode").asText();
    }

    private JsonNode post(String request, int status) throws Exception {
        String response = RestAssuredMockMvc.given().postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON).body(request)
            .when().post("/spaces").then().statusCode(status).extract().asString();
        return objectMapper.readTree(response);
    }

    private JsonNode patch(String code, String request, int status) throws Exception {
        String response = RestAssuredMockMvc.given().postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON).body(request)
            .when().patch("/spaces/" + code).then().statusCode(status).extract().asString();
        return objectMapper.readTree(response);
    }

    private JsonNode get(String code) throws Exception {
        String response = RestAssuredMockMvc.given().postProcessors(withAccessToken(token))
            .when().get("/spaces/" + code).then().statusCode(200).extract().asString();
        return objectMapper.readTree(response);
    }

    private void assertLocation(JsonNode response, String name, String id, String url) {
        JsonNode data = response.path("data");
        assertThat(data.has("placeName")).isTrue();
        assertThat(data.has("placeId")).isTrue();
        assertThat(data.has("placeUrl")).isTrue();
        assertThat(data.get("placeName")).isEqualTo(objectMapper.valueToTree(name));
        assertThat(data.get("placeId")).isEqualTo(objectMapper.valueToTree(id));
        assertThat(data.get("placeUrl")).isEqualTo(objectMapper.valueToTree(url));
    }
}
