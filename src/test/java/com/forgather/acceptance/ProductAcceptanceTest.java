package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.product.dto.ProductResponse;
import com.forgather.domain.product.dto.ProductsResponse;
import com.forgather.domain.product.dto.RegisterProductPhotoRequest;
import com.forgather.domain.product.dto.RegisterProductRequest;
import com.forgather.domain.product.dto.UpdateProductRequest;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.fixture.ProductFixture;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.AwsS3Cloud;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
class ProductAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @MockitoBean
    private AwsS3Cloud awsS3Cloud;

    private Space space;
    private String accessToken;
    private String anotherAccessToken;

    private RegisterProductRequest registerRequest = new RegisterProductRequest(
        "title",
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
        spaceHostRepository.save(new SpaceHost(space, host));

        accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        anotherAccessToken = jwtTokenProvider.generateAccessToken(anotherHost.getId());

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("작품 목록 조회")
    @Nested
    class getAll {
        @DisplayName("작품 목록 조회")
        @Test
        void getAll() {
            // given
            ProductResponse registerResponse1 = registerProductV3();
            ProductResponse registerResponse2 = registerProductV3();

            // when
            ApiResponse<ProductsResponse> result = RestAssuredMockMvc.given()
                .header("X-API-Version", "3")
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().products().get(0).id()).isEqualTo(registerResponse1.id()),
                () -> assertThat(result.data().products().get(0).title()).isEqualTo(registerResponse1.title()),
                () -> assertThat(result.data().products().get(0).videoUrl()).isEqualTo(registerResponse1.videoUrl()),

                () -> assertThat(result.data().products().get(1).id()).isEqualTo(registerResponse2.id()),
                () -> assertThat(result.data().products().get(1).title()).isEqualTo(registerResponse2.title()),
                () -> assertThat(result.data().products().get(1).videoUrl()).isEqualTo(registerResponse2.videoUrl()),

                () -> assertThat(result.data().products().get(0).firstPhoto().originalName()).isEqualTo("photo1"),
                () -> assertThat(result.data().products().get(0).firstPhoto().path()).endsWith(
                    "/spaces/1234567890/product/file1.png"),
                () -> assertThat(result.data().products().get(0).firstPhoto().order()).isEqualTo(1)
            );
        }

        @DisplayName("사진이 저장된 순서와 무관하게 정렬 순서가 가장 앞선 사진을 첫 번째 사진으로 반환한다")
        @Test
        void returnFirstPhotoBySortOrder() {
            // given
            Product product = productRepository.save(ProductFixture.createProductWithSpace(space));
            productPhotoRepository.save(new ProductPhoto(product, "photo3", "path/third.webp", 1024L, 3));
            productPhotoRepository.save(new ProductPhoto(product, "photo2", "path/second.webp", 1024L, 2));
            productPhotoRepository.save(new ProductPhoto(product, "photo1", "path/first.webp", 1024L, 1));

            // when
            ApiResponse<ProductsResponse> result = RestAssuredMockMvc.given()
                .header("X-API-Version", "3")
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.data().products().getFirst().firstPhoto().order()).isEqualTo(1),
                () -> assertThat(result.data().products().getFirst().firstPhoto().path())
                    .isEqualTo("path/first.webp")
            );
        }

        @DisplayName("작품 목록 조회 시 등록된 작품이 없으면 빈 리스트를 반환한다")
        @Test
        void returnEmptyListWhenNoProducts() {
            // when
            ApiResponse<ProductsResponse> result = RestAssuredMockMvc.given()
                .header("X-API-Version", "3")
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().products()).isEmpty()
            );
        }
    }

    @Nested
    class getProductDetail {
        @DisplayName("작품 상세 조회")
        @Test
        void get() {
            // given
            ProductResponse registerResponse = registerProductV3();

            // when
            ApiResponse<ProductResponse> result = RestAssuredMockMvc.given()
                .header("X-API-Version", "1")
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/products/%d".formatted(space.getCode(), registerResponse.id()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().id()).isEqualTo(registerResponse.id()),
                () -> assertThat(result.data().title()).isEqualTo(registerResponse.title()),
                () -> assertThat(result.data().authorName()).isEqualTo(registerResponse.authorName()),
                () -> assertThat(result.data().description()).isEqualTo(registerResponse.description()),
                () -> assertThat(result.data().videoUrl()).isEqualTo(registerResponse.videoUrl()),
                () -> assertThat(result.data().isVideoAfterPhoto()).isEqualTo(registerResponse.isVideoAfterPhoto()),
                () -> assertThat(result.data().photos().get(0).originalName()).isEqualTo("photo1"),
                () -> assertThat(result.data().photos().get(0).path()).endsWith("/spaces/1234567890/product/file1.png"),
                () -> assertThat(result.data().photos().get(0).order()).isEqualTo(1),
                () -> assertThat(result.data().photos().get(1).originalName()).isEqualTo("photo2"),
                () -> assertThat(result.data().photos().get(1).path()).endsWith("/spaces/1234567890/product/file2.png"),
                () -> assertThat(result.data().photos().get(1).order()).isEqualTo(2),
                () -> assertThat(result.data().photos().get(2).originalName()).isEqualTo("photo3"),
                () -> assertThat(result.data().photos().get(2).path()).endsWith("/spaces/1234567890/product/file3.png"),
                () -> assertThat(result.data().photos().get(2).order()).isEqualTo(3)
            );
        }

        @DisplayName("작품 조회 시 해당 id의 작품이 존재하지 않으면 예외를 던진다")
        @Test
        void throwExceptionWhenNoProducts() {
            // when, then
            RestAssuredMockMvc.given()
                .header("X-API-Version", "1")
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/products/%d".formatted(space.getCode(), 1L))
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", containsString("해당 스페이스에 존재하지 않는 작품입니다"));
        }
    }

    @DisplayName("작품 등록")
    @Nested
    class registerProductV3 {
        @DisplayName("작품 등록")
        @Test
        void register() {
            // when
            ApiResponse<ProductResponse> response = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "3")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(response.message()).isNull(),
                () -> assertThat(response.data().id()).isNotNull(),
                () -> assertThat(response.data().title()).isEqualTo(registerRequest.title()),
                () -> assertThat(response.data().authorName()).isEqualTo(registerRequest.authorName()),
                () -> assertThat(response.data().description()).isEqualTo(registerRequest.description()),
                () -> assertThat(response.data().videoUrl()).isEqualTo(registerRequest.videoUrl()),
                () -> assertThat(response.data().isVideoAfterPhoto()).isEqualTo(registerRequest.isVideoAfterPhoto()),
                () -> assertThat(response.data().photos().get(0).originalName()).isEqualTo("photo1"),
                () -> assertThat(response.data().photos().get(0).path()).endsWith(
                    "/spaces/1234567890/product/file1.png"),
                () -> assertThat(response.data().photos().get(0).order()).isEqualTo(1),
                () -> assertThat(response.data().photos().get(1).originalName()).isEqualTo("photo2"),
                () -> assertThat(response.data().photos().get(1).path()).endsWith(
                    "/spaces/1234567890/product/file2.png"),
                () -> assertThat(response.data().photos().get(1).order()).isEqualTo(2),
                () -> assertThat(response.data().photos().get(2).originalName()).isEqualTo("photo3"),
                () -> assertThat(response.data().photos().get(2).path()).endsWith(
                    "/spaces/1234567890/product/file3.png"),
                () -> assertThat(response.data().photos().get(2).order()).isEqualTo(3)
            );
        }

        @DisplayName("작품 복수 등록이 가능하다")
        @Test
        void registerMultipleProducts() {
            // given
            registerProductV3();
            registerProductV3();

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "3")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(201);
        }

        @DisplayName("작품 개수 제한 없이 3개를 초과해서 등록할 수 있다")
        @Test
        void registerProductsWithoutMaxCountLimit() {
            // given
            registerProductV3();
            registerProductV3();
            registerProductV3();

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "3")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(201);
        }

        @DisplayName("작품 설명을 2000자까지 작성할 수 있다")
        @Test
        void doesNotThrowAnyExceptionWhenMaxDescriptionLength() {
            // given
            RegisterProductRequest registerRequest = new RegisterProductRequest(
                "title",
                "authorName",
                "1234567890".repeat(200),
                "https://youtu.be/lkuAxAVgAX0?si=OAobeoMmjeGurOHI",
                false,
                List.of()
            );
            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "3")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(201);
        }

        @DisplayName("작품 설명 없이 작품을 등록하면 빈 문자열로 저장된다")
        @Test
        void registerWithoutDescription() {
            // given
            RegisterProductRequest request = new RegisterProductRequest(
                "title",
                "authorName",
                null,
                "https://youtu.be/lkuAxAVgAX0?si=OAobeoMmjeGurOHI",
                false,
                List.of()
            );

            // when
            ApiResponse<ProductResponse> response = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "3")
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(response.data().description()).isEqualTo("")
            );
        }

        @DisplayName("멀티 코드포인트 이모지로만 이루어진 작품명 50자를 등록할 수 있다")
        @Test
        void registerWithMultiCodePointEmojiTitle() {
            // given
            String title = "👨‍👩‍👧‍👦".repeat(50); // grapheme 50자, 코드포인트 350개
            RegisterProductRequest request = new RegisterProductRequest(
                title,
                "authorName",
                "description",
                "https://youtu.be/lkuAxAVgAX0?si=OAobeoMmjeGurOHI",
                false,
                List.of()
            );

            // when
            ApiResponse<ProductResponse> response = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "3")
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(201)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(response.data().title()).isEqualTo(title)
            );
        }

        @DisplayName("작품명이 50자를 초과하면 검증에 실패한다")
        @Test
        void throwExceptionWhenTitleExceedMaxLength() {
            // given
            RegisterProductRequest request = new RegisterProductRequest(
                "1234567890".repeat(5) + "1",
                "authorName",
                "description",
                "https://youtu.be/lkuAxAVgAX0?si=OAobeoMmjeGurOHI",
                false,
                List.of()
            );

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "3")
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"));
        }

        @DisplayName("방문자가 작품을 등록하면 예외를 던진다")
        @Test
        void throwExceptionWhenGuestRegister() {
            // when, then
            RestAssuredMockMvc.given()
                .header("X-API-Version", "3")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("message", containsString("로그인이 필요합니다."));
        }

        @DisplayName("다른 호스트가 작품을 등록하면 예외를 던진다")
        @Test
        void throwExceptionWhenAnotherHostRegister() {
            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .header("X-API-Version", "3")
                .body(registerRequest)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/products".formatted(space.getCode()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("해당 스페이스에 대한 접근 권한이 없습니다."));
        }
    }

    @DisplayName("작품 수정")
    @Nested
    class updateProductV3 {
        @DisplayName("작품 정보 수정")
        @Test
        void update() {
            // given
            ProductResponse registerResponse = registerProductV3();
            Mockito.doNothing().when(awsS3Cloud).deleteContents(Mockito.anyList());
            UpdateProductRequest request = new UpdateProductRequest(
                "foovar1",
                null,
                "description",
                "https://youtu.be/aaa",
                true,
                List.of(2L),
                List.of(
                    new RegisterProductPhotoRequest("photo4", "file4.png", 1024L),
                    new RegisterProductPhotoRequest("photo5", "file5.png", 1024L)
                )
            );

            // when
            ApiResponse<ProductResponse> result = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "1")
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .patch("/spaces/%s/products/%d".formatted(space.getCode(), registerResponse.id()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().id()).isEqualTo(registerResponse.id()),
                () -> assertThat(result.data().title()).isEqualTo(request.title()),
                () -> assertThat(result.data().authorName()).isEqualTo(registerResponse.authorName()),
                () -> assertThat(result.data().description()).isEqualTo(request.description()),
                () -> assertThat(result.data().videoUrl()).isEqualTo(request.videoUrl()),
                () -> assertThat(result.data().isVideoAfterPhoto()).isEqualTo(request.isVideoAfterPhoto()),
                () -> assertThat(result.data().photos().get(0).originalName()).isEqualTo("photo1"),
                () -> assertThat(result.data().photos().get(0).path()).endsWith("/spaces/1234567890/product/file1.png"),
                () -> assertThat(result.data().photos().get(0).order()).isEqualTo(1),
                () -> assertThat(result.data().photos().get(1).originalName()).isEqualTo("photo3"),
                () -> assertThat(result.data().photos().get(1).path()).endsWith("/spaces/1234567890/product/file3.png"),
                () -> assertThat(result.data().photos().get(1).order()).isEqualTo(2),
                () -> assertThat(result.data().photos().get(2).originalName()).isEqualTo("photo4"),
                () -> assertThat(result.data().photos().get(2).path()).endsWith("/spaces/1234567890/product/file4.png"),
                () -> assertThat(result.data().photos().get(2).order()).isEqualTo(3),
                () -> assertThat(result.data().photos().get(3).originalName()).isEqualTo("photo5"),
                () -> assertThat(result.data().photos().get(3).path()).endsWith("/spaces/1234567890/product/file5.png"),
                () -> assertThat(result.data().photos().get(3).order()).isEqualTo(4)
            );
        }

        @DisplayName("작품 정보 수정 중 작품 것이 아닌 사진 삭제 시 예외를 던진다")
        @Test
        void throwExceptionWhenInvalidDeleteId() {
            // given
            ProductResponse registerResponse = registerProductV3();
            UpdateProductRequest request = new UpdateProductRequest(
                "foovar1",
                null,
                "description",
                "https://youtu.be/aaa",
                true,
                List.of((long)(registerResponse.photos().size() + 10)),
                List.of()
            );

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "1")
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .patch("/spaces/%s/products/%d".formatted(space.getCode(), registerResponse.id()))
                .then()
                .statusCode(400)
                .body("code", equalTo("BAD_REQUEST"))
                .body("message", containsString("작품에 존재하지 않는 사진입니다."));
        }

        @DisplayName("방문자가 작품 정보를 수정하면 예외를 던진다")
        @Test
        void throwExceptionWhenGuestUpdate() {
            // given
            ProductResponse registerResponse = registerProductV3();
            Mockito.doNothing().when(awsS3Cloud).deleteContents(Mockito.anyList());
            UpdateProductRequest request = new UpdateProductRequest(
                "foovar1",
                null,
                "description",
                "https://youtu.be/aaa",
                true,
                List.of(2L),
                List.of(
                    new RegisterProductPhotoRequest("photo4", "file4.png", 1024L),
                    new RegisterProductPhotoRequest("photo5", "file5.png", 1024L)
                )
            );

            // when
            RestAssuredMockMvc.given()
                .header("X-API-Version", "1")
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .patch("/spaces/%s/products/%d".formatted(space.getCode(), registerResponse.id()))
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("message", containsString("로그인이 필요합니다."));
        }

        @DisplayName("다른 호스트가 작품 정보를 수정하면 예외를 던진다")
        @Test
        void throwExceptionWhenAnotherHostUpdate() {
            // given
            ProductResponse registerResponse = registerProductV3();
            Mockito.doNothing().when(awsS3Cloud).deleteContents(Mockito.anyList());
            UpdateProductRequest request = new UpdateProductRequest(
                "foovar1",
                null,
                "description",
                "https://youtu.be/aaa",
                true,
                List.of(2L),
                List.of(
                    new RegisterProductPhotoRequest("photo4", "file4.png", 1024L),
                    new RegisterProductPhotoRequest("photo5", "file5.png", 1024L)
                )
            );

            // when
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .header("X-API-Version", "1")
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .patch("/spaces/%s/products/%d".formatted(space.getCode(), registerResponse.id()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("해당 스페이스에 대한 접근 권한이 없습니다."));
        }
    }

    @DisplayName("작품 삭제")
    @Nested
    class deleteProductV2 {
        @DisplayName("작품 삭제")
        @Test
        void delete() {
            // given
            ProductResponse registerResponse = registerProductV3();
            Mockito.doNothing().when(awsS3Cloud).deleteContents(Mockito.anyList());

            // when, then
            RestAssuredMockMvc
                .given()
                .header("Authorization", "Bearer " + accessToken)
                .header("X-API-Version", "1")
                .when()
                .delete("/spaces/%s/products/%d".formatted(space.getCode(), registerResponse.id()))
                .then()
                .statusCode(204);

            assertThat(productRepository.findBySpaceAndIdAndDeletedAtIsNull(space, registerResponse.id())).isEmpty();
        }

        @DisplayName("방문자가 작품을 삭제하면 예외를 던진다")
        @Test
        void throwExceptionWhenGuestDelete() {
            // given
            ProductResponse registerResponse = registerProductV3();
            Mockito.doNothing().when(awsS3Cloud).deleteContents(Mockito.anyList());

            // when, then
            RestAssuredMockMvc
                .given()
                .header("X-API-Version", "1")
                .when()
                .delete("/spaces/%s/products/%d".formatted(space.getCode(), registerResponse.id()))
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("message", containsString("로그인이 필요합니다."));
        }

        @DisplayName("다른 호스트가 작품을 삭제하면 예외를 던진다")
        @Test
        void throwExceptionWhenAnotherHostDelete() {
            // given
            ProductResponse registerResponse = registerProductV3();
            Mockito.doNothing().when(awsS3Cloud).deleteContents(Mockito.anyList());

            // when, then
            RestAssuredMockMvc
                .given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .header("X-API-Version", "1")
                .when()
                .delete("/spaces/%s/products/%d".formatted(space.getCode(), registerResponse.id()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("해당 스페이스에 대한 접근 권한이 없습니다."));
        }
    }

    private ProductResponse registerProductV3() {
        ApiResponse<ProductResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + accessToken)
            .header("X-API-Version", "3")
            .body(registerRequest)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .when()
            .post("/spaces/%s/products".formatted(space.getCode()))
            .then()
            .statusCode(201)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        return response.data();
    }
}
