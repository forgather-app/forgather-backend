package com.forgather.acceptance;

import static com.forgather.fixture.TermFixture.createMarketingTerm;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Arrays;
import java.util.HashMap;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.repository.jpa.TermJpaRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.util.AuthCookieProvider;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ResponseCode;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.response.ExtractableResponse;
import jakarta.servlet.http.Cookie;

@DisplayName("인수 테스트: Auth")
@AutoConfigureMockMvc
class AuthAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private TermJpaRepository termJpaRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("닉네임이 없으면 내 정보의 온보딩 완료 여부가 false이다")
    @Test
    void getCurrentUserWhenNicknameIsMissing() {
        // given
        Host host = saveHost(null);
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(response.jsonPath().getBoolean("data.onboardingCompleted")).isFalse()
        );
    }

    @DisplayName("Authorization 헤더가 없으면 access token 쿠키로 내 정보를 조회한다")
    @Test
    void getCurrentUserWithAccessTokenCookie() {
        // given
        Host host = saveHost("쿠키호스트");
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .postProcessors(withCookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, token))
            .accept(ContentType.JSON)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertThat(response.jsonPath().getLong("data.id")).isEqualTo(host.getId());
    }

    @DisplayName("Authorization 헤더와 access token 쿠키가 함께 있으면 헤더로 인증한다")
    @Test
    void getCurrentUserWithAuthorizationHeaderAndCookie() {
        // given
        Host headerHost = saveHost("헤더호스트");
        Host cookieHost = saveHost("쿠키호스트");
        String headerToken = jwtTokenProvider.generateAccessToken(headerHost.getId());
        String cookieToken = jwtTokenProvider.generateAccessToken(cookieHost.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + headerToken)
            .postProcessors(withCookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, cookieToken))
            .accept(ContentType.JSON)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertThat(response.jsonPath().getLong("data.id")).isEqualTo(headerHost.getId());
    }

    @DisplayName("Authorization 헤더 형식이 잘못되면 정상 쿠키가 있어도 인증에 실패한다")
    @Test
    void failAuthenticationWithInvalidAuthorizationHeaderAndValidCookie() {
        // given
        Host host = saveHost("쿠키호스트");
        String cookieToken = jwtTokenProvider.generateAccessToken(host.getId());

        // when & then
        RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Invalid token")
            .postProcessors(withCookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, cookieToken))
            .when()
            .get("/auth/me")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo(ResponseCode.JWT_INVALID.name()));
    }

    @DisplayName("refresh 요청 바디가 없으면 refresh token 쿠키로 토큰을 재발급한다")
    @Test
    void refreshWithRefreshTokenCookie() {
        // given
        Host host = saveHost("쿠키호스트");
        String refreshToken = jwtTokenProvider.generateRefreshToken(host.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .postProcessors(withCookie(AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME, refreshToken))
            .when()
            .post("/auth/refresh")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        List<String> setCookieHeaders = response.headers().getValues(HttpHeaders.SET_COOKIE);
        assertAll(
            () -> assertThat(response.jsonPath().getString("data.refreshToken")).isEqualTo(refreshToken),
            () -> assertThat(setCookieHeaders).hasSize(2),
            () -> assertThat(setCookieHeaders).anySatisfy(
                cookie -> assertThat(cookie).startsWith(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME + "=")
            ),
            () -> assertThat(setCookieHeaders).anySatisfy(
                cookie -> assertThat(cookie).startsWith(AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME + "=")
            )
        );
    }

    @DisplayName("필수 약관 타입별 동의 이력이 있으면 내 정보의 온보딩 완료 여부가 true이다")
    @Test
    void getCurrentUserWhenRequiredTermTypesAreAgreed() {
        // given
        Host host = saveHost("포개더");
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term oldServiceTerm = termJpaRepository.save(createServiceTerm("0.9.0", "old service"));
        termJpaRepository.save(createServiceTerm("1.0.0", "latest service"));
        Term privacyTerm = termJpaRepository.save(createPrivacyTerm("1.0.0", "privacy"));
        insertAgreeHistory(host.getId(), oldServiceTerm.getId());
        insertAgreeHistory(host.getId(), privacyTerm.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertThat(response.jsonPath().getBoolean("data.onboardingCompleted")).isTrue();
    }

    @DisplayName("필수 약관 타입별 동의 이력이 없으면 내 정보의 온보딩 완료 여부가 false이다")
    @Test
    void getCurrentUserWhenRequiredTermTypeIsMissing() {
        // given
        Host host = saveHost("포개더");
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        termJpaRepository.save(createPrivacyTerm("1.0.0", "privacy"));
        insertAgreeHistory(host.getId(), serviceTerm.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertThat(response.jsonPath().getBoolean("data.onboardingCompleted")).isFalse();
    }

    @DisplayName("온보딩을 제출하면 닉네임과 약관 동의 이력을 원자적으로 저장한다")
    @Test
    void submitOnboarding() {
        // given
        Host host = saveHost(null);
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        Term privacyTerm = termJpaRepository.save(createPrivacyTerm("1.0.0", "privacy"));
        Term marketingTerm = termJpaRepository.save(createMarketingTerm("1.0.0", "marketing"));

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of(
                "nickname", "포개더",
                "agreedTermIds", List.of(serviceTerm.getId(), privacyTerm.getId(), marketingTerm.getId())
            ))
            .when()
            .post("/auth/onboarding")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        Host savedHost = hostRepository.getByIdOrThrow(host.getId());

        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(response.jsonPath().getString("data.name")).isEqualTo("포개더"),
            () -> assertThat(response.jsonPath().getBoolean("data.onboardingCompleted")).isTrue(),
            () -> assertThat(savedHost.getName()).isEqualTo("카카오원본이름"),
            () -> assertThat(savedHost.getNickname()).isEqualTo("포개더"),
            () -> assertThat(countAgreeHistories(host.getId())).isEqualTo(3)
        );
    }

    @DisplayName("존재하지 않는 약관 ID가 있으면 온보딩 제출에 실패하고 닉네임을 저장하지 않는다")
    @Test
    void rejectOnboardingWhenTermDoesNotExist() {
        // given
        Host host = saveHost(null);
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "nickname", "포개더",
                "agreedTermIds", List.of(serviceTerm.getId(), 999_999L)
            ))
            .when()
            .post("/auth/onboarding")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());

        // then
        Host savedHost = hostRepository.getByIdOrThrow(host.getId());

        assertAll(
            () -> assertThat(savedHost.getNickname()).isNull(),
            () -> assertThat(countAgreeHistories(host.getId())).isZero()
        );
    }

    @DisplayName("필수 약관 타입이 누락되면 온보딩 제출에 실패하고 닉네임을 저장하지 않는다")
    @Test
    void rejectOnboardingWhenRequiredTermTypeIsMissing() {
        // given
        Host host = saveHost(null);
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        termJpaRepository.save(createPrivacyTerm("1.0.0", "privacy"));

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(Map.of(
                "nickname", "포개더",
                "agreedTermIds", List.of(serviceTerm.getId())
            ))
            .when()
            .post("/auth/onboarding")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());

        // then
        Host savedHost = hostRepository.getByIdOrThrow(host.getId());

        assertAll(
            () -> assertThat(savedHost.getNickname()).isNull(),
            () -> assertThat(countAgreeHistories(host.getId())).isZero()
        );
    }

    @DisplayName("닉네임이 유효하지 않으면 온보딩 제출에 실패한다")
    @Test
    void rejectOnboardingWhenNicknameIsInvalid() {
        // given
        Host host = saveHost(null);
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term serviceTerm = termJpaRepository.save(createServiceTerm("1.0.0", "service"));
        Term privacyTerm = termJpaRepository.save(createPrivacyTerm("1.0.0", "privacy"));
        List<Object> invalidNicknames = Arrays.asList(null, "   ", "123456789012345678901");

        // when
        for (Object invalidNickname : invalidNicknames) {
            Map<String, Object> request = new HashMap<>();
            request.put("nickname", invalidNickname);
            request.put("agreedTermIds", List.of(serviceTerm.getId(), privacyTerm.getId()));

            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/auth/onboarding")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
        }

        // then
        Host savedHost = hostRepository.getByIdOrThrow(host.getId());

        assertAll(
            () -> assertThat(savedHost.getNickname()).isNull(),
            () -> assertThat(countAgreeHistories(host.getId())).isZero()
        );
    }

    private Host saveHost(String nickname) {
        Host host = new Host("카카오원본이름", "postie@forgather.app");
        if (nickname != null) {
            host.updateNickname(nickname);
        }
        return hostRepository.save(host);
    }

    private RequestPostProcessor withCookie(String name, String value) {
        return request -> {
            request.setCookies(new Cookie(name, value));
            return request;
        };
    }

    private void insertAgreeHistory(Long hostId, Long termId) {
        jdbcTemplate.update(
            "INSERT INTO host_term_history (host_id, term_id, action, created_at, updated_at) VALUES (?, ?, 'AGREE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            hostId,
            termId
        );
    }

    private int countAgreeHistories(Long hostId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM host_term_history WHERE host_id = ? AND action = 'AGREE' AND deleted_at IS NULL",
            Integer.class,
            hostId
        );
        return count == null ? 0 : count;
    }
}
