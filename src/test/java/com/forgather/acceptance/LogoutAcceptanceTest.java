package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.space.repository.HostRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ResponseCode;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.response.ExtractableResponse;

@DisplayName("인수 테스트: 로그아웃")
@AutoConfigureMockMvc
class LogoutAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("로그아웃하면 인증 쿠키를 만료시킨다")
    @Test
    void logoutExpiresAuthCookies() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(host.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken), withRefreshToken(refreshToken))
            .accept(ContentType.JSON)
            .when()
            .post("/auth/logout")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        List<String> setCookies = response.headers().getValues(HttpHeaders.SET_COOKIE);
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(setCookies).anyMatch(cookie ->
                cookie.startsWith(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME + "=") && cookie.contains("Max-Age=0")),
            () -> assertThat(setCookies).anyMatch(cookie ->
                cookie.startsWith(AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME + "=") && cookie.contains("Max-Age=0"))
        );
    }

    @DisplayName("인증 쿠키가 없어도 로그아웃은 성공한다")
    @Test
    void logoutSucceedsWithoutAuthCookies() {
        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .post("/auth/logout")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        List<String> setCookies = response.headers().getValues(HttpHeaders.SET_COOKIE);
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(setCookies).anyMatch(cookie ->
                cookie.startsWith(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME + "=") && cookie.contains("Max-Age=0")),
            () -> assertThat(setCookies).anyMatch(cookie ->
                cookie.startsWith(AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME + "=") && cookie.contains("Max-Age=0"))
        );
    }

}
