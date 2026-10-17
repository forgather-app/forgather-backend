package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.stats.dto.LandingStatsResponse;
import com.forgather.fixture.GuestBookCardFixture;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.ProductFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;

@AutoConfigureMockMvc
class StatsAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("랜딩 통계로 스페이스, 방명록 카드, 회원, 작품의 총 개수를 조회한다")
    @Test
    void landing() {
        // given
        Space space1 = spaceRepository.save(SpaceFixture.createSpace());
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("0123456789"));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpace(space1));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpace(space1));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpace(space2));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpace(space2));
        guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpace(space2));

        hostRepository.save(HostFixture.createHost());
        hostRepository.save(HostFixture.createHost());
        productRepository.save(ProductFixture.createProductWithSpace(space1));
        productRepository.save(ProductFixture.createProductWithSpace(space1));
        productRepository.save(ProductFixture.createProductWithSpace(space2));

        // when
        ApiResponse<LandingStatsResponse> result = RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/stats/landing")
            .then()
            .statusCode(200)
            .body("data.hostStats.hostCount", equalTo(2))
            .body("data.productStats.productCount", equalTo(3))
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(result.message()).isNull(),
            () -> assertThat(result.data().spaceStats().spaceCount()).isEqualTo(2),
            () -> assertThat(result.data().guestBookStats().cardCount()).isEqualTo(5)
        );
    }

    @DisplayName("데이터가 없으면 랜딩 통계의 모든 개수가 0이다")
    @Test
    void landingWithoutData() {
        // given
        // when
        JsonPath result = getLandingStats();

        // then
        assertCounts(result, 0, 0, 0, 0);
    }

    @DisplayName("삭제된 스페이스, 방명록 카드, 작품도 랜딩 통계에 포함한다")
    @Test
    void landingIncludesDeletedData() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product product = productRepository.save(ProductFixture.createProductWithSpace(space));
        GuestBookCard card = guestBookCardRepository.save(GuestBookCardFixture.createGuestBookCardWithSpace(space));
        product.delete();
        productRepository.save(product);
        card.delete();
        guestBookCardRepository.save(card);
        space.delete();
        spaceRepository.save(space);

        // when
        JsonPath result = getLandingStats();

        // then
        assertCounts(result, 1, 1, 0, 1);
    }

    @DisplayName("탈퇴 회원과 익명화된 탈퇴 회원도 랜딩 회원 수에 포함한다")
    @Test
    void landingIncludesWithdrawnAndAnonymizedHosts() {
        // given
        hostRepository.save(HostFixture.createHost());
        Host withdrawn = HostFixture.createHost();
        withdrawn.delete();
        hostRepository.save(withdrawn);
        Host anonymized = HostFixture.createHost();
        anonymized.delete();
        anonymized.anonymize();
        hostRepository.save(anonymized);

        // when
        JsonPath result = getLandingStats();

        // then
        assertCounts(result, 0, 0, 3, 0);
    }

    @DisplayName("방명록이 비공개여도 작품을 랜딩 통계에 포함한다")
    @Test
    void landingIncludesProductsWithPrivateGuestBook() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createPrivateSpace());
        productRepository.save(ProductFixture.createProductWithSpace(space));

        // when
        JsonPath result = getLandingStats();

        // then
        assertCounts(result, 1, 0, 0, 1);
    }

    private JsonPath getLandingStats() {
        return RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/stats/landing")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath();
    }

    private void assertCounts(JsonPath result, long spaceCount, long cardCount, long hostCount, long productCount) {
        assertAll(
            () -> assertThat(result.getObject("data.spaceStats.spaceCount", Long.class)).isEqualTo(spaceCount),
            () -> assertThat(result.getObject("data.guestBookStats.cardCount", Long.class)).isEqualTo(cardCount),
            () -> assertThat(result.getObject("data.hostStats.hostCount", Long.class)).isEqualTo(hostCount),
            () -> assertThat(result.getObject("data.productStats.productCount", Long.class)).isEqualTo(productCount)
        );
    }
}
