package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.product.dto.ProductResponseV2;
import com.forgather.domain.product.dto.RegisterProductPhotoRequest;
import com.forgather.domain.product.dto.RegisterProductRequestV2;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.AwsS3Cloud;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHostMap;
import com.forgather.global.auth.repository.SpaceHostMapRepository;
import com.forgather.global.auth.util.JwtTokenProvider;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
public class ProductAcceptanceV2Test extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceHostMapRepository spaceHostMapRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private AwsS3Cloud awsS3Cloud;

    private Space space;
    private String accessToken;
    private String anotherAccessToken;

    private RegisterProductRequestV2 registerRequest = new RegisterProductRequestV2(
        "title",
        "category",
        "authorName",
        "description",
        "https://youtu.be/lkuAxAVgAX0?si=OAobeoMmjeGurOHI",
        false,
        List.of(
            new RegisterProductPhotoRequest("photo1", "file1.png", 1024L),
            new RegisterProductPhotoRequest("photo2", "file2.png", 2048L),
            new RegisterProductPhotoRequest("photo3", "file3.png", 4096L)
        )
    );

    @BeforeEach
    void setUp() {
        space = SpaceFixture.createSpace();
        spaceRepository.save(space);

        Host host = HostFixture.createHost();
        Host anotherHost = HostFixture.createHost();
        hostRepository.save(host);
        hostRepository.save(anotherHost);
        spaceHostMapRepository.save(new SpaceHostMap(space, host));

        accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        anotherAccessToken = jwtTokenProvider.generateAccessToken(anotherHost.getId());

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("작품 등록")
    @Nested
    class registerProduct {
        @DisplayName("작품 등록")
        @Test
        void register() {
            // when
            ProductResponseV2 response = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "2")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(ProductResponseV2.class);

            // then
            assertAll(
                () -> assertThat(response.id()).isNotNull(),
                () -> assertThat(response.title()).isEqualTo(registerRequest.title()),
                () -> assertThat(response.category()).isEqualTo(registerRequest.category()),
                () -> assertThat(response.authorName()).isEqualTo(registerRequest.authorName()),
                () -> assertThat(response.description()).isEqualTo(registerRequest.description()),
                () -> assertThat(response.videoUrl()).isEqualTo(registerRequest.videoUrl()),
                () -> assertThat(response.isVideoAfterPhoto()).isEqualTo(registerRequest.isVideoAfterPhoto()),
                () -> assertThat(response.photos().get(0).originalName()).isEqualTo("photo1"),
                () -> assertThat(response.photos().get(0).path()).endsWith("/spaces/1234567890/product/file1.png"),
                () -> assertThat(response.photos().get(0).order()).isEqualTo(1),
                () -> assertThat(response.photos().get(1).originalName()).isEqualTo("photo2"),
                () -> assertThat(response.photos().get(1).path()).endsWith("/spaces/1234567890/product/file2.png"),
                () -> assertThat(response.photos().get(1).order()).isEqualTo(2),
                () -> assertThat(response.photos().get(2).originalName()).isEqualTo("photo3"),
                () -> assertThat(response.photos().get(2).path()).endsWith("/spaces/1234567890/product/file3.png"),
                () -> assertThat(response.photos().get(2).order()).isEqualTo(3)
            );
        }

        @DisplayName("중복 작품 등록 시 예외를 던진다")
        @Test
        void throwExceptionWhenProductAlreadyExists() {
            // given
            registerProduct();

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "2")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(400)
                .body("message", containsString("이미 등록된"));
        }

        @DisplayName("작품 설명을 1000자까지 작성할 수 있다")
        @Test
        void doesNotThrowAnyExceptionWhenMaxDescriptionLength() {
            // given
            RegisterProductRequestV2 registerRequest = new RegisterProductRequestV2(
                "title",
                "category",
                "authorName",
                "1234567890".repeat(100),
                "https://youtu.be/lkuAxAVgAX0?si=OAobeoMmjeGurOHI",
                false,
                List.of()
            );
            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "2")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(201);
        }

        @DisplayName("방문자가 작품을 등록하면 예외를 던진다")
        @Test
        void throwExceptionWhenGuestRegister() {
            // when, then
            RestAssuredMockMvc.given()
                .header("X-API-Version", "2")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(401)
                .body("message", containsString("로그인이 필요합니다."));
        }

        @DisplayName("다른 호스트가 작품을 등록하면 예외를 던진다")
        @Test
        void throwExceptionWhenAnotherHostRegister() {
            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .header("X-API-Version", "2")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(403)
                .body("message", containsString("해당 스페이스에 대한 접근 권한이 없습니다."));
        }
    }

    private ProductResponseV2 registerProduct() {
        return RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + accessToken)
            .header("X-API-Version", "2")
            .body(registerRequest)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .when()
            .post("/spaces/%s/products".formatted(space.getCode()))
            .then()
            .statusCode(201)
            .extract()
            .body()
            .as(ProductResponseV2.class);
    }
}
