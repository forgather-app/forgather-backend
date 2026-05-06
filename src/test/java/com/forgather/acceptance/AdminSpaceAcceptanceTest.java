package com.forgather.acceptance;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.dto.AdminSpaceResponse;
import com.forgather.back_office.dto.SpaceDetailResponse;
import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.AdminUserFixture;
import com.forgather.fixture.GuestBookCardFixture;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.ProductFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.util.RandomCodeGenerator;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import jakarta.servlet.http.Cookie;

@AutoConfigureMockMvc
class AdminSpaceAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private RandomCodeGenerator randomCodeGenerator;

    private AdminUser adminUser;
    private String sessionId;
    private Host host;

    @BeforeEach
    void setUp() {
        host = hostRepository.save(HostFixture.createHost());

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

    @DisplayName("모든 스페이스를 조회한다.")
    @Test
    void getAllSpaces() {
        // given
        createSpaces(16);

        // when
        AdminSpaceResponse result = givenWithSession()
            .when()
            .get("/admin/spaces")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(15),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(15),
            () -> assertThat(result.totalCount()).isEqualTo(16),
            () -> assertThat(result.totalPages()).isEqualTo(2)
        );
    }

    @DisplayName("세션이 없으면 모든 스페이스를 조회할 수 없다.")
    @Test
    void getAllSpacesWithoutSession() {
        // given
        createSpaces(16);

        // when & then
        RestAssuredMockMvc.given()
            .when()
            .get("/admin/spaces")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("스페이스를 상세 조회한다.")
    @Test
    void getSpaceDetail() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode("1234567890"));
        spaceHostRepository.save(new SpaceHost(space, host));
        productRepository.save(ProductFixture.createProductWithSpace(space));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpaceAndNickname(space, "1"));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpaceAndNickname(space, "2"));

        // when
        SpaceDetailResponse result = givenWithSession()
            .when()
            .get("/admin/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(SpaceDetailResponse.class);

        // then
        assertAll(
            () -> assertThat(result.space().code()).isEqualTo(space.getCode()),
            () -> assertThat(result.productCount()).isOne(),
            () -> assertThat(result.guestBookCount()).isEqualTo(2)
        );
    }

    @DisplayName("작품 소개를 등록하지 않은 스페이스를 상세 조회한다.")
    @Test
    void getSpaceDetailWithoutProduct() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode("1234567890"));
        spaceHostRepository.save(new SpaceHost(space, host));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpaceAndNickname(space, "1"));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpaceAndNickname(space, "2"));

        // when
        SpaceDetailResponse result = givenWithSession()
            .when()
            .get("/admin/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(SpaceDetailResponse.class);

        // then
        assertAll(
            () -> assertThat(result.space().code()).isEqualTo(space.getCode()),
            () -> assertThat(result.productCount()).isZero(),
            () -> assertThat(result.guestBookCount()).isEqualTo(2)
        );
    }

    @DisplayName("작품 소개와 방명록이 없는 스페이스를 상세 조회한다.")
    @Test
    void getSpaceDetailWithoutProductAndGuestBookCard() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode("1234567890"));
        spaceHostRepository.save(new SpaceHost(space, host));

        // when
        SpaceDetailResponse result = givenWithSession()
            .when()
            .get("/admin/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(SpaceDetailResponse.class);

        // then
        assertAll(
            () -> assertThat(result.space().code()).isEqualTo(space.getCode()),
            () -> assertThat(result.productCount()).isZero(),
            () -> assertThat(result.guestBookCount()).isZero()
        );
    }

    @DisplayName("세션이 없으면 스페이스를 상세 조회할 수 없다.")
    @Test
    void getSpaceDetailWithoutSession() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode("1234567890"));
        spaceHostRepository.save(new SpaceHost(space, host));

        // when & then
        RestAssuredMockMvc.given()
            .when()
            .get("/admin/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("작품 소개가 등록된 모든 스페이스를 조회한다.")
    @Test
    void getSpacesHasProduct() {
        // given
        createSpacesWithProduct(16);
        createSpaces(4);

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("hasProduct", true)
            .when()
            .get("/admin/spaces/search")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(15),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(15),
            () -> assertThat(result.totalCount()).isEqualTo(16),
            () -> assertThat(result.totalPages()).isEqualTo(2)
        );
    }

    @DisplayName("작품 소개가 등록되지 않은 모든 스페이스를 조회한다.")
    @Test
    void getSpacesHasNoProduct() {
        // given
        createSpaces(16);
        createSpacesWithProduct(4);

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("hasProduct", false)
            .when()
            .get("/admin/spaces/search")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(15),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(15),
            () -> assertThat(result.totalCount()).isEqualTo(16),
            () -> assertThat(result.totalPages()).isEqualTo(2)
        );
    }

    @DisplayName("세션이 없으면 필터링된 스페이스 목록을 조회할 수 없다.")
    @Test
    void getSpacesByFilterWithoutSession() {
        // when & then
        RestAssuredMockMvc.given()
            .queryParam("hasProduct", true)
            .when()
            .get("/admin/spaces/search")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("스페이스 이름으로 검색한다. (완전 일치)")
    @Test
    void searchSpacesByNameExactMatch() {
        // given
        createSpaceWithName("나의 졸업전시");
        createSpaceWithName("친구의 졸업전시");
        createSpaceWithName("우리의 작품전시");

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "나의 졸업전시")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(1),
            () -> assertThat(result.spaces().getFirst().name()).isEqualTo("나의 졸업전시"),
            () -> assertThat(result.totalCount()).isEqualTo(1)
        );
    }

    @DisplayName("스페이스 이름으로 검색한다. (앞부분 일치)")
    @Test
    void searchSpacesByNamePrefixMatch() {
        // given
        createSpaceWithName("졸업전시 2024");
        createSpaceWithName("졸업전시 2025");
        createSpaceWithName("작품전시 2024");

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "졸업전시")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(2),
            () -> assertThat(result.spaces()).allMatch(space -> space.name()
                .contains("졸업전시")),
            () -> assertThat(result.totalCount()).isEqualTo(2)
        );
    }

    @DisplayName("스페이스 이름으로 검색한다. (뒷부분 일치)")
    @Test
    void searchSpacesByNameSuffixMatch() {
        // given
        createSpaceWithName("나의 졸업전시");
        createSpaceWithName("친구의 졸업전시");
        createSpaceWithName("나의 작품전시");

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "졸업전시")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(2),
            () -> assertThat(result.spaces()).allMatch(space -> space.name()
                .contains("졸업전시")),
            () -> assertThat(result.totalCount()).isEqualTo(2)
        );
    }

    @DisplayName("스페이스 이름으로 검색한다. (중간 부분 일치)")
    @Test
    void searchSpacesByNameMiddleMatch() {
        // given
        createSpaceWithName("2024 졸업전시 작품");
        createSpaceWithName("2025 졸업전시 모음");
        createSpaceWithName("2024 작품전시 모음");

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "졸업전시")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(2),
            () -> assertThat(result.spaces()).allMatch(space -> space.name().contains("졸업전시")),
            () -> assertThat(result.totalCount()).isEqualTo(2)
        );
    }

    @DisplayName("스페이스 이름 검색 결과가 없으면 빈 목록을 반환한다.")
    @Test
    void searchSpacesByNameNoResult() {
        // given
        createSpaceWithName("나의 졸업전시");
        createSpaceWithName("친구의 졸업전시");

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "존재하지않는이름")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertThat(result.spaces()).isEmpty();
    }

    @DisplayName("스페이스 이름 검색 시 검색어가 없으면 모든 스페이스를 반환한다.")
    @Test
    void searchSpacesByNameWithEmptyKeyword() {
        // given
        createSpaceWithName("나의 졸업전시");
        createSpaceWithName("친구의 졸업전시");
        createSpaceWithName("우리의 작품전시");

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(3),
            () -> assertThat(result.totalCount()).isEqualTo(3)
        );
    }

    @DisplayName("스페이스 이름 검색 시 검색어 파라미터가 없으면 모든 스페이스를 반환한다.")
    @Test
    void searchSpacesByNameWithoutParameter() {
        // given
        createSpaceWithName("나의 졸업전시");
        createSpaceWithName("친구의 졸업전시");

        // when
        AdminSpaceResponse result = givenWithSession()
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(2),
            () -> assertThat(result.totalCount()).isEqualTo(2)
        );
    }

    @DisplayName("이모지가 포함된 스페이스 이름으로 검색한다.")
    @Test
    void searchSpacesByNameWithEmoji() {
        // given
        createSpaceWithName("나의 졸업전시 🎨");
        createSpaceWithName("친구의 졸업전시 🖼️");
        createSpaceWithName("우리의 작품전시");

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "🎨")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(1),
            () -> assertThat(result.spaces().getFirst().name()).contains("🎨"),
            () -> assertThat(result.totalCount()).isEqualTo(1)
        );
    }

    @DisplayName("특수문자가 포함된 스페이스 이름으로 검색한다.")
    @Test
    void searchSpacesByNameWithSpecialCharacters() {
        // given
        createSpaceWithName("나의 전시 [2024]");
        createSpaceWithName("친구의 전시 (2024)");
        createSpaceWithName("우리의 전시");

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "[2024]")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(1),
            () -> assertThat(result.spaces().getFirst().name()).contains("[2024]"),
            () -> assertThat(result.totalCount()).isEqualTo(1)
        );
    }

    @DisplayName("스페이스 이름 검색 결과는 페이지네이션이 적용된다.")
    @Test
    void searchSpacesByNameWithPagination() {
        // given
        for (int i = 0; i < 20; i++) {
            createSpaceWithName("졸업전시 " + i);
        }

        // when
        AdminSpaceResponse result = givenWithSession()
            .queryParam("name", "졸업전시")
            .when()
            .get("/admin/spaces/search/by-name")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminSpaceResponse.class);

        // then
        assertAll(
            () -> assertThat(result.spaces()).hasSize(15),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(15),
            () -> assertThat(result.totalCount()).isEqualTo(20),
            () -> assertThat(result.totalPages()).isEqualTo(2)
        );
    }

    private void createSpaces(int count) {
        for (int i = 0; i < count; i++) {
            String spaceCode = randomCodeGenerator.generate(10);
            Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode(spaceCode));
            spaceHostRepository.save(new SpaceHost(space, host));
        }
    }

    private void createSpacesWithProduct(int count) {
        for (int i = 0; i < count; i++) {
            String spaceCode = randomCodeGenerator.generate(10);
            Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode(spaceCode));
            spaceHostRepository.save(new SpaceHost(space, host));
            productRepository.save(ProductFixture.createProductWithSpace(space));
        }
    }

    private Space createSpaceWithName(String name) {
        String spaceCode = randomCodeGenerator.generate(10);
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName(spaceCode, name));
        spaceHostRepository.save(new SpaceHost(space, host));
        return space;
    }
}
