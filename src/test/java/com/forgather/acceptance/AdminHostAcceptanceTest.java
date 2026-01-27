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
import com.forgather.back_office.dto.AdminHostResponse;
import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.fixture.AdminUserFixture;
import com.forgather.fixture.HostFixture;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import jakarta.servlet.http.Cookie;

@AutoConfigureMockMvc
class AdminHostAcceptanceTest extends AcceptanceTest {

    private static final String SESSION_COOKIE_NAME = "ADMIN_SESSION_ID";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SessionManager sessionManager;

    private AdminUser adminUser;
    private String sessionId;

    @BeforeEach
    void setUp() {
        adminUser = adminUserRepository.save(AdminUserFixture.createAdminUser("어드민", "패스워드"));
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

    @DisplayName("모든 호스트 정보를 조회한다.")
    @Test
    void getAllHosts() {
        // given
        createHost(16);

        // when
        AdminHostResponse result = givenWithSession()
            .when()
            .get("/admin/hosts")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminHostResponse.class);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(15),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(15),
            () -> assertThat(result.totalCount()).isEqualTo(16),
            () -> assertThat(result.totalPages()).isEqualTo(2)
        );
    }

    @DisplayName("세션이 없으면 모든 호스트 정보를 조회할 수 없다.")
    @Test
    void getAllHostsWithoutSession() {
        // when
        var result = RestAssuredMockMvc.given()
            .when()
            .get("/admin/hosts")
            .then()
            .extract();

        // then
        assertThat(result.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private void createHost(int count) {
        for (int i = 0; i < count; i++) {
            hostRepository.save(HostFixture.createHost());
        }
    }
}
