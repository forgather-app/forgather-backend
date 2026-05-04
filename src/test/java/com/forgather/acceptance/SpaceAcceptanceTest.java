package com.forgather.acceptance;

import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.dto.SpaceResponse;
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
import com.forgather.fixture.SpacePhotoFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHostMap;
import com.forgather.global.auth.repository.SpaceHostMapRepository;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
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
    private SpaceHostMapRepository spaceHostMapRepository;

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
                "forgather@forgather.me")
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
                "forgather@forgather.me")
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
                "forgather@forgather.me")
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

    @DisplayName("스페이스를 상세 조회한다.")
    @Test
    void getSpaceInformation() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        SpacePhoto spacePhoto = spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostMapRepository.save(new SpaceHostMap(space, host));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(space, "nickname1", "카드1"));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(space, "nickname2", "카드2"));

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
        spaceHostMapRepository.save(new SpaceHostMap(space, host));

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
        spaceHostMapRepository.save(new SpaceHostMap(space, host));
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
            () -> assertThat(guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(guestBookCard)).isEmpty()
        );
    }

    @DisplayName("로그인 없이 스페이스를 삭제할 수 없다.")
    @Test
    void deleteSpaceWithoutLogin() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostMapRepository.save(new SpaceHostMap(space, host));

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
        spaceHostMapRepository.save(new SpaceHostMap(space, host));
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
        spaceHostMapRepository.save(new SpaceHostMap(space, host));

        MockMultipartFile newFile = new MockMultipartFile(
            "file",
            "new.jpg",
            "image/jpeg",
            "new image content".getBytes()
        );
        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", "새로운 설명", false, "forgather_official_new", "forgather_new@forgather.me", true)
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
            () -> assertThat(spacePhotoRepository.getBySpaceAndDeletedAtIsNullOrEmpty(space).getOriginalName()).isEqualTo("new.jpg"),
            () -> assertThat(result.data().guestBookCardCount()).isZero()
        );
    }

    @DisplayName("스페이스 이름만 수정한다.")
    @Test
    void updateOnlySpaceName() throws Exception {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spacePhotoRepository.save(SpacePhotoFixture.createSpacePhotoWithSpace(space));
        spaceHostMapRepository.save(new SpaceHostMap(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", null, null, null, null, false)
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
        spaceHostMapRepository.save(new SpaceHostMap(space, host));

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", null, null, null, null, false)
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
        spaceHostMapRepository.save(new SpaceHostMap(space, host));
        Host otherHost = hostRepository.save(createHost());
        String otherToken = jwtTokenProvider.generateAccessToken(otherHost.getId());

        String request = objectMapper.writeValueAsString(new UpdateSpaceRequest(
            "새로운 스페이스", null, null, null, null, false)
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
        spaceHostMapRepository.save(new SpaceHostMap(space1, host));
        spaceHostMapRepository.save(new SpaceHostMap(space2, host));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(space1, "nickname", "방명록1"));

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
}
