package com.forgather.acceptance;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.dto.SignupTrendResponse;
import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.model.TrendPoint;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.fixture.AdminUserFixture;
import com.forgather.fixture.HostFixture;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import jakarta.servlet.http.Cookie;

@AutoConfigureMockMvc
class AdminStatsAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SessionManager sessionManager;

    private String sessionId;

    @BeforeEach
    void setUp() {
        AdminUser adminUser = adminUserRepository.save(AdminUserFixture.createAdminUser("어드민"));
        AdminSession session = sessionManager.createSession(adminUser.getId(), adminUser.getUsername());
        sessionId = session.getSessionId().getValue();

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    private MockMvcRequestSpecification givenWithSession() {
        return RestAssuredMockMvc.given()
            .postProcessors(request -> {
                request.setCookies(new Cookie(SESSION_COOKIE_NAME, sessionId));
                return request;
            });
    }

    @DisplayName("unit 없이 요청하면 일 단위 가입 추이를 조회한다.")
    @Test
    void getSignupTrendDefaultDay() {
        // given
        hostRepository.save(HostFixture.createHost());

        // when
        SignupTrendResponse result = givenWithSession()
            .when()
            .get("/admin/stats/signups")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(SignupTrendResponse.class);

        // then
        assertAll(
            () -> assertThat(result.unit()).isEqualTo("DAY"),
            () -> assertThat(result.points()).hasSize(30),
            () -> assertThat(result.totalCount()).isEqualTo(1)
        );
    }

    @DisplayName("월 단위 가입 추이를 조회한다.")
    @Test
    void getSignupTrendMonth() {
        SignupTrendResponse result = givenWithSession()
            .queryParam("unit", "MONTH")
            .when()
            .get("/admin/stats/signups")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(SignupTrendResponse.class);

        assertThat(result.points()).hasSize(12);
    }

    @DisplayName("월 단위로 기간을 지정해 가입 추이를 조회한다.")
    @Test
    void getSignupTrendMonthRange() {
        SignupTrendResponse result = givenWithSession()
            .queryParam("unit", "MONTH")
            .queryParam("from", "2026-01")
            .queryParam("to", "2026-03")
            .when()
            .get("/admin/stats/signups")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(SignupTrendResponse.class);

        assertThat(result.points()).extracting(TrendPoint::periodStart).containsExactly(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 3, 1)
        );
    }

    @DisplayName("시작 월이 2025년 7월 이전이면 400을 반환한다.")
    @Test
    void monthRangeBeforeMinMonth() {
        givenWithSession()
            .queryParam("unit", "MONTH")
            .queryParam("from", "2025-06")
            .queryParam("to", "2026-01")
            .when()
            .get("/admin/stats/signups")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("월 형식이 올바르지 않으면 400을 반환한다.")
    @Test
    void invalidMonthFormat() {
        givenWithSession()
            .queryParam("unit", "MONTH")
            .queryParam("from", "2026-1-01")
            .queryParam("to", "2026-03")
            .when()
            .get("/admin/stats/signups")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("지원하지 않는 unit이면 400을 반환한다.")
    @Test
    void invalidUnit() {
        givenWithSession()
            .queryParam("unit", "YEAR")
            .when()
            .get("/admin/stats/signups")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("세션 없이 요청하면 401을 반환한다.")
    @Test
    void unauthorized() {
        RestAssuredMockMvc.given()
            .when()
            .get("/admin/stats/signups")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
