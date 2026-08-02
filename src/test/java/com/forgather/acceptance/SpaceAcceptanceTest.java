package com.forgather.acceptance;

import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.repository.GuestBookCardPhotoRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.dto.CreateSpaceRequest;
import com.forgather.domain.space.dto.CreateSpaceResponse;
import com.forgather.domain.space.dto.FeaturedSpaceResponse;
import com.forgather.domain.space.dto.HostSpaceItemResponse;
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.dto.SpaceResponse;
import com.forgather.domain.space.dto.UpdateFeaturedSpaceRequest;
import com.forgather.domain.space.dto.UpdateSpaceRequest;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpacePhotoRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.fixture.GuestBookCardFixture;
import com.forgather.fixture.GuestBookCardPhotoFixture;
import com.forgather.fixture.ProductFixture;
import com.forgather.fixture.ProductPhotoFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.SpaceHostFixture;
import com.forgather.fixture.SpacePhotoFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@DisplayName("인수 테스트: Space")
@AutoConfigureMockMvc
class SpaceAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpacePhotoRepository spacePhotoRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private GuestBookCardPhotoRepository guestBookCardPhotoRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContentsStorage contentsStorage;

    private Host host;
    private String token;

    @BeforeEach
    void setUp() throws IOException {
        RestAssuredMockMvc.mockMvc(mockMvc);
        Mockito.when(contentsStorage.upload(any(), any()))
            .thenReturn("forgather/temp.png");

        host = createHost();
        hostRepository.save(host);
        token = jwtTokenProvider.generateAccessToken(host.getId());
    }

    @DisplayName("스페이스를 생성한다.")
    @Test
    void createSpace() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", null, null)
        );

        // when
        ApiResponse<CreateSpaceResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .multiPart("file", file.getOriginalFilename(), file.getBytes(), file.getContentType())
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(response.message()).isNull(),
            () -> assertThat(response.data().spaceCode()).isNotEmpty()
        );
    }

    @DisplayName("스페이스 사진이 없는 스페이스를 생성한다.")
    @Test
    void createSpaceWithoutFile() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", null, null)
        );

        // when
        ApiResponse<CreateSpaceResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(response.message()).isNull(),
            () -> assertThat(response.data().spaceCode()).isNotEmpty()
        );
    }

    @DisplayName("스페이스를 생성하려면 로그인이 필요하다.")
    @Test
    void createSpaceWithoutLogin() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .multiPart("request", request, "application/json")
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("링크 URL과 표시 이름을 함께 입력해 스페이스를 생성한다.")
    @Test
    void createSpaceWithLink() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", "https://forgather.me", "포트폴리오")
        );

        // when
        ApiResponse<CreateSpaceResponse> created = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        ApiResponse<SpaceResponse> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/spaces/{spaceCode}", created.data().spaceCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(result.data().linkUrl()).isEqualTo("https://forgather.me"),
            () -> assertThat(result.data().linkName()).isEqualTo("포트폴리오")
        );
    }

    @DisplayName("링크 URL만 입력하고 표시 이름을 누락하면 스페이스를 생성할 수 없다.")
    @Test
    void createSpaceWithOnlyLinkUrl() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", "https://forgather.me", null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("255자를 초과하는 긴 링크 URL도 저장하고 조회할 수 있다.")
    @Test
    void createSpaceWithLongLinkUrl() throws Exception {
        // given
        String longLinkUrl = "https://forgather.me/" + "a".repeat(300); // 321자 (255 초과, 2048 이하)
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", longLinkUrl, "포트폴리오")
        );

        // when
        ApiResponse<CreateSpaceResponse> created = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        ApiResponse<SpaceResponse> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/spaces/{spaceCode}", created.data().spaceCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(result.data().linkUrl()).isEqualTo(longLinkUrl);
    }

    @DisplayName("스페이스 이름이 30자를 초과하면 검증에 실패한다.")
    @Test
    void createSpaceWithOverLengthName() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("1234567890".repeat(3) + "1", "description", false, "forgather_official",
                "forgather@forgather.me", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("스페이스를 상세 조회한다.")
    @Test
    void getSpaceInformation() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        SpacePhoto spacePhoto = spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(space, "nickname1", "카드1"));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(space, "nickname2", "카드2"));
        GuestBookCard hiddenCard = GuestBookCardFixture.createGuestBookCard(space, "nickname3", "카드3");
        hiddenCard.hideByAdmin();
        guestBookCardRepository.save(hiddenCard);

        // when
        ApiResponse<SpaceResponse> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(result.message()).isNull(),
            () -> assertThat(result.data().spaceCode()).isEqualTo(space.getCode()),
            () -> assertThat(result.data().spacePhoto().path()).isEqualTo(spacePhoto.getPath()),
            () -> assertThat(result.data().guestBookCardCount()).isEqualTo(2)
        );
    }

    @DisplayName("스페이스를 삭제한다.")
    @Test
    void deleteSpace() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        // when
        var response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/spaces/{spaceCode}", space.getCode())
            .then()
            .extract();

        // then
        assertAll(
            () -> assertThat(response.statusCode()).isEqualTo(204),
            () -> assertThat(spaceRepository.findByCodeAndDeletedAtIsNull(space.getCode())).isEmpty(),
            () -> assertThat(spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)).isEmpty()
        );
    }

    @DisplayName("스페이스, 작품, 방명록 모두 삭제한다.")
    @Test
    void deleteSpaceWithRelatedThings() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));
        Product product = productRepository.save(ProductFixture.createProductWithSpace(space));
        productPhotoRepository.save(ProductPhotoFixture.createProductPhotoWithProduct(product));
        GuestBookCard guestBookCard = guestBookCardRepository.save(
            GuestBookCardFixture.createGuestBookCard(space, "nickname", "message"));
        guestBookCardPhotoRepository.saveAll(List.of(
            GuestBookCardPhotoFixture.createGuestBookCardPhotoWithGuestBookCard(guestBookCard)));

        // when
        var response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/spaces/{spaceCode}", space.getCode())
            .then()
            .extract();

        // then
        assertAll(
            () -> assertThat(response.statusCode()).isEqualTo(204),
            () -> assertThat(spaceRepository.findByCodeAndDeletedAtIsNull(space.getCode())).isEmpty(),
            () -> assertThat(spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)).isEmpty(),
            () -> assertThat(productRepository.findAllBySpaceAndDeletedAtIsNull(space)).isEmpty(),
            () -> assertThat(productPhotoRepository.findAllByProductAndDeletedAtIsNull(product)).isEmpty(),
            () -> assertThat(guestBookCardRepository.findAllBySpaceAndDeletedAtIsNull(space)).isEmpty(),
            () -> assertThat(
                guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(guestBookCard)).isEmpty()
        );
    }

    @DisplayName("로그인 없이 스페이스를 삭제할 수 없다.")
    @Test
    void deleteSpaceWithoutLogin() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        // when & then
        RestAssuredMockMvc.given()
            .when()
            .delete("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("스페이스의 호스트가 아니면 삭제할 수 없다.")
    @Test
    void deleteSpaceWithOtherHost() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));
        Host otherHost = hostRepository.save(createHost());
        String otherToken = jwtTokenProvider.generateAccessToken(otherHost.getId());

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + otherToken)
            .when()
            .delete("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("code", equalTo("FORBIDDEN"));
    }

    @DisplayName("스페이스를 수정한다.")
    @Test
    void updateSpace() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(new SpacePhoto(space, "original.png", "forgather/uuid.png", 1024L));
        spaceHostRepository.save(new SpaceHost(space, host));

        MockMultipartFile newFile = new MockMultipartFile(
            "file",
            "new.jpg",
            "image/jpeg",
            "new image content".getBytes()
        );
        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", "새로운 설명", false, "forgather_official_new", "forgather_new@forgather.me",
            "https://forgather.me", "포트폴리오", true)
        );

        // when
        ApiResponse<SpaceResponse> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .multiPart("file", newFile.getOriginalFilename(), newFile.getBytes(), newFile.getContentType())
            .when()
            .patch("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(result.message()).isNull(),
            () -> assertThat(result.data().name()).isEqualTo("새로운 스페이스"),
            () -> assertThat(result.data().description()).isEqualTo("새로운 설명"),
            () -> assertThat(result.data().isPublic()).isFalse(),
            () -> assertThat(result.data().instagramUsername()).isEqualTo("forgather_official_new"),
            () -> assertThat(result.data().email()).isEqualTo("forgather_new@forgather.me"),
            () -> assertThat(result.data().linkUrl()).isEqualTo("https://forgather.me"),
            () -> assertThat(result.data().linkName()).isEqualTo("포트폴리오"),
            () -> assertThat(
                spacePhotoRepository.getBySpaceAndDeletedAtIsNullOrEmpty(space).getOriginalName()).isEqualTo("new.jpg"),
            () -> assertThat(result.data().guestBookCardCount()).isZero()
        );
    }

    @DisplayName("스페이스 이름만 수정한다.")
    @Test
    void updateOnlySpaceName() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", null, null, null, null, null, null, false)
        );

        // when
        ApiResponse<SpaceResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .when()
            .patch("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(response.message()).isNull(),
            () -> assertThat(response.data().name()).isEqualTo("새로운 스페이스"),
            () -> assertThat(response.data().description()).isEqualTo("description"),
            () -> assertThat(response.data().isPublic()).isTrue(),
            () -> assertThat(response.data().instagramUsername()).isEqualTo("instagramUsername"),
            () -> assertThat(response.data().email()).isEqualTo("email@forgather.me"),
            () -> assertThat(response.data().spacePhoto().path()).isEqualTo("path"),

            () -> verify(contentsStorage, never()).deletePhotos(anyList())
        );
    }

    @DisplayName("로그인 없이 스페이스를 수정할 수 없다.")
    @Test
    void updateWithoutLogin() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", null, null, null, null, null, null, false)
        );

        // when & then
        RestAssuredMockMvc.given()
            .multiPart("request", request, "application/json")
            .when()
            .patch("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("스페이스의 호스트가 아니면 수정할 수 없다.")
    @Test
    void updateWithOtherHost() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));
        Host otherHost = hostRepository.save(createHost());
        String otherToken = jwtTokenProvider.generateAccessToken(otherHost.getId());

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", null, null, null, null, null, null, false)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + otherToken)
            .multiPart("request", request, "application/json")
            .when()
            .patch("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("code", equalTo("FORBIDDEN"));
    }

    @DisplayName("나의 스페이스 목록을 조회한다.")
    @Test
    void getSpaces() throws InterruptedException {
        // given
        Space space1 = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space1));
        Thread.sleep(1000);
        Space space2 = spaceRepository.save(SpaceFixture.createPrivateSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space2));
        spaceHostRepository.save(new SpaceHost(space1, host));
        spaceHostRepository.save(new SpaceHost(space2, host));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(space1, "nickname", "방명록1"));
        GuestBookCard hiddenCard = GuestBookCardFixture.createGuestBookCard(space1, "hidden", "숨김 방명록");
        hiddenCard.hideByAdmin();
        guestBookCardRepository.save(hiddenCard);

        // when
        ApiResponse<HostSpaceResponse> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/spaces/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(result.message()).isNull(),
            () -> assertThat(result.data().spaces().getFirst().spaceCode()).isEqualTo(space2.getCode()),
            () -> assertThat(result.data().spaces().getFirst().guestBookCardCount()).isZero(),

            () -> assertThat(result.data().spaces().getLast().spaceCode()).isEqualTo(space1.getCode()),
            () -> assertThat(result.data().spaces().getLast().guestBookCardCount()).isOne()
        );
    }

    @DisplayName("축하받는 스페이스로 지정하면 지정된 스페이스 코드를 응답한다.")
    @Test
    void feature() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when
        ApiResponse<FeaturedSpaceResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UpdateFeaturedSpaceRequest(space.getCode()))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(response.data().spaceCode()).isEqualTo(space.getCode())
        );
    }

    @DisplayName("축하받는 스페이스로 지정하면 요청한 스페이스만 지정 상태가 된다.")
    @Test
    void featureMarksOnlyTargetSpace() {
        // given
        Space target = saveSpaceOf(host, "1111111111");
        Space other = saveSpaceOf(host, "2222222222");

        // when
        feature(target.getCode());

        // then
        assertAll(
            () -> assertThat(isFeatured(target)).isTrue(),
            () -> assertThat(isFeatured(other)).isFalse()
        );
    }

    /**
     * "호스트당 최대 1개"는 DB 제약이 아니라 {@code SpaceService.updateFeaturedSpace()}가 보장한다.
     * 이 테스트가 그 불변식의 핵심 안전망이다.
     */
    @DisplayName("다른 스페이스를 지정하면 이전에 지정된 스페이스는 해제된다.")
    @Test
    void featureReplacesPreviousOne() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        feature(first.getCode());

        // when
        feature(second.getCode());

        // then
        assertAll(
            () -> assertThat(isFeatured(first)).isFalse(),
            () -> assertThat(isFeatured(second)).isTrue()
        );
    }

    @DisplayName("이미 지정된 스페이스를 다시 지정해도 지정 상태가 유지된다.")
    @Test
    void featureWithAlreadyFeaturedSpace() {
        // given
        Space space = saveSpaceOf(host, "1111111111");
        feature(space.getCode());

        // when
        feature(space.getCode());

        // then
        assertThat(isFeatured(space)).isTrue();
    }

    @DisplayName("한 번도 지정하지 않으면 모든 스페이스가 미지정 상태다.")
    @Test
    void notFeaturedByDefault() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");

        // when & then
        assertAll(
            () -> assertThat(isFeatured(first)).isFalse(),
            () -> assertThat(isFeatured(second)).isFalse()
        );
    }

    @DisplayName("다른 호스트의 스페이스는 축하받는 스페이스로 지정할 수 없다.")
    @Test
    void featureWithOtherHostSpace() {
        // given
        Host otherHost = hostRepository.save(createHost());
        Space otherSpace = saveSpaceOf(otherHost, "9999999999");

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UpdateFeaturedSpaceRequest(otherSpace.getCode()))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value())
            .body("code", equalTo("FORBIDDEN"))
            .body("message", containsString("권한이 존재하지 않습니다."));
    }

    @DisplayName("존재하지 않는 스페이스는 축하받는 스페이스로 지정할 수 없다.")
    @Test
    void featureWithNotExistingSpace() {
        // given
        String notExistingSpaceCode = "0000000000";

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UpdateFeaturedSpaceRequest(notExistingSpaceCode))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("code", equalTo("NOT_FOUND"))
            .body("message", containsString("존재하지 않는 스페이스입니다."));
    }

    @DisplayName("로그인하지 않으면 축하받는 스페이스를 지정할 수 없다.")
    @Test
    void featureWithoutLogin() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when & then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .body(new UpdateFeaturedSpaceRequest(space.getCode()))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("스페이스 코드가 공백이면 축하받는 스페이스를 지정할 수 없다.")
    @Test
    void featureWithBlankSpaceCode() {
        // given
        String blankSpaceCode = " ";

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UpdateFeaturedSpaceRequest(blankSpaceCode))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("지정된 스페이스를 삭제한 뒤에도 다른 스페이스를 축하받는 스페이스로 지정할 수 있다.")
    @Test
    void featureAfterDeletingFeaturedSpace() {
        // given
        Space featuredSpace = saveSpaceOf(host, "1111111111");
        Space other = saveSpaceOf(host, "2222222222");
        feature(featuredSpace.getCode());
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .delete("/spaces/{spaceCode}", featuredSpace.getCode())
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        // when
        feature(other.getCode());

        // then
        assertThat(isFeatured(other)).isTrue();
    }

    /**
     * 유일성을 서비스 계층이 지키므로, 스페이스 수정 API로 지정 상태가 바뀌면 그 보장이 통째로 우회된다.
     * {@code UpdateSpaceRequest}에 축하 여부 필드가 추가되는 것을 막는 회귀 테스트다.
     */
    @DisplayName("스페이스 정보를 수정해도 축하받는 스페이스 지정 상태는 바뀌지 않는다.")
    @Test
    void updateSpaceDoesNotChangeFeatured() throws Exception {
        // given
        Space space = saveSpaceOf(host, "1111111111");
        feature(space.getCode());
        String request = objectMapper.writeValueAsString(
            new UpdateSpaceRequest("새로운 이름", null, null, null, null, null, null, false)
        );

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .multiPart("request", request, "application/json")
            .when()
            .patch("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(HttpStatus.OK.value());

        // then
        assertThat(isFeatured(space)).isTrue();
    }

    @DisplayName("나의 스페이스 목록의 읽지 않은 방명록 수는 공개 상태이면서 읽지 않은 방명록만 센다.")
    @Test
    void getSpacesWithUnreadGuestBookCount() {
        // given
        Space space = saveSpaceOf(host, "1111111111");
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(space, "안읽음1", "방명록"));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(space, "안읽음2", "방명록"));

        GuestBookCard readCard = GuestBookCardFixture.createGuestBookCard(space, "읽음", "방명록");
        readCard.read(true);
        guestBookCardRepository.save(readCard);

        GuestBookCard hiddenCard = GuestBookCardFixture.createGuestBookCard(space, "숨김", "방명록");
        hiddenCard.hideByAdmin();
        guestBookCardRepository.save(hiddenCard);

        // when
        HostSpaceItemResponse response = findByCode(getMySpaces(), space.getCode());

        // then
        assertAll(
            () -> assertThat(response.guestBookCardCount()).isEqualTo(3L),
            () -> assertThat(response.unreadGuestBookCount()).isEqualTo(2L)
        );
    }

    @DisplayName("방명록이 없는 스페이스의 읽지 않은 방명록 수는 0이다.")
    @Test
    void getSpacesWithoutGuestBook() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when
        HostSpaceItemResponse response = findByCode(getMySpaces(), space.getCode());

        // then
        assertThat(response.unreadGuestBookCount()).isZero();
    }

    @DisplayName("나의 스페이스 목록에서 축하받는 스페이스로 지정된 항목은 항상 1개다.")
    @Test
    void getSpacesWithFeaturedSpace() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        feature(first.getCode());
        feature(second.getCode());

        // when
        List<HostSpaceItemResponse> spaces = getMySpaces();

        // then
        assertAll(
            () -> assertThat(findByCode(spaces, first.getCode()).isFeatured()).isFalse(),
            () -> assertThat(findByCode(spaces, second.getCode()).isFeatured()).isTrue(),
            () -> assertThat(spaces.stream().filter(HostSpaceItemResponse::isFeatured)).hasSize(1)
        );
    }

    private List<HostSpaceItemResponse> getMySpaces() {
        ApiResponse<HostSpaceResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/spaces/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        return response.data().spaces();
    }

    private HostSpaceItemResponse findByCode(List<HostSpaceItemResponse> spaces, String spaceCode) {
        return spaces.stream()
            .filter(space -> space.spaceCode().equals(spaceCode))
            .findFirst()
            .orElseThrow(() -> new AssertionError("스페이스를 찾을 수 없습니다. spaceCode: " + spaceCode));
    }

    private Space saveSpaceOf(Host owner, String spaceCode) {
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode(spaceCode));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space, owner));
        return space;
    }

    private void feature(String spaceCode) {
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UpdateFeaturedSpaceRequest(spaceCode))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private boolean isFeatured(Space space) {
        return spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(space.getCode()).isFeatured();
    }
}
