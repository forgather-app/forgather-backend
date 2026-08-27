package com.forgather.global.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class RestClientConfigTest {

    private WireMockServer wireMock;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @DisplayName("자동구성 빌더를 주입받아도 read timeout 설정이 그대로 적용된다")
    @Test
    void readTimeoutIsApplied() {
        // given — 자동구성 빌더 대신 동일한 초기 상태의 빌더를 넘겨 설정 적용만 검증한다
        RestClient restClient = new RestClientConfig().restClient(RestClient.builder());
        wireMock.stubFor(get(urlEqualTo("/slow"))
            .willReturn(aResponse().withFixedDelay(7_000).withBody("{}")));

        // when & then — READ_TIMEOUT 5초를 넘기므로 IO 예외가 발생해야 한다
        assertThatThrownBy(() -> restClient.get()
            .uri(wireMock.baseUrl() + "/slow")
            .retrieve()
            .toBodilessEntity())
            .isInstanceOf(ResourceAccessException.class);
    }
}
