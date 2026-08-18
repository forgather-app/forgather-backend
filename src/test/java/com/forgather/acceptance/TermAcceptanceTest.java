package com.forgather.acceptance;

import static com.forgather.domain.term.model.HostTermHistoryAction.AGREE;
import static com.forgather.fixture.TermFixture.createMarketingTerm;
import static com.forgather.fixture.TermFixture.createPrivacyTerm;
import static com.forgather.fixture.TermFixture.createServiceTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.term.dto.TermResponse;
import com.forgather.domain.term.model.HostTermHistory;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.repository.HostTermHistoryRepository;
import com.forgather.domain.term.repository.jpa.TermJpaRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.response.ExtractableResponse;

@AutoConfigureMockMvc
class TermAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermJpaRepository jpaRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private HostTermHistoryRepository hostTermHistoryRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @DisplayName("비로그인 상태로 타입별 최신 약관을 노출 순서대로 조회한다")
    @Test
    void getLatestTerms() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        jpaRepository.save(createServiceTerm("0.9.0", "## old service"));
        Term latestServiceTerm = jpaRepository.save(createServiceTerm("1.0.0", "## latest service"));
        jpaRepository.save(createPrivacyTerm("0.9.0", "## old privacy"));
        Term latestPrivacyTerm = jpaRepository.save(createPrivacyTerm("1.0.0", "## latest privacy"));
        Term latestMarketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## latest active marketing"));
        Term deletedMarketingTerm = jpaRepository.save(createMarketingTerm("1.1.0", "## deleted marketing"));
        deletedMarketingTerm.delete();
        jpaRepository.save(deletedMarketingTerm);

        // when
        ApiResponse<List<TermResponse>> result = RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/terms")
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
            () -> assertThat(result.data()).hasSize(3),
            () -> assertThat(result.data()).extracting(TermResponse::id)
                .containsExactly(latestServiceTerm.getId(), latestPrivacyTerm.getId(), latestMarketingTerm.getId()),
            () -> assertThat(result.data()).extracting(TermResponse::type)
                .containsExactly("SERVICE", "PRIVACY", "MARKETING"),
            () -> assertThat(result.data()).extracting(TermResponse::content)
                .containsExactly("## latest service", "## latest privacy", "## latest active marketing"),
            () -> assertThat(result.data()).extracting(TermResponse::isRequired)
                .containsExactly(true, true, false)
        );
    }

    @DisplayName("로그인 상태로 내 약관 동의 현황을 노출 순서대로 조회한다")
    @Test
    void getMyTermAgreements() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term oldServiceTerm = jpaRepository.save(createServiceTerm("1.0.0", "## old service"));
        jpaRepository.save(createServiceTerm("2.0.0", "2.0.0", "## latest service"));
        Term privacyTerm = jpaRepository.save(createPrivacyTerm("1.0.0", "## privacy"));
        jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));
        hostTermHistoryRepository.saveAll(List.of(
            new HostTermHistory(host, oldServiceTerm, AGREE),
            new HostTermHistory(host, privacyTerm, AGREE)
        ));

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .get("/terms/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(response.jsonPath().getList("data")).hasSize(3),
            () -> assertThat(response.jsonPath().getList("data.type"))
                .containsExactly("SERVICE", "PRIVACY", "MARKETING"),
            () -> assertThat(response.jsonPath().getString("data[0].version")).isEqualTo("2.0.0"),
            () -> assertThat(response.jsonPath().getString("data[0].content")).isEqualTo("## latest service"),
            () -> assertThat(response.jsonPath().getBoolean("data[0].isAgreed")).isFalse(),
            () -> assertThat(response.jsonPath().getString("data[0].agreedAt")).isNull(),
            () -> assertThat(response.jsonPath().getBoolean("data[0].isReagreementRequired")).isTrue(),
            () -> assertThat(response.jsonPath().getBoolean("data[1].isAgreed")).isTrue(),
            () -> assertThat(response.jsonPath().getString("data[1].agreedAt")).isNotBlank(),
            () -> assertThat(response.jsonPath().getBoolean("data[1].isReagreementRequired")).isFalse(),
            () -> assertThat(response.jsonPath().getBoolean("data[2].isRequired")).isFalse(),
            () -> assertThat(response.jsonPath().getBoolean("data[2].isAgreed")).isFalse(),
            () -> assertThat(response.jsonPath().getBoolean("data[2].isReagreementRequired")).isFalse()
        );
    }

    @DisplayName("비로그인 상태로 내 약관 동의 현황을 조회하면 401을 반환한다")
    @Test
    void getMyTermAgreementsWithoutLogin() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);

        // when & then
        RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/terms/me")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
