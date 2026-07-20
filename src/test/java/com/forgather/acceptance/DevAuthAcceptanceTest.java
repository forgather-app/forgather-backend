package com.forgather.acceptance;

import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.term.repository.jpa.TermJpaRepository;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.response.ResponseCode;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import io.restassured.response.ExtractableResponse;

@DisplayName("인수 테스트: 개발용 임시 로그인")
@AutoConfigureMockMvc
class DevAuthAcceptanceTest extends AcceptanceTest {

    private static final String LOGIN_ID = "testId";
    private static final String PASSWORD = "testPw";
    private static final String NICKNAME = "테스트";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermJpaRepository termJpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        // 온보딩 자동 완료에 필수 약관이 필요한데, cleanup.sql이 매 테스트 후 term을 truncate한다.
        termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        termJpaRepository.save(createPrivacyTerm("1.0.0", "privacy"));
    }

    @DisplayName("고정 아이디와 비밀번호로 로그인하면 카카오 로그인과 동일하게 토큰을 바디와 쿠키로 함께 반환한다")
    @Test
    void loginWithFixedCredentials() {
        // given
        Map<String, String> request = Map.of("loginId", LOGIN_ID, "password", PASSWORD);

        // when
        ExtractableResponse<MockMvcResponse> response = login(request)
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        List<String> setCookieHeaders = response.headers().getValues(HttpHeaders.SET_COOKIE);
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(response.jsonPath().getString("data.accessToken")).isNotBlank(),
            () -> assertThat(response.jsonPath().getString("data.refreshToken")).isNotBlank(),
            () -> assertThat(setCookieHeaders).hasSize(2),
            () -> assertThat(setCookieHeaders).anySatisfy(
                cookie -> assertThat(cookie).startsWith(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME + "=")
            ),
            () -> assertThat(setCookieHeaders).anySatisfy(
                cookie -> assertThat(cookie).startsWith(AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME + "=")
            )
        );
    }

    @DisplayName("임시 로그인으로 발급받은 토큰으로 내 정보를 조회하면 온보딩이 완료되어 있다")
    @Test
    void getCurrentUserAfterDevLogin() {
        // given
        String accessToken = login(Map.of("loginId", LOGIN_ID, "password", PASSWORD))
            .extract()
            .jsonPath()
            .getString("data.accessToken");

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .accept(ContentType.JSON)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getBoolean("data.onboardingCompleted")).isTrue(),
            () -> assertThat(response.jsonPath().getString("data.name")).isEqualTo(NICKNAME)
        );
    }

    @DisplayName("비밀번호가 일치하지 않으면 로그인할 수 없다")
    @Test
    void loginWithWrongPassword() {
        // given
        Map<String, String> request = Map.of("loginId", LOGIN_ID, "password", "wrong-password");

        // when & then
        login(request).statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("아이디가 일치하지 않으면 로그인할 수 없다")
    @Test
    void loginWithWrongLoginId() {
        // given
        Map<String, String> request = Map.of("loginId", "wrong-login-id", "password", PASSWORD);

        // when & then
        login(request).statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("두 번 로그인해도 같은 호스트를 재사용하고 약관 동의 이력을 중복 저장하지 않는다")
    @Test
    void loginTwiceWithSameCredentials() {
        // given
        Map<String, String> request = Map.of("loginId", LOGIN_ID, "password", PASSWORD);

        // when
        login(request).statusCode(HttpStatus.OK.value());
        login(request).statusCode(HttpStatus.OK.value());

        // then
        assertAll(
            () -> assertThat(countKakaoHostsByUserId(LOGIN_ID)).isEqualTo(1),
            () -> assertThat(countAgreeHistories()).isEqualTo(2)
        );
    }

    private ValidatableMockMvcResponse login(Map<String, String> request) {
        return RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/auth/login/dev")
            .then();
    }

    private int countKakaoHostsByUserId(String userId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM host_kakao WHERE user_id = ?",
            Integer.class,
            userId
        );
        return count == null ? 0 : count;
    }

    private int countAgreeHistories() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM host_term_history WHERE action = 'AGREE' AND deleted_at IS NULL",
            Integer.class
        );
        return count == null ? 0 : count;
    }
}
