package com.forgather.acceptance;

import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
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
import com.forgather.domain.space.dto.FeatureSpacesRequest;
import com.forgather.domain.space.dto.FeaturedSpacesResponse;
import com.forgather.domain.space.dto.HostSpaceItemResponse;
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.dto.SpacePhotoRequest;
import com.forgather.domain.space.dto.SpaceResponse;
import com.forgather.domain.space.dto.UnfeatureSpacesRequest;
import com.forgather.domain.space.dto.UpdateSpaceRequest;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpacePhotoRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.domain.upload.domain.UploadFileMetadata;
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

    private static final String ROOT_DIRECTORY = "photogather/v2";
    private static final String UPLOAD_FILE_NAME = "0f8e7d6c-1234-5678-9abc-def012345678.webp";

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
        Mockito.when(contentsStorage.getRootDirectory()).thenReturn(ROOT_DIRECTORY);

        host = createHost();
        hostRepository.save(host);
        token = jwtTokenProvider.generateAccessToken(host.getId());
    }

    @DisplayName("스페이스를 생성한다.")
    @Test
    void createSpace() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", null, null, new SpacePhotoRequest(UPLOAD_FILE_NAME, 102400L))
        );

        // when
        ApiResponse<CreateSpaceResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(response.data().spaceCode());
        assertAll(
            () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(response.message()).isNull(),
            () -> assertThat(response.data().spaceCode()).isNotEmpty(),
            () -> assertThat(spacePhotoRepository.getBySpaceAndDeletedAtIsNullOrEmpty(space).getPath())
                .isEqualTo(expectedPhotoPath())
        );
    }

    @DisplayName("스페이스 사진 파일명 형식이 올바르지 않으면 스페이스를 생성할 수 없다.")
    @Test
    void createSpaceWithInvalidPhotoFileName() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", null, null, new SpacePhotoRequest("../../etc/passwd", 102400L))
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("스페이스 사진 크기가 20MB를 초과하면 스페이스를 생성할 수 없다.")
    @Test
    void createSpaceWithOversizePhoto() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", null, null,
                new SpacePhotoRequest(UPLOAD_FILE_NAME, UploadFileMetadata.MAX_FILE_SIZE_BYTES + 1))
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/spaces")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("스페이스 사진이 없는 스페이스를 생성한다.")
    @Test
    void createSpaceWithoutFile() throws Exception {
        // given
        String request = objectMapper.writeValueAsString(
            new CreateSpaceRequest("test-space", "description", false, "forgather_official",
                "forgather@forgather.me", null, null, null)
        );

        // when
        ApiResponse<CreateSpaceResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
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
                "forgather@forgather.me", null, null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .body(request)
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
                "forgather@forgather.me", "https://forgather.me", "포트폴리오", null)
        );

        // when
        ApiResponse<CreateSpaceResponse> created = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
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
                "forgather@forgather.me", "https://forgather.me", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
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
                "forgather@forgather.me", longLinkUrl, "포트폴리오", null)
        );

        // when
        ApiResponse<CreateSpaceResponse> created = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
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
                "forgather@forgather.me", null, null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
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
        spacePhotoRepository.save(new SpacePhoto(space, "forgather/uuid.png", 1024L));
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", "새로운 설명", false, "forgather_official_new", "forgather_new@forgather.me",
            "https://forgather.me", "포트폴리오", true, new SpacePhotoRequest(UPLOAD_FILE_NAME, 102400L))
        );

        // when
        ApiResponse<SpaceResponse> result = patchSpace(space, request, HttpStatus.OK);

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
            () -> assertThat(spacePhotoRepository.getBySpaceAndDeletedAtIsNullOrEmpty(space).getPath())
                .isEqualTo(expectedPhotoPath()),
            () -> assertThat(result.data().guestBookCardCount()).isZero()
        );
    }

    @DisplayName("삭제 요청만 보내면 스페이스 사진만 삭제된다.")
    @Test
    void updateSpaceDeletingOnlyPhoto() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            null, null, null, null, null, null, null, true, null)
        );

        // when
        ApiResponse<SpaceResponse> result = patchSpace(space, request, HttpStatus.OK);

        // then
        assertAll(
            () -> assertThat(result.data().spacePhoto().isExists()).isFalse(),
            () -> assertThat(spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)).isEmpty()
        );
    }

    @DisplayName("기존 사진이 없으면 삭제 요청 없이 새 사진만 보내도 등록된다.")
    @Test
    void updateSpaceSavesPhotoWithoutDeleteFlagWhenNoExistingPhoto() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            null, null, null, null, null, null, null, false, new SpacePhotoRequest(UPLOAD_FILE_NAME, 102400L))
        );

        // when
        ApiResponse<SpaceResponse> result = patchSpace(space, request, HttpStatus.OK);

        // then
        assertAll(
            () -> assertThat(result.data().spacePhoto().isExists()).isTrue(),
            () -> assertThat(result.data().spacePhoto().path())
                .isEqualTo(expectedPhotoPath()),
            () -> assertThat(spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)).isPresent()
        );
    }

    @DisplayName("기존 사진이 있으면 삭제 요청 없이 새 사진만 보내도 교체된다.")
    @Test
    void updateSpaceReplacesPhotoWithoutDeleteFlag() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            null, null, null, null, null, null, null, false, new SpacePhotoRequest(UPLOAD_FILE_NAME, 102400L))
        );

        // when
        ApiResponse<SpaceResponse> result = patchSpace(space, request, HttpStatus.OK);

        // then
        assertAll(
            () -> assertThat(result.data().spacePhoto().path())
                .isEqualTo(expectedPhotoPath()),
            () -> assertThat(spacePhotoRepository.findAllBySpaceIdInAndDeletedAtIsNull(List.of(space.getId())))
                .hasSize(1)
        );
    }

    @DisplayName("기존 사진이 없어도 삭제 요청과 함께 보낸 새 사진이 등록된다.")
    @Test
    void updateSpaceSavesPhotoWithDeleteFlagWhenNoExistingPhoto() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            null, null, null, null, null, null, null, true, new SpacePhotoRequest(UPLOAD_FILE_NAME, 102400L))
        );

        // when
        ApiResponse<SpaceResponse> result = patchSpace(space, request, HttpStatus.OK);

        // then
        assertThat(result.data().spacePhoto().path())
            .isEqualTo(expectedPhotoPath());
    }

    @DisplayName("기존 사진이 없는데 삭제를 요청해도 성공한다.")
    @Test
    void updateSpaceDeletingNotExistingPhoto() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            null, null, null, null, null, null, null, true, null)
        );

        // when
        ApiResponse<SpaceResponse> result = patchSpace(space, request, HttpStatus.OK);

        // then
        assertAll(
            () -> assertThat(result.data().spacePhoto().isExists()).isFalse(),
            () -> assertThat(spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)).isEmpty()
        );
    }

    @DisplayName("사진 삭제를 두 번 연속 요청해도 모두 성공한다.")
    @Test
    void updateSpaceDeletingPhotoIsIdempotent() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            null, null, null, null, null, null, null, true, null)
        );

        // when
        patchSpace(space, request, HttpStatus.OK);
        ApiResponse<SpaceResponse> second = patchSpace(space, request, HttpStatus.OK);

        // then
        assertAll(
            () -> assertThat(second.data().spacePhoto().isExists()).isFalse(),
            () -> assertThat(spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)).isEmpty()
        );
    }
    
    @DisplayName("isDeletePhoto를 생략해도 수정에 성공하고 기존 사진이 유지된다.")
    @Test
    void updateSpaceWithoutDeletePhotoField() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = """
            { "name": "이름만 수정" }
            """;

        // when
        ApiResponse<SpaceResponse> result = patchSpace(space, request, HttpStatus.OK);

        // then
        assertAll(
            () -> assertThat(result.data().name()).isEqualTo("이름만 수정"),
            () -> assertThat(result.data().spacePhoto().isExists()).isTrue(),
            () -> assertThat(spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)).isPresent()
        );
    }

    /**
     * 발급·등록 모두 인증된 hostId로 경로를 격리한다. 격리가 빠지면 클라이언트가 지정한 파일명만으로
     * 다른 호스트의 객체 키를 겨냥할 수 있으므로, 기대 경로에 hostId가 들어가는지 함께 고정한다.
     */
    private String expectedPhotoPath() {
        return ROOT_DIRECTORY + "/spaces/photos/" + host.getId() + "/" + UPLOAD_FILE_NAME;
    }

    private ApiResponse<SpaceResponse> patchSpace(Space space, String request, HttpStatus expected) {
        return RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .patch("/spaces/{spaceCode}", space.getCode())
            .then()
            .statusCode(expected.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
    }

    @DisplayName("스페이스 이름만 수정한다.")
    @Test
    void updateOnlySpaceName() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostRepository.save(new SpaceHost(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", null, null, null, null, null, null, false, null)
        );

        // when
        ApiResponse<SpaceResponse> response = patchSpace(space, request, HttpStatus.OK);

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
            "새로운 스페이스", null, null, null, null, null, null, false, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .body(request)
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
            "새로운 스페이스", null, null, null, null, null, null, false, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + otherToken)
            .contentType(ContentType.JSON)
            .body(request)
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

    @DisplayName("여러 스페이스를 축하받는 스페이스로 한 번에 지정하고 지정된 코드 목록을 응답한다.")
    @Test
    void featureSpaces() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");

        // when
        ApiResponse<FeaturedSpacesResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(List.of(first.getCode(), second.getCode())))
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
            () -> assertThat(response.data().featuredSpaceCodes())
                .containsExactlyInAnyOrder(first.getCode(), second.getCode()),
            () -> assertThat(isFeatured(first)).isTrue(),
            () -> assertThat(isFeatured(second)).isTrue()
        );
    }

    /**
     * 지정 API는 요청한 스페이스만 켠다. 해제는 별도 해제 API의 책임이므로, 요청에서 빠졌다는 이유로
     * 기존 지정이 풀려서는 안 된다. 이 API가 교체 의미를 갖지 않는다는 것을 고정하는 테스트다.
     */
    @DisplayName("요청에 포함되지 않은 스페이스의 지정 상태는 그대로 유지된다.")
    @Test
    void featureSpacesKeepsOmittedSpaces() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        feature(first.getCode(), second.getCode());

        // when
        feature(second.getCode());

        // then
        assertAll(
            () -> assertThat(isFeatured(first)).isTrue(),
            () -> assertThat(isFeatured(second)).isTrue()
        );
    }

    @DisplayName("응답에는 이번에 지정한 스페이스와 이전에 지정되어 있던 스페이스가 함께 담긴다.")
    @Test
    void featureSpacesRespondsWithAllFeaturedSpaces() {
        // given
        Space previous = saveSpaceOf(host, "1111111111");
        Space target = saveSpaceOf(host, "2222222222");
        feature(previous.getCode());

        // when
        ApiResponse<FeaturedSpacesResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(List.of(target.getCode())))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(response.data().featuredSpaceCodes())
            .containsExactlyInAnyOrder(previous.getCode(), target.getCode());
    }

    @DisplayName("호스트가 가진 모든 스페이스를 축하받는 스페이스로 지정할 수 있다.")
    @Test
    void featureSpacesWithAllSpaces() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        Space third = saveSpaceOf(host, "3333333333");

        // when
        feature(first.getCode(), second.getCode(), third.getCode());

        // then
        assertAll(
            () -> assertThat(isFeatured(first)).isTrue(),
            () -> assertThat(isFeatured(second)).isTrue(),
            () -> assertThat(isFeatured(third)).isTrue()
        );
    }

    @DisplayName("빈 목록을 요청하면 축하받는 스페이스를 지정할 수 없다.")
    @Test
    void featureSpacesWithEmptyList() {
        // given
        List<String> emptySpaceCodes = List.of();

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(emptySpaceCodes))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"))
            .body("message", containsString("스페이스 코드는 1개 이상 100개 이하로 요청할 수 있습니다."));
    }

    @DisplayName("같은 목록으로 다시 지정해도 지정 상태가 유지된다.")
    @Test
    void featureSpacesIsIdempotent() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        feature(first.getCode(), second.getCode());

        // when
        feature(first.getCode(), second.getCode());

        // then
        assertAll(
            () -> assertThat(isFeatured(first)).isTrue(),
            () -> assertThat(isFeatured(second)).isTrue()
        );
    }

    @DisplayName("같은 스페이스 코드가 중복으로 들어와도 집합으로 취급해 한 번만 지정된다.")
    @Test
    void featureSpacesWithDuplicatedCodes() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when
        ApiResponse<FeaturedSpacesResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(List.of(space.getCode(), space.getCode())))
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
            () -> assertThat(response.data().featuredSpaceCodes()).containsExactly(space.getCode()),
            () -> assertThat(isFeatured(space)).isTrue()
        );
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

    /**
     * 부분 반영을 허용하면 클라이언트가 최종 상태를 알 수 없다. 목록에 남의 스페이스가 섞이면
     * 요청 전체가 실패하고 기존 지정 상태가 그대로 남아야 한다.
     */
    @DisplayName("목록에 다른 호스트의 스페이스가 섞이면 전체가 실패하고 기존 지정 상태가 유지된다.")
    @Test
    void featureSpacesWithOtherHostSpace() {
        // given
        Space featured = saveSpaceOf(host, "1111111111");
        Space notFeatured = saveSpaceOf(host, "2222222222");
        feature(featured.getCode());
        Host otherHost = hostRepository.save(createHost());
        Space otherSpace = saveSpaceOf(otherHost, "9999999999");

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(List.of(notFeatured.getCode(), otherSpace.getCode())))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("BAD_REQUEST"))
            .body("message", containsString("유효하지 않은 스페이스 코드입니다."));

        // then
        assertAll(
            () -> assertThat(isFeatured(featured)).isTrue(),
            () -> assertThat(isFeatured(notFeatured)).isFalse(),
            () -> assertThat(isFeatured(otherSpace)).isFalse()
        );
    }

    @DisplayName("목록에 존재하지 않는 스페이스가 섞이면 전체가 실패하고 기존 지정 상태가 유지된다.")
    @Test
    void featureSpacesWithNotExistingSpace() {
        // given
        Space featured = saveSpaceOf(host, "1111111111");
        Space notFeatured = saveSpaceOf(host, "2222222222");
        feature(featured.getCode());
        String notExistingSpaceCode = "0000000000";

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(List.of(notFeatured.getCode(), notExistingSpaceCode)))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("BAD_REQUEST"))
            .body("message", containsString("유효하지 않은 스페이스 코드입니다."));

        // then
        assertAll(
            () -> assertThat(isFeatured(featured)).isTrue(),
            () -> assertThat(isFeatured(notFeatured)).isFalse()
        );
    }

    @DisplayName("로그인하지 않으면 축하받는 스페이스를 지정할 수 없다.")
    @Test
    void featureSpacesWithoutLogin() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when & then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(List.of(space.getCode())))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("스페이스 코드 목록이 null이면 축하받는 스페이스를 지정할 수 없다.")
    @Test
    void featureSpacesWithNullSpaceCodes() {
        // given
        List<String> nullSpaceCodes = null;

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(nullSpaceCodes))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    /**
     * 요청 목록은 호스트가 가진 스페이스 수를 넘을 이유가 없다. 상한이 없으면 악의적 요청이
     * 임의 크기의 목록을 보내 역직렬화 단계에서 메모리를 소모시킬 수 있으므로 경계에서 막는다.
     */
    @DisplayName("스페이스 코드를 100개 넘게 요청하면 축하받는 스페이스를 지정할 수 없다.")
    @Test
    void featureSpacesWithTooManySpaceCodes() {
        // given
        List<String> tooManySpaceCodes = IntStream.rangeClosed(1, 101)
            .mapToObj(number -> "%010d".formatted(number))
            .toList();

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(tooManySpaceCodes))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("스페이스 코드 목록에 공백이 섞이면 축하받는 스페이스를 지정할 수 없다.")
    @Test
    void featureSpacesWithBlankSpaceCode() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(List.of(space.getCode(), " ")))
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
     * 지정 상태는 전용 엔드포인트로만 바뀌어야 한다. 스페이스 수정 API로도 바뀌면 지정/해제 경로가
     * 이원화되므로, {@code UpdateSpaceRequest}에 축하 여부 필드가 추가되는 것을 막는 회귀 테스트다.
     */
    @DisplayName("스페이스 정보를 수정해도 축하받는 스페이스 지정 상태는 바뀌지 않는다.")
    @Test
    void updateSpaceDoesNotChangeFeatured() throws Exception {
        // given
        Space space = saveSpaceOf(host, "1111111111");
        feature(space.getCode());
        String request = objectMapper.writeValueAsString(
            new UpdateSpaceRequest("새로운 이름", null, null, null, null, null, null, false, null)
        );

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(request)
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

    @DisplayName("나의 스페이스 목록은 스페이스별 지정 여부를 그대로 내려준다.")
    @Test
    void getSpacesWithFeaturedSpace() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        Space third = saveSpaceOf(host, "3333333333");
        feature(first.getCode(), second.getCode());

        // when
        List<HostSpaceItemResponse> spaces = getMySpaces();

        // then
        assertAll(
            () -> assertThat(findByCode(spaces, first.getCode()).isFeatured()).isTrue(),
            () -> assertThat(findByCode(spaces, second.getCode()).isFeatured()).isTrue(),
            () -> assertThat(findByCode(spaces, third.getCode()).isFeatured()).isFalse(),
            () -> assertThat(spaces.stream().filter(HostSpaceItemResponse::isFeatured)).hasSize(2)
        );
    }

    @DisplayName("지정된 스페이스 중 일부만 해제하고 data 없이 200으로 응답한다.")
    @Test
    void unfeatureSpaces() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        feature(first.getCode(), second.getCode());

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(List.of(first.getCode())))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("code", equalTo("SUCCESS"))
            .body("data", nullValue());

        // then
        assertAll(
            () -> assertThat(isFeatured(first)).isFalse(),
            () -> assertThat(isFeatured(second)).isTrue()
        );
    }

    /**
     * 해제 API는 요청한 스페이스만 끈다. 요청에서 빠졌다는 이유로 기존 지정이 풀리면
     * 클라이언트가 매번 전체 목록을 보내야 하므로, 이 API가 교체 의미를 갖지 않는다는 것을 고정하는 테스트다.
     */
    @DisplayName("해제 요청에 포함되지 않은 스페이스의 지정 상태는 그대로 유지된다.")
    @Test
    void unfeatureSpacesKeepsOmittedSpaces() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        feature(first.getCode(), second.getCode());

        // when
        unfeature(first.getCode());

        // then
        assertThat(isFeatured(second)).isTrue();
    }

    /**
     * DELETE는 멱등해야 한다. 이미 미지정인 스페이스를 해제해도 실패하지 않아야
     * 클라이언트가 현재 지정 상태를 몰라도 안전하게 재시도할 수 있다.
     */
    @DisplayName("지정되지 않은 스페이스를 해제해도 성공 응답을 반환한다.")
    @Test
    void unfeatureSpacesIsIdempotent() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when
        unfeature(space.getCode());

        // then
        assertThat(isFeatured(space)).isFalse();
    }

    @DisplayName("같은 스페이스 코드가 중복으로 들어와도 집합으로 취급해 한 번만 해제된다.")
    @Test
    void unfeatureSpacesWithDuplicatedCodes() {
        // given
        Space space = saveSpaceOf(host, "1111111111");
        feature(space.getCode());

        // when
        unfeature(space.getCode(), space.getCode());

        // then
        assertThat(isFeatured(space)).isFalse();
    }

    @DisplayName("빈 목록을 요청하면 축하받는 스페이스를 해제할 수 없다.")
    @Test
    void unfeatureSpacesWithEmptyList() {
        // given
        List<String> emptySpaceCodes = List.of();

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(emptySpaceCodes))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"))
            .body("message", containsString("스페이스 코드는 1개 이상 100개 이하로 요청할 수 있습니다."));
    }

    @DisplayName("해제 목록에 다른 호스트의 스페이스가 섞이면 전체가 실패하고 기존 지정 상태가 유지된다.")
    @Test
    void unfeatureSpacesWithOtherHostSpace() {
        // given
        Space featured = saveSpaceOf(host, "1111111111");
        feature(featured.getCode());
        Host otherHost = hostRepository.save(createHost());
        Space otherSpace = saveSpaceOf(otherHost, "9999999999");

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(List.of(featured.getCode(), otherSpace.getCode())))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("BAD_REQUEST"))
            .body("message", containsString("유효하지 않은 스페이스 코드입니다."));

        // then
        assertThat(isFeatured(featured)).isTrue();
    }

    @DisplayName("해제 목록에 존재하지 않는 스페이스가 섞이면 전체가 실패하고 기존 지정 상태가 유지된다.")
    @Test
    void unfeatureSpacesWithNotExistingSpace() {
        // given
        Space featured = saveSpaceOf(host, "1111111111");
        feature(featured.getCode());
        String notExistingSpaceCode = "0000000000";

        // when
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(List.of(featured.getCode(), notExistingSpaceCode)))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("BAD_REQUEST"))
            .body("message", containsString("유효하지 않은 스페이스 코드입니다."));

        // then
        assertThat(isFeatured(featured)).isTrue();
    }

    @DisplayName("로그인하지 않으면 축하받는 스페이스를 해제할 수 없다.")
    @Test
    void unfeatureSpacesWithoutLogin() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when & then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(List.of(space.getCode())))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("스페이스 코드 목록이 null이면 축하받는 스페이스를 해제할 수 없다.")
    @Test
    void unfeatureSpacesWithNullSpaceCodes() {
        // given
        List<String> nullSpaceCodes = null;

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(nullSpaceCodes))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("스페이스 코드를 100개 넘게 요청하면 축하받는 스페이스를 해제할 수 없다.")
    @Test
    void unfeatureSpacesWithTooManySpaceCodes() {
        // given
        List<String> tooManySpaceCodes = IntStream.rangeClosed(1, 101)
            .mapToObj(number -> "%010d".formatted(number))
            .toList();

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(tooManySpaceCodes))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("스페이스 코드 목록에 공백이 섞이면 축하받는 스페이스를 해제할 수 없다.")
    @Test
    void unfeatureSpacesWithBlankSpaceCode() {
        // given
        Space space = saveSpaceOf(host, "1111111111");

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(List.of(space.getCode(), " ")))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("해제한 스페이스를 다시 축하받는 스페이스로 지정할 수 있다.")
    @Test
    void featureAfterUnfeaturing() {
        // given
        Space space = saveSpaceOf(host, "1111111111");
        feature(space.getCode());
        unfeature(space.getCode());

        // when
        feature(space.getCode());

        // then
        assertThat(isFeatured(space)).isTrue();
    }

    @DisplayName("해제한 뒤 나의 스페이스 목록을 조회하면 해제한 스페이스만 미지정 상태로 내려온다.")
    @Test
    void getSpacesAfterUnfeaturing() {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        feature(first.getCode(), second.getCode());

        // when
        unfeature(first.getCode());

        // then
        List<HostSpaceItemResponse> spaces = getMySpaces();
        assertAll(
            () -> assertThat(findByCode(spaces, first.getCode()).isFeatured()).isFalse(),
            () -> assertThat(findByCode(spaces, second.getCode()).isFeatured()).isTrue()
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

    private void feature(String... spaceCodes) {
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new FeatureSpacesRequest(List.of(spaceCodes)))
            .when()
            .put("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private void unfeature(String... spaceCodes) {
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(new UnfeatureSpacesRequest(List.of(spaceCodes)))
            .when()
            .delete("/spaces/me/featured")
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    private boolean isFeatured(Space space) {
        return spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(space.getCode()).isFeatured();
    }
}
