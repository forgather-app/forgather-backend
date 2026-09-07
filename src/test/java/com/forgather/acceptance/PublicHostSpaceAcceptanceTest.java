package com.forgather.acceptance;

import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.dto.PublicHostSpaceItemResponse;
import com.forgather.domain.space.dto.PublicHostSpacesResponse;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.GuestBookCardFixture;
import com.forgather.fixture.ProductFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.SpaceHostFixture;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@DisplayName("인수 테스트: 호스트 스페이스 목록")
@AutoConfigureMockMvc
class PublicHostSpaceAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Host host;

    @BeforeEach
    void setUpMockMvc() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        host = hostRepository.save(createHost());
    }

    @DisplayName("비로그인 방문자가 호스트 코드로 스페이스 목록을 최신 생성 순으로 조회한다.")
    @Test
    void getPublicHostSpaces() throws InterruptedException {
        // given
        Space first = saveSpaceOf(host, "1111111111");
        Product product = productRepository.save(ProductFixture.createProductWithSpace(first));
        ProductPhoto firstPhoto = saveProductPhoto(product, 1);
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(first, "nickname", "방명록"));
        Thread.sleep(1000);
        Space second = saveSpaceOf(host, "2222222222");

        // when
        List<PublicHostSpaceItemResponse> spaces = getSpacesWithoutLogin(host.getCode());

        // then
        assertAll(
            () -> assertThat(spaces).hasSize(2),
            () -> assertThat(spaces.getFirst().spaceCode()).isEqualTo(second.getCode()),
            // second에는 작품이 없으므로 사진이 없다.
            () -> assertThat(spaces.getFirst().spacePhotoPath()).isNull(),
            () -> assertThat(spaces.getFirst().guestBookCardCount()).isZero(),

            () -> assertThat(spaces.getLast().spaceCode()).isEqualTo(first.getCode()),
            () -> assertThat(spaces.getLast().name()).isEqualTo(first.getName()),
            () -> assertThat(spaces.getLast().spacePhotoPath()).isEqualTo(firstPhoto.getPath()),
            () -> assertThat(spaces.getLast().guestBookCardCount()).isOne(),
            () -> assertThat(spaces.getLast().isPublic()).isTrue()
        );
    }

    @DisplayName("스페이스가 없는 호스트는 빈 목록을 응답한다.")
    @Test
    void getPublicHostSpacesWithoutSpaces() {
        // when
        List<PublicHostSpaceItemResponse> spaces = getSpacesWithoutLogin(host.getCode());

        // then
        assertThat(spaces).isEmpty();
    }

    @DisplayName("존재하지 않는 호스트 코드로 조회하면 찾을 수 없다.")
    @Test
    void getPublicHostSpacesWithUnknownCode() {
        // when & then
        RestAssuredMockMvc.given()
            .when()
            .get("/hosts/{hostCode}/spaces", "zzzzzzzzzz")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("code", equalTo(ResponseCode.NOT_FOUND.name()));
    }

    @DisplayName("탈퇴한 호스트의 스페이스 목록은 조회할 수 없다.")
    @Test
    void getPublicHostSpacesOfWithdrawnHost() {
        // given
        saveSpaceOf(host, "1111111111");
        host.delete();
        hostRepository.save(host);

        // when & then
        RestAssuredMockMvc.given()
            .when()
            .get("/hosts/{hostCode}/spaces", host.getCode())
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("code", equalTo(ResponseCode.NOT_FOUND.name()));
    }

    @DisplayName("비로그인으로 조회하면 비공개 스페이스도 목록에 포함하되 방명록 개수는 null(개수 비공개)로 응답한다.")
    @Test
    void getPublicHostSpacesMasksPrivateSpaceCount() {
        // given
        Space publicSpace = saveSpaceOf(host, "1111111111");
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(publicSpace, "nickname", "방명록"));
        Space privateSpace = spaceRepository.save(SpaceFixture.createPrivateSpace());
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(privateSpace, host));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(privateSpace, "nickname1", "방명록1"));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(privateSpace, "nickname2", "방명록2"));

        // when
        List<PublicHostSpaceItemResponse> spaces = getSpacesWithoutLogin(host.getCode());

        // then
        assertAll(
            () -> assertThat(spaces).hasSize(2),
            () -> assertThat(findByCode(spaces, privateSpace.getCode()).isPublic()).isFalse(),
            () -> assertThat(findByCode(spaces, privateSpace.getCode()).guestBookCardCount()).isNull(),
            () -> assertThat(findByCode(spaces, publicSpace.getCode()).guestBookCardCount()).isOne()
        );
    }

    @DisplayName("호스트 본인이 조회하면 비공개 스페이스도 방명록 개수를 실제 값으로 응답한다.")
    @Test
    void getPublicHostSpacesWithOwnerToken() {
        // given
        Space privateSpace = spaceRepository.save(SpaceFixture.createPrivateSpace());
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(privateSpace, host));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(privateSpace, "nickname1", "방명록1"));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(privateSpace, "nickname2", "방명록2"));
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        List<PublicHostSpaceItemResponse> spaces = getSpacesWithToken(host.getCode(), token);

        // then
        assertThat(findByCode(spaces, privateSpace.getCode()).guestBookCardCount()).isEqualTo(2);
    }

    @DisplayName("호스트가 아닌 사용자가 조회하면 비공개 스페이스의 방명록 개수를 null(개수 비공개)로 응답한다.")
    @Test
    void getPublicHostSpacesWithOtherHostToken() {
        // given
        Space privateSpace = spaceRepository.save(SpaceFixture.createPrivateSpace());
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(privateSpace, host));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCard(privateSpace, "nickname", "방명록"));
        Host otherHost = hostRepository.save(createHost());
        String otherToken = jwtTokenProvider.generateAccessToken(otherHost.getId());

        // when
        List<PublicHostSpaceItemResponse> spaces = getSpacesWithToken(host.getCode(), otherToken);

        // then
        assertThat(findByCode(spaces, privateSpace.getCode()).guestBookCardCount()).isNull();
    }

    @DisplayName("논리 삭제된 스페이스는 목록에서 제외한다.")
    @Test
    void getPublicHostSpacesExcludesDeletedSpace() {
        // given
        Space remaining = saveSpaceOf(host, "1111111111");
        Space deleted = saveSpaceOf(host, "2222222222");
        deleted.delete();
        spaceRepository.save(deleted);

        // when
        List<PublicHostSpaceItemResponse> spaces = getSpacesWithoutLogin(host.getCode());

        // then
        assertAll(
            () -> assertThat(spaces).hasSize(1),
            () -> assertThat(spaces.getFirst().spaceCode()).isEqualTo(remaining.getCode())
        );
    }

    private Space saveSpaceOf(Host owner, String spaceCode) {
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode(spaceCode));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space, owner));
        return space;
    }

    private ProductPhoto saveProductPhoto(Product product, int sortOrder) {
        return productPhotoRepository.save(
            new ProductPhoto(product, "original.webp", "path/%d-%d.webp".formatted(product.getId(), sortOrder),
                1024L, sortOrder)
        );
    }

    private List<PublicHostSpaceItemResponse> getSpacesWithoutLogin(String hostCode) {
        ApiResponse<PublicHostSpacesResponse> response = RestAssuredMockMvc.given()
            .when()
            .get("/hosts/{hostCode}/spaces", hostCode)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        return response.data().spaces();
    }

    private List<PublicHostSpaceItemResponse> getSpacesWithToken(String hostCode, String accessToken) {
        ApiResponse<PublicHostSpacesResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + accessToken)
            .when()
            .get("/hosts/{hostCode}/spaces", hostCode)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        return response.data().spaces();
    }

    private PublicHostSpaceItemResponse findByCode(List<PublicHostSpaceItemResponse> spaces, String spaceCode) {
        return spaces.stream()
            .filter(space -> space.spaceCode().equals(spaceCode))
            .findFirst()
            .orElseThrow(() -> new AssertionError("스페이스를 찾을 수 없습니다. spaceCode: " + spaceCode));
    }
}
