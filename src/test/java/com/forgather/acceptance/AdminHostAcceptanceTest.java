package com.forgather.acceptance;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;
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
import com.forgather.domain.space.repository.AppUserRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.AdminUserFixture;
import com.forgather.fixture.AppUserFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import jakarta.servlet.http.Cookie;

@AutoConfigureMockMvc
class AdminHostAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private AppUserRepository hostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

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
        createAppUser(16);

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
        AppUser host = hostRepository.save(AppUserFixture.createAppUser());
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCode("1111111111"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("2222222222"));
        spaceHostRepository.save(new SpaceHost(space1, host));
        spaceHostRepository.save(new SpaceHost(space2, host));

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
        AppUser host = hostRepository.save(AppUserFixture.createAppUser());

        // when
        var result = RestAssuredMockMvc.given()
            .when()
            .get("/admin/hosts/{hostId}/spaces", host.getId())
            .then()
            .extract();

        // then
        assertThat(result.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("호스트 이름으로 검색한다. (완전 일치)")
    @Test
    void searchHostsByNameExactMatch() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("레오"));

        // when
        AdminHostResponse result = givenWithSession()
            .queryParam("name", "포스티")
            .when()
            .get("/admin/hosts/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminHostResponse.class);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(1),
            () -> assertThat(result.hosts().getFirst().name()).isEqualTo("포스티"),
            () -> assertThat(result.totalCount()).isEqualTo(1)
        );
    }

    @DisplayName("호스트 이름으로 검색한다. (부분 일치)")
    @Test
    void searchHostsByNameContaining() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("포스트맨"));
        hostRepository.save(AppUserFixture.createAppUserWithName("레오"));

        // when
        AdminHostResponse result = givenWithSession()
            .queryParam("name", "포스")
            .when()
            .get("/admin/hosts/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminHostResponse.class);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(2),
            () -> assertThat(result.hosts()).allMatch(host -> host.name().contains("포스")),
            () -> assertThat(result.totalCount()).isEqualTo(2)
        );
    }

    @DisplayName("호스트 이름 검색 결과가 없으면 빈 목록을 반환한다.")
    @Test
    void searchHostsByNameNoResult() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("레오"));

        // when
        AdminHostResponse result = givenWithSession()
            .queryParam("name", "존재하지않는이름")
            .when()
            .get("/admin/hosts/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminHostResponse.class);

        // then
        assertThat(result.hosts()).isEmpty();
    }

    @DisplayName("호스트 이름 검색 시 검색어가 없으면 모든 호스트를 반환한다.")
    @Test
    void searchHostsByNameWithEmptyKeyword() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("클로버"));
        hostRepository.save(AppUserFixture.createAppUserWithName("레오"));

        // when
        AdminHostResponse result = givenWithSession()
            .queryParam("name", "")
            .when()
            .get("/admin/hosts/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminHostResponse.class);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(3),
            () -> assertThat(result.totalCount()).isEqualTo(3)
        );
    }

    @DisplayName("호스트 이름 검색 시 검색어 파라미터가 없으면 모든 호스트를 반환한다.")
    @Test
    void searchHostsByNameWithoutParameter() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("레오"));

        // when
        AdminHostResponse result = givenWithSession()
            .when()
            .get("/admin/hosts/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminHostResponse.class);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(2),
            () -> assertThat(result.totalCount()).isEqualTo(2)
        );
    }

    @DisplayName("호스트 이름 검색 결과는 페이지네이션이 적용된다.")
    @Test
    void searchHostsByNameWithPagination() {
        // given
        for (int i = 0; i < 20; i++) {
            hostRepository.save(AppUserFixture.createAppUserWithName("포스티 " + i));
        }

        // when
        AdminHostResponse result = givenWithSession()
            .queryParam("name", "포스티")
            .when()
            .get("/admin/hosts/search/by-name")
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
            () -> assertThat(result.totalCount()).isEqualTo(20),
            () -> assertThat(result.totalPages()).isEqualTo(2)
        );
    }

    @DisplayName("세션이 없으면 호스트 이름으로 검색할 수 없다.")
    @Test
    void searchHostsByNameWithoutSession() {
        // when
        var result = RestAssuredMockMvc.given()
            .queryParam("name", "포스티")
            .when()
            .get("/admin/hosts/search/by-name")
            .then()
            .extract();

        // then
        assertThat(result.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private void createAppUser(int count) {
        for (int i = 0; i < count; i++) {
            hostRepository.save(AppUserFixture.createAppUser());
        }
    }
}
