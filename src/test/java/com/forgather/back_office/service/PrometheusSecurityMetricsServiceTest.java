package com.forgather.back_office.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.forgather.back_office.model.AttackType;
import com.forgather.back_office.model.SecuritySummary;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class PrometheusSecurityMetricsServiceTest {

    private static final String SCENARIO = "prometheus-queries";

    private WireMockServer wireMock;
    private PrometheusSecurityMetricsService sut;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        RestClient restClient = RestClient.builder()
            .baseUrl("http://localhost:" + wireMock.port())
            .build();
        sut = new PrometheusSecurityMetricsService(restClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @DisplayName("7개 쿼리 모두 성공하면 SecuritySummary를 정상 반환한다")
    @Test
    void getSummary_allQueriesSucceed() {
        // given
        stubAllQueries();

        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.todayBlocked()).isEqualTo(1247);
        assertThat(summary.blockedRatio()).isEqualTo(23.5);
        assertThat(summary.rateLimited()).isEqualTo(3);
        assertThat(summary.newAttackIps()).isEqualTo(18);
        assertThat(summary.unblocked4xx()).isEqualTo(12);
        assertThat(summary.attackTypes()).hasSize(3);
        assertThat(summary.topAttackIps()).hasSize(2);
    }

    @DisplayName("스칼라 쿼리 결과가 비어있으면 0을 반환한다")
    @Test
    void getSummary_emptyScalarResult_returnsZero() {
        // given
        stubSequence(
            scalarBody("0"),          // todayBlocked
            scalarBody("0"),          // blockedRatio
            emptyResultBody(),        // rateLimited
            emptyResultBody(),        // newAttackIps
            emptyResultBody(),        // unblocked4xx
            emptyResultBody(),        // attackTypes
            emptyResultBody()         // topAttackIps
        );

        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.rateLimited()).isZero();
        assertThat(summary.newAttackIps()).isZero();
        assertThat(summary.unblocked4xx()).isZero();
    }

    @DisplayName("중간 쿼리가 실패하면 SecuritySummary.empty()를 반환한다")
    @Test
    void getSummary_queryFails_returnsEmpty() {
        // given — todayBlocked 성공 후 blockedRatio에서 500 에러
        stubSequenceWithFailAt(1,
            scalarBody("1247")
        );

        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary).isEqualTo(SecuritySummary.empty());
    }

    @DisplayName("path_category 라벨을 AttackType Map으로 변환한다")
    @Test
    void getSummary_vectorToAttackTypeMap() {
        // given
        stubAllQueries();

        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.attackTypes())
            .containsEntry(AttackType.CREDENTIAL_SCAN, 774L)
            .containsEntry(AttackType.CMS_SCAN, 312L)
            .containsEntry(AttackType.OTHER, 100L);
    }

    @DisplayName("remote_addr 라벨을 AttackIpInfo 리스트로 변환한다")
    @Test
    void getSummary_vectorToAttackIps() {
        // given
        stubAllQueries();

        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.topAttackIps()).hasSize(2);
        assertThat(summary.topAttackIps().get(0).getIp()).isEqualTo("135.125.133.168");
        assertThat(summary.topAttackIps().get(0).getCount()).isEqualTo(342);
        assertThat(summary.topAttackIps().get(1).getIp()).isEqualTo("209.38.91.12");
        assertThat(summary.topAttackIps().get(1).getCount()).isEqualTo(128);
    }

    @DisplayName("벡터 쿼리가 실패하면 SecuritySummary.empty()를 반환한다")
    @Test
    void getSummary_vectorQueryFails_returnsEmpty() {
        // given — 스칼라 5개 성공 후 attackTypes에서 500 에러
        stubSequenceWithFailAt(5,
            scalarBody("1247"),
            scalarBody("23.5"),
            scalarBody("3"),
            scalarBody("18"),
            scalarBody("12")
        );

        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary).isEqualTo(SecuritySummary.empty());
    }

    @DisplayName("blockedRatio의 double 값을 소수점 1자리로 파싱한다")
    @Test
    void getSummary_doubleParsingPrecision() {
        // given
        stubAllQueriesWithRatio("23.456");

        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary.blockedRatio()).isEqualTo(23.5);
    }

    @DisplayName("double 쿼리가 실패하면 SecuritySummary.empty()를 반환한다")
    @Test
    void getSummary_doubleQueryFails_returnsEmpty() {
        // given — todayBlocked 성공 후 blockedRatio에서 500 에러
        stubSequenceWithFailAt(1,
            scalarBody("1247")
        );

        // when
        SecuritySummary summary = sut.getSummary();

        // then
        assertThat(summary).isEqualTo(SecuritySummary.empty());
    }

    @DisplayName("Prometheus 응답 지연 시 타임아웃으로 SecuritySummary.empty()를 반환한다")
    @Test
    void getSummary_timeout_returnsEmpty() {
        // given
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/query"))
            .willReturn(aResponse()
                .withFixedDelay(10_000)
                .withHeader("Content-Type", "application/json")
                .withBody(scalarBody("1247"))));

        RestClient timeoutClient = RestClient.builder()
            .baseUrl("http://localhost:" + wireMock.port())
            .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                setConnectTimeout(java.time.Duration.ofSeconds(1));
                setReadTimeout(java.time.Duration.ofSeconds(1));
            }})
            .build();
        PrometheusSecurityMetricsService timeoutSut = new PrometheusSecurityMetricsService(timeoutClient);

        // when
        SecuritySummary summary = timeoutSut.getSummary();

        // then
        assertThat(summary).isEqualTo(SecuritySummary.empty());
    }

    @DisplayName("SecuritySummary.empty()와 정상 응답은 서로 다르다")
    @Test
    void emptySummary_isDifferentFromNormalResponse() {
        // given
        stubAllQueries();

        // when
        SecuritySummary summary = sut.getSummary();
        SecuritySummary empty = SecuritySummary.empty();

        // then
        assertThat(summary).isNotEqualTo(empty);
        assertThat(empty.todayBlocked()).isZero();
        assertThat(empty.attackTypes()).isEmpty();
        assertThat(empty.topAttackIps()).isEmpty();
    }

    // --- Scenario 기반 순차 응답 헬퍼 ---

    private void stubAllQueries() {
        stubAllQueriesWithRatio("23.456");
    }

    private void stubAllQueriesWithRatio(String ratio) {
        stubSequence(
            scalarBody("1247.3"),     // todayBlocked
            scalarBody(ratio),         // blockedRatio
            scalarBody("3.1"),         // rateLimited
            scalarBody("18"),          // newAttackIps
            scalarBody("12.4"),        // unblocked4xx
            attackTypesBody(),         // attackTypes
            topAttackIpsBody()         // topAttackIps
        );
    }

    private void stubSequence(String... responseBodies) {
        String[] states = new String[responseBodies.length + 1];
        states[0] = STARTED;
        for (int i = 1; i < states.length; i++) {
            states[i] = "state-" + i;
        }

        for (int i = 0; i < responseBodies.length; i++) {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/query"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(states[i])
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBodies[i]))
                .willSetStateTo(states[i + 1]));
        }
    }

    private void stubSequenceWithFailAt(int failIndex, String... successBodies) {
        String[] states = new String[successBodies.length + 2];
        states[0] = STARTED;
        for (int i = 1; i < states.length; i++) {
            states[i] = "state-" + i;
        }

        for (int i = 0; i < successBodies.length; i++) {
            wireMock.stubFor(get(urlPathEqualTo("/api/v1/query"))
                .inScenario(SCENARIO)
                .whenScenarioStateIs(states[i])
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(successBodies[i]))
                .willSetStateTo(states[i + 1]));
        }

        // failIndex 번째에서 500 에러
        wireMock.stubFor(get(urlPathEqualTo("/api/v1/query"))
            .inScenario(SCENARIO)
            .whenScenarioStateIs(states[failIndex])
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo(states[failIndex + 1]));
    }

    // --- JSON 응답 바디 ---

    private String scalarBody(String value) {
        return """
            {
                "status": "success",
                "data": {
                    "resultType": "vector",
                    "result": [
                        {
                            "metric": {},
                            "value": [1708905600, "%s"]
                        }
                    ]
                }
            }
            """.formatted(value);
    }

    private String emptyResultBody() {
        return """
            {
                "status": "success",
                "data": {
                    "resultType": "vector",
                    "result": []
                }
            }
            """;
    }

    private String attackTypesBody() {
        return """
            {
                "status": "success",
                "data": {
                    "resultType": "vector",
                    "result": [
                        {"metric": {"path_category": "credential_scan"}, "value": [1708905600, "774.2"]},
                        {"metric": {"path_category": "cms_scan"}, "value": [1708905600, "312.1"]},
                        {"metric": {"path_category": "other"}, "value": [1708905600, "100.4"]}
                    ]
                }
            }
            """;
    }

    private String topAttackIpsBody() {
        return """
            {
                "status": "success",
                "data": {
                    "resultType": "vector",
                    "result": [
                        {"metric": {"remote_addr": "135.125.133.168"}, "value": [1708905600, "342.1"]},
                        {"metric": {"remote_addr": "209.38.91.12"}, "value": [1708905600, "128.3"]}
                    ]
                }
            }
            """;
    }
}
