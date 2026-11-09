package com.forgather.acceptance;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import com.forgather.back_office.dto.AdminProductDetailResponse;
import com.forgather.back_office.dto.AdminProductPhotoResponse;
import com.forgather.back_office.dto.AdminProductResponse;
import com.forgather.back_office.dto.AdminProductSummaryResponse;
import com.forgather.back_office.model.AdminSession;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.AdminUserFixture;
import com.forgather.fixture.ProductFixture;
import com.forgather.fixture.ProductPhotoFixture;
import com.forgather.fixture.SpaceFixture;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import jakarta.servlet.http.Cookie;

@AutoConfigureMockMvc
class AdminProductAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

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

    @DisplayName("모든 작품을 기본 페이지 크기로 조회한다.")
    @Test
    void getAllProducts() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        createProducts(space, 16);

        // when
        AdminProductResponse result = givenWithSession()
            .when()
            .get("/admin/products")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminProductResponse.class);

        // then
        assertAll(
            () -> assertThat(result.products()).hasSize(15),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(15),
            () -> assertThat(result.totalCount()).isEqualTo(16),
            () -> assertThat(result.totalPages()).isEqualTo(2)
        );
    }

    @DisplayName("페이지 번호는 1부터 시작하고 마지막 페이지에 남은 작품을 조회한다.")
    @Test
    void getAllProductsSecondPage() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        createProducts(space, 16);

        // when
        AdminProductResponse result = givenWithSession()
            .queryParam("page", 2)
            .when()
            .get("/admin/products")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminProductResponse.class);

        // then
        assertAll(
            () -> assertThat(result.products()).hasSize(1),
            () -> assertThat(result.currentPage()).isEqualTo(2)
        );
    }

    @DisplayName("세션이 없으면 작품 목록을 조회할 수 없다.")
    @Test
    void getAllProductsWithoutSession() {
        RestAssuredMockMvc.given()
            .when()
            .get("/admin/products")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("스페이스 코드로 작품 목록을 필터링한다.")
    @Test
    void getProductsBySpaceCode() {
        // given
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCode("1111111111"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("2222222222"));
        createProducts(space1, 2);
        createProducts(space2, 3);

        // when
        AdminProductResponse result = givenWithSession()
            .queryParam("spaceCode", "1111111111")
            .when()
            .get("/admin/products/search")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminProductResponse.class);

        // then
        assertAll(
            () -> assertThat(result.totalCount()).isEqualTo(2),
            () -> assertThat(result.products())
                .extracting(AdminProductSummaryResponse::spaceCode)
                .containsOnly("1111111111")
        );
    }

    @DisplayName("세션이 없으면 스페이스 코드로 작품 목록을 필터링할 수 없다.")
    @Test
    void getProductsBySpaceCodeWithoutSession() {
        RestAssuredMockMvc.given()
            .queryParam("spaceCode", "1111111111")
            .when()
            .get("/admin/products/search")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("작품명으로 작품을 검색한다.")
    @Test
    void searchProductsByTitle() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product matched = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "졸업 작품"));
        productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "습작"));

        // when
        AdminProductResponse result = givenWithSession()
            .queryParam("title", "업 작")
            .when()
            .get("/admin/products/search/by-title")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminProductResponse.class);

        // then
        assertThat(result.products())
            .extracting(AdminProductSummaryResponse::id)
            .containsExactly(matched.getId());
    }

    @DisplayName("작품명 검색어가 없으면 모든 작품을 반환한다.")
    @Test
    void searchProductsWithoutTitle() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        createProducts(space, 3);

        // when
        AdminProductResponse result = givenWithSession()
            .when()
            .get("/admin/products/search/by-title")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminProductResponse.class);

        // then
        assertThat(result.totalCount()).isEqualTo(3);
    }

    @DisplayName("세션이 없으면 작품명으로 작품을 검색할 수 없다.")
    @Test
    void searchProductsByTitleWithoutSession() {
        RestAssuredMockMvc.given()
            .queryParam("title", "작품")
            .when()
            .get("/admin/products/search/by-title")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("작품을 상세 조회하면 소속 스페이스와 사진 목록을 함께 반환한다.")
    @Test
    void getProductDetail() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName("1111111111", "졸업 전시"));
        Product product = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "고귀한 의자"));
        productPhotoRepository.save(ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 2));
        productPhotoRepository.save(ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 1));

        // when
        AdminProductDetailResponse result = givenWithSession()
            .when()
            .get("/admin/products/{productId}", product.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminProductDetailResponse.class);

        // then
        assertAll(
            () -> assertThat(result.id()).isEqualTo(product.getId()),
            () -> assertThat(result.title()).isEqualTo("고귀한 의자"),
            () -> assertThat(result.isVideoAfterPhoto()).isTrue(),
            () -> assertThat(result.space().code()).isEqualTo("1111111111"),
            () -> assertThat(result.space().name()).isEqualTo("졸업 전시"),
            () -> assertThat(result.photos())
                .extracting(AdminProductPhotoResponse::sortOrder)
                .containsExactly(1, 2)
        );
    }

    @DisplayName("사진이 없는 작품을 상세 조회하면 빈 사진 목록을 반환한다.")
    @Test
    void getProductDetailWithoutPhoto() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product product = productRepository.save(ProductFixture.createProductWithSpace(space));

        // when
        AdminProductDetailResponse result = givenWithSession()
            .when()
            .get("/admin/products/{productId}", product.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(AdminProductDetailResponse.class);

        // then
        assertThat(result.photos()).isEmpty();
    }

    @DisplayName("존재하지 않는 작품을 상세 조회하면 NOT_FOUND를 반환한다.")
    @Test
    void getProductDetailNotFound() {
        givenWithSession()
            .when()
            .get("/admin/products/{productId}", Long.MAX_VALUE)
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("code", equalTo("NOT_FOUND"));
    }

    @DisplayName("세션이 없으면 작품을 상세 조회할 수 없다.")
    @Test
    void getProductDetailWithoutSession() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product product = productRepository.save(ProductFixture.createProductWithSpace(space));

        // when & then
        RestAssuredMockMvc.given()
            .when()
            .get("/admin/products/{productId}", product.getId())
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("세션이 있으면 작품 관리 페이지를 렌더링한다.")
    @Test
    void renderProductsPage() {
        givenWithSession()
            .when()
            .get("/view/admin/products")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(containsString("text/html"))
            .body(containsString("작품 관리 페이지"))
            .body(containsString("/js/admin/products.js"));
    }

    @DisplayName("세션이 없으면 작품 관리 페이지 대신 로그인 페이지로 이동한다.")
    @Test
    void redirectProductsPageWithoutSession() {
        RestAssuredMockMvc.given()
            .when()
            .get("/view/admin/products")
            .then()
            .statusCode(HttpStatus.FOUND.value())
            .header("Location", equalTo("/view/admin/login"));
    }

    private void createProducts(Space space, int count) {
        for (int i = 0; i < count; i++) {
            productRepository.save(ProductFixture.createProductWithSpace(space));
        }
    }
}
