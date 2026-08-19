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
import com.forgather.domain.term.model.TermType;
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

    @DisplayName("미동의 선택 약관에 동의하면 동의 상태로 갱신된 약관을 반환한다")
    @Test
    void agreeTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = createOnboardedHost();
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(response.jsonPath().getLong("data.id")).isEqualTo(marketingTerm.getId()),
            () -> assertThat(response.jsonPath().getString("data.type")).isEqualTo("MARKETING"),
            () -> assertThat(response.jsonPath().getBoolean("data.isAgreed")).isTrue(),
            () -> assertThat(response.jsonPath().getString("data.agreedAt")).isNotBlank(),
            () -> assertThat(response.jsonPath().getBoolean("data.isReagreementRequired")).isFalse()
        );
    }

    @DisplayName("개정으로 무효화된 필수 약관에 재동의하면 동의 상태로 갱신된다")
    @Test
    void reagreeTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = createOnboardedHost();
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term oldServiceTerm = jpaRepository.save(createServiceTerm("1.0.0", "## old service"));
        Term latestServiceTerm = jpaRepository.save(createServiceTerm("2.0.0", "2.0.0", "## latest service"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, oldServiceTerm, AGREE)));

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", latestServiceTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getString("data.version")).isEqualTo("2.0.0"),
            () -> assertThat(response.jsonPath().getBoolean("data.isAgreed")).isTrue(),
            () -> assertThat(response.jsonPath().getBoolean("data.isReagreementRequired")).isFalse()
        );
    }

    @DisplayName("이미 동의한 약관에 다시 동의해도 이력이 늘지 않고 동의 상태를 유지한다")
    @Test
    void agreeTermTwice() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = createOnboardedHost();
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));
        HostTermHistory agreed = new HostTermHistory(host, marketingTerm, AGREE);
        hostTermHistoryRepository.saveAll(List.of(agreed));

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getBoolean("data.isAgreed")).isTrue(),
            () -> assertThat(hostTermHistoryRepository.findLastHistoryByHostAndType(host, TermType.MARKETING))
                .map(HostTermHistory::getId)
                .contains(agreed.getId())
        );
    }

    @DisplayName("동의한 선택 약관을 철회하면 미동의 상태로 갱신된 약관을 반환한다")
    @Test
    void withdrawTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = createOnboardedHost();
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, marketingTerm, AGREE)));

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getBoolean("data.isAgreed")).isFalse(),
            () -> assertThat(response.jsonPath().getString("data.agreedAt")).isNull(),
            () -> assertThat(response.jsonPath().getBoolean("data.isReagreementRequired")).isFalse()
        );
    }

    @DisplayName("동의하지 않은 선택 약관을 철회해도 성공한다")
    @Test
    void withdrawNotAgreedTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = createOnboardedHost();
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getBoolean("data.isAgreed")).isFalse(),
            () -> assertThat(hostTermHistoryRepository.findLastHistoryByHostAndType(host, TermType.MARKETING))
                .isEmpty()
        );
    }

    @DisplayName("필수 약관을 철회하면 400을 반환한다")
    @Test
    void withdrawRequiredTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term serviceTerm = jpaRepository.save(createServiceTerm("1.0.0", "## service"));

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", serviceTerm.getId())
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("최신이 아닌 약관에 동의하면 400을 반환한다")
    @Test
    void agreeOutdatedTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term oldServiceTerm = jpaRepository.save(createServiceTerm("1.0.0", "## old service"));
        jpaRepository.save(createServiceTerm("2.0.0", "2.0.0", "## latest service"));

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", oldServiceTerm.getId())
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("최신이 아닌 약관을 철회하면 400을 반환한다")
    @Test
    void withdrawOutdatedTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term oldMarketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## old marketing"));
        jpaRepository.save(createMarketingTerm("2.0.0", "2.0.0", "## latest marketing"));

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", oldMarketingTerm.getId())
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("존재하지 않는 약관에 동의하면 404를 반환한다")
    @Test
    void agreeNotFoundTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", 999L)
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @DisplayName("삭제된 약관을 철회하면 404를 반환한다")
    @Test
    void withdrawDeletedTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term deletedTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## deleted marketing"));
        deletedTerm.delete();
        jpaRepository.save(deletedTerm);

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", deletedTerm.getId())
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @DisplayName("비로그인 상태로 약관에 동의하면 401을 반환한다")
    @Test
    void agreeTermWithoutLogin() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));

        // when & then
        RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("선택 약관을 철회한 뒤 다시 동의하면 동의 상태로 돌아간다")
    @Test
    void agreeAfterWithdraw() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = createOnboardedHost();
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, marketingTerm, AGREE)));
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getBoolean("data.isAgreed")).isTrue(),
            () -> assertThat(response.jsonPath().getString("data.agreedAt")).isNotBlank(),
            () -> assertThat(response.jsonPath().getBoolean("data.isReagreementRequired")).isFalse()
        );
    }

    @DisplayName("개정으로 무효화된 선택 약관을 철회하면 재동의를 요구하지 않는다")
    @Test
    void withdrawInvalidatedOptionalTerm() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = createOnboardedHost();
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term oldMarketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## old marketing"));
        Term latestMarketingTerm = jpaRepository.save(createMarketingTerm("2.0.0", "2.0.0", "## latest marketing"));
        HostTermHistory agreed = new HostTermHistory(host, oldMarketingTerm, AGREE);
        hostTermHistoryRepository.saveAll(List.of(agreed));

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", latestMarketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getBoolean("data.isAgreed")).isFalse(),
            () -> assertThat(response.jsonPath().getBoolean("data.isReagreementRequired")).isFalse(),
            () -> assertThat(hostTermHistoryRepository.findLatestHistoriesPerTypeByHost(host))
                .extracting(HostTermHistory::getId)
                .doesNotContain(agreed.getId())
        );
    }

    @DisplayName("약관 동의를 토글하면 내 약관 동의 현황 조회에 반영된다")
    @Test
    void getMyTermAgreementsAfterToggle() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = createOnboardedHost();
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term serviceTerm = jpaRepository.save(createServiceTerm("2.0.0", "2.0.0", "## revised service"));
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", serviceTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value());
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value());
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.OK.value());

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
            () -> assertThat(response.jsonPath().getList("data.type"))
                .containsExactly("SERVICE", "PRIVACY", "MARKETING"),
            () -> assertThat(response.jsonPath().getString("data[0].version")).isEqualTo("2.0.0"),
            () -> assertThat(response.jsonPath().getBoolean("data[0].isAgreed")).isTrue(),
            () -> assertThat(response.jsonPath().getString("data[0].agreedAt")).isNotBlank(),
            () -> assertThat(response.jsonPath().getBoolean("data[0].isReagreementRequired")).isFalse(),
            () -> assertThat(response.jsonPath().getBoolean("data[2].isAgreed")).isFalse(),
            () -> assertThat(response.jsonPath().getBoolean("data[2].isReagreementRequired")).isFalse()
        );
    }

    @DisplayName("비로그인 상태로 약관 동의를 철회하면 401을 반환한다")
    @Test
    void withdrawTermWithoutLogin() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));

        // when & then
        RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("온보딩을 마치지 않은 호스트가 약관에 동의하면 400을 반환한다")
    @Test
    void agreeTermWithoutOnboarding() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .post("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("온보딩을 마치지 않은 호스트가 약관 동의를 철회하면 400을 반환한다")
    @Test
    void withdrawTermWithoutOnboarding() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        Term marketingTerm = jpaRepository.save(createMarketingTerm("1.0.0", "## marketing"));
        hostTermHistoryRepository.saveAll(List.of(new HostTermHistory(host, marketingTerm, AGREE)));

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/terms/{termId}/agreement", marketingTerm.getId())
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 토글 API는 온보딩을 마친 호스트만 사용할 수 있으므로 필수 약관 동의 이력까지 만들어 둔다.
     * HostFixture.createHost()는 닉네임만 채워 주고 약관 동의 이력은 남기지 않는다.
     */
    private Host createOnboardedHost() {
        Host host = hostRepository.save(HostFixture.createHost());
        Term serviceTerm = jpaRepository.save(createServiceTerm("1.0.0", "## onboarding service"));
        Term privacyTerm = jpaRepository.save(createPrivacyTerm("1.0.0", "## onboarding privacy"));
        hostTermHistoryRepository.saveAll(List.of(
            new HostTermHistory(host, serviceTerm, AGREE),
            new HostTermHistory(host, privacyTerm, AGREE)
        ));
        return host;
    }
}
