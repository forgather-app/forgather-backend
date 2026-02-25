package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.dto.SecuritySummaryResponse;
import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.fixture.AdminUserFixture;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import jakarta.servlet.http.Cookie;

@AutoConfigureMockMvc
class AdminSecurityAcceptanceTest extends AcceptanceTest {

    private static final String SESSION_COOKIE_NAME = "ADMIN_SESSION_ID";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

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

    @DisplayName("보안 요약 정보를 조회한다.")
    @Test
    void getSummary() {
        // when
        SecuritySummaryResponse result = givenWithSession()
            .when()
            .get("/admin/security/summary")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(SecuritySummaryResponse.class);

        // then
        assertAll(
            () -> assertThat(result.todayBlocked()).isNotNegative(),
            () -> assertThat(result.attackTypes()).isNotNull(),
            () -> assertThat(result.topAttackIps()).isNotNull(),
            () -> assertThat(result.grafanaDashboardUrl()).isNotNull()
        );
    }

    @DisplayName("세션이 없으면 보안 요약 정보를 조회할 수 없다.")
    @Test
    void getSummaryWithoutSession() {
        // when
        var result = RestAssuredMockMvc.given()
            .when()
            .get("/admin/security/summary")
            .then()
            .extract();

        // then
        assertThat(result.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}
