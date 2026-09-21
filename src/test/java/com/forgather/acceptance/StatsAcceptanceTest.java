package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.stats.dto.LandingStatsResponse;
import com.forgather.fixture.GuestBookCardFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
class StatsAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("랜딩 페이지용 통계로 스페이스와 방명록 카드의 총 개수를 조회한다")
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

        // when
        ApiResponse<LandingStatsResponse> result = RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/stats/landing")
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
            () -> assertThat(result.data().spaceStats().spaceCount()).isEqualTo(2),
            () -> assertThat(result.data().guestBookStats().cardCount()).isEqualTo(5)
        );
    }
}
