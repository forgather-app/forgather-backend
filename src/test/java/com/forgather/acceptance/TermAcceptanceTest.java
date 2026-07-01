package com.forgather.acceptance;

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
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.term.dto.TermResponse;
import com.forgather.domain.term.model.Term;
import com.forgather.domain.term.repository.jpa.TermJpaRepository;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
class TermAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TermJpaRepository jpaRepository;

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
}
