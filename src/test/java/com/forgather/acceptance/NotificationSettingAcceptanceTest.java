package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.util.JwtTokenProvider;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;

@DisplayName("인수 테스트: 알림 설정")
@AutoConfigureMockMvc
class NotificationSettingAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String accessToken;

    @BeforeEach
    void setUpMockMvc() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        accessToken = jwtTokenProvider.generateAccessToken(host.getId());
    }

    private Map<String, Object> updateRequest(Object enabled) {
        Map<String, Object> body = new HashMap<>();
        body.put("enabled", enabled);
        return body;
    }

    private boolean getEnabled() {
        return RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .when()
            .get("/notifications/settings")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath()
            .getBoolean("data.enabled");
    }

    private ValidatableMockMvcResponse update(Map<String, Object> body) {
        return RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .patch("/notifications/settings")
            .then();
    }

    private long countSettings() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification_setting", Long.class);
        return count == null ? 0 : count;
    }

    @DisplayName("설정을 변경한 적이 없으면 알림 설정 조회에 true를 응답하고 설정 행을 만들지 않는다")
    @Test
    void getDefaultSetting() {
        // when
        boolean enabled = getEnabled();

        // then
        assertAll(
            () -> assertThat(enabled).isTrue(),
            () -> assertThat(countSettings()).isZero()
        );
    }

    @DisplayName("알림 설정을 끄면 200과 함께 변경한 값을 응답하고 이후 조회에도 반영된다")
    @Test
    void disableSetting() {
        // when
        update(updateRequest(false))
            .statusCode(HttpStatus.OK.value())
            .body("code", equalTo("SUCCESS"))
            .body("data.enabled", equalTo(false));

        // then
        assertThat(getEnabled()).isFalse();
    }

    @DisplayName("끈 알림 설정을 다시 켜면 이후 조회에 true가 반영되고 설정 행은 하나로 유지된다")
    @Test
    void enableSettingAgain() {
        // given
        update(updateRequest(false)).statusCode(HttpStatus.OK.value());

        // when
        update(updateRequest(true))
            .statusCode(HttpStatus.OK.value())
            .body("data.enabled", equalTo(true));

        // then
        assertAll(
            () -> assertThat(getEnabled()).isTrue(),
            () -> assertThat(countSettings()).isEqualTo(1)
        );
    }

    @DisplayName("알림 수신 여부가 없으면 400 VALIDATION_FAILED로 응답한다")
    @Test
    void rejectMissingEnabled() {
        // when & then
        update(updateRequest(null))
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("알림 수신 여부가 불리언으로 읽히지 않는 값이면 400 BAD_REQUEST로 응답한다")
    @Test
    void rejectNonBooleanEnabled() {
        // when & then
        update(updateRequest("yes"))
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("BAD_REQUEST"));
    }

    @DisplayName("로그인하지 않고 알림 설정을 조회하면 401 UNAUTHORIZED로 응답한다")
    @Test
    void getSettingWithoutLogin() {
        // when & then
        RestAssuredMockMvc.given()
            .when()
            .get("/notifications/settings")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("로그인하지 않고 알림 설정을 변경하면 401 UNAUTHORIZED로 응답한다")
    @Test
    void updateSettingWithoutLogin() {
        // when & then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .body(updateRequest(false))
            .when()
            .patch("/notifications/settings")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }
}
