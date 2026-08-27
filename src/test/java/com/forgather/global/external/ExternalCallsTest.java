package com.forgather.global.external;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;

class ExternalCallsTest {

    private static final ExternalOperation OPERATION = ExternalOperation.KAKAO_UNLINK;

    private WireMockServer wireMock;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(500));
        requestFactory.setReadTimeout(Duration.ofMillis(500));
        restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @DisplayName("정상 응답은 그대로 반환한다")
    @Test
    void success() {
        // given
        stub(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody("{\"ok\":true}"));

        // when
        Map<String, Object> body = ExternalCalls.execute(OPERATION, () -> call(Map.class));

        // then
        assertThat(body).containsEntry("ok", true);
    }

    @DisplayName("외부 5xx는 UPSTREAM_ERROR로 분류한다")
    @Test
    void serverError() {
        // given
        stub(aResponse().withStatus(503));

        // when & then
        assertThatThrownBy(() -> ExternalCalls.execute(OPERATION, () -> call(Map.class)))
            .isInstanceOf(ExternalApiException.class)
            .extracting("type")
            .isEqualTo(FailureType.UPSTREAM_ERROR);
    }

    @DisplayName("외부 4xx는 CALLER_ERROR로 분류하고 본문을 보관한다")
    @Test
    void clientError() {
        // given
        stub(aResponse()
            .withStatus(400)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody("{\"code\":-101}"));

        // when & then
        assertThatThrownBy(() -> ExternalCalls.execute(OPERATION, () -> call(Map.class)))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.CALLER_ERROR);
                assertThat(exception.getResponseBody()).contains("-101");
            });
    }

    @DisplayName("429는 RATE_LIMITED로 분류한다")
    @Test
    void rateLimited() {
        // given
        stub(aResponse().withStatus(429));

        // when & then
        assertThatThrownBy(() -> ExternalCalls.execute(OPERATION, () -> call(Map.class)))
            .isInstanceOf(ExternalApiException.class)
            .extracting("type")
            .isEqualTo(FailureType.RATE_LIMITED);
    }

    @DisplayName("응답이 지연되면 READ_TIMEOUT으로 분류한다")
    @Test
    void readTimeout() {
        // given
        stub(aResponse()
            .withFixedDelay(2_000)
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody("{\"ok\":true}"));

        // when & then
        assertThatThrownBy(() -> ExternalCalls.execute(OPERATION, () -> call(Map.class)))
            .isInstanceOf(ExternalApiException.class)
            .extracting("type")
            .isEqualTo(FailureType.READ_TIMEOUT);
    }

    @DisplayName("연결이 끊기면 CONNECTION_FAILED로 분류한다")
    @Test
    void connectionFailed() {
        // given
        stub(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER));

        // when & then
        assertThatThrownBy(() -> ExternalCalls.execute(OPERATION, () -> call(Map.class)))
            .isInstanceOf(ExternalApiException.class)
            .extracting("type")
            .isEqualTo(FailureType.CONNECTION_FAILED);
    }

    private void stub(ResponseDefinitionBuilder response) {
        wireMock.stubFor(get(urlEqualTo("/probe")).willReturn(response));
    }

    private <T> T call(Class<T> type) {
        return restClient.get()
            .uri(wireMock.baseUrl() + "/probe")
            .retrieve()
            .body(type);
    }
}
