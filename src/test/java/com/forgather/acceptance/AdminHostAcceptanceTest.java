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
import com.forgather.back_office.dto.HostSpacesResponse;
import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.AdminUserFixture;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHostMap;
import com.forgather.global.auth.repository.SpaceHostMapRepository;

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
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostMapRepository spaceHostMapRepository;

    @Autowired
    private SessionManager sessionManager;

    private AdminUser adminUser;
    private String sessionId;

    @BeforeEach
    void setUp() {
        adminUser = adminUserRepository.save(AdminUserFixture.createAdminUser("어드민"));
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

    @DisplayName("호스트가 소유한 스페이스 목록을 조회한다.")
    @Test
    void getHostSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCode("1111111111"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("2222222222"));
        spaceHostMapRepository.save(new SpaceHostMap(space1, host));
        spaceHostMapRepository.save(new SpaceHostMap(space2, host));

        // when
        HostSpacesResponse result = givenWithSession()
            .when()
            .get("/admin/hosts/{hostId}/spaces", host.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(HostSpacesResponse.class);

        // then
        assertAll(
            () -> assertThat(result.hostId()).isEqualTo(host.getId()),
            () -> assertThat(result.hostName()).isEqualTo(host.getName()),
            () -> assertThat(result.spaces()).hasSize(2)
        );
    }

    @DisplayName("세션이 없으면 호스트의 스페이스 목록을 조회할 수 없다.")
    @Test
    void getHostSpacesWithoutSession() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        var result = RestAssuredMockMvc.given()
            .when()
            .get("/admin/hosts/{hostId}/spaces", host.getId())
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
