package com.forgather.global.external.social;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.config.KakaoProperties;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.ExternalApiException;
import com.forgather.global.external.FailureType;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;

class KakaoApiClientTest {

    private WireMockServer wireMock;
    private KakaoApiClient kakaoApiClient;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        kakaoApiClient = new KakaoApiClient(RestClient.create(), kakaoProperties(), new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @DisplayName("Kakao unlink는 admin key와 target_id를 form으로 전달한다")
    @Test
    void unlink() {
        // given
        wireMock.stubFor(post(urlEqualTo("/v1/user/unlink"))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"id":12345}
                    """)));

        // when & then
        assertThatCode(() -> kakaoApiClient.unlink("12345")).doesNotThrowAnyException();
        wireMock.verify(postRequestedFor(urlEqualTo("/v1/user/unlink"))
            .withHeader(HttpHeaders.AUTHORIZATION, containing("KakaoAK test-admin-key"))
            .withRequestBody(containing("target_id=12345")));
    }

    @DisplayName("Kakao가 4xx를 반환하면 우리 쪽 오류로 500을 던진다")
    @Test
    void unlinkWithClientError() {
        // given
        wireMock.stubFor(post(urlEqualTo("/v1/user/unlink"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"code":-401,"msg":"invalid admin key"}
                    """)));

        // when & then
        assertThatThrownBy(() -> kakaoApiClient.unlink("12345"))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.CALLER_ERROR);
                assertThat(exception.getStatusCode()).isEqualTo(500);
            });
    }

    @DisplayName("이미 연결이 끊긴 사용자(-101)는 목적이 달성된 것이므로 예외 없이 종료한다")
    @Test
    void unlinkAlreadyUnlinked() {
        // given
        wireMock.stubFor(post(urlEqualTo("/v1/user/unlink"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"msg":"not exist kakao account linked","code":-101}
                    """)));

        // when & then
        assertThatCode(() -> kakaoApiClient.unlink("12345")).doesNotThrowAnyException();
    }

    @DisplayName("-101이 아닌 4xx는 우리 쪽 오류로 분류한다")
    @Test
    void unlinkWithOtherClientError() {
        // given
        wireMock.stubFor(post(urlEqualTo("/v1/user/unlink"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"msg":"bad request","code":-2}
                    """)));

        // when & then
        assertThatThrownBy(() -> kakaoApiClient.unlink("12345"))
            .isInstanceOf(ExternalApiException.class)
            .extracting("type")
            .isEqualTo(FailureType.CALLER_ERROR);
    }

    @DisplayName("Kakao가 5xx를 반환하면 외부 서비스 장애 예외를 던진다")
    @Test
    void unlinkWithServerError() {
        // given
        wireMock.stubFor(post(urlEqualTo("/v1/user/unlink"))
            .willReturn(aResponse().withStatus(502)));

        // when & then
        assertThatThrownBy(() -> kakaoApiClient.unlink("12345"))
            .isInstanceOf(ExternalApiException.class)
            .extracting("statusCode")
            .isEqualTo(503);
    }

    @DisplayName("Kakao 서버 연결에 실패하면 외부 서비스 장애 예외를 던진다")
    @Test
    void unlinkWithConnectionFailure() {
        // given
        wireMock.stubFor(post(urlEqualTo("/v1/user/unlink"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        // when & then
        assertThatThrownBy(() -> kakaoApiClient.unlink("12345"))
            .isInstanceOf(ExternalApiException.class)
            .extracting("statusCode")
            .isEqualTo(503);
    }

    @DisplayName("user id가 비어 있으면 외부 호출 없이 500을 던진다")
    @Test
    void unlinkWithoutUserId() {
        // when & then
        assertThatThrownBy(() -> kakaoApiClient.unlink(" "))
            .isInstanceOf(BaseException.class)
            .isNotInstanceOf(ExternalApiException.class)
            .extracting("statusCode")
            .isEqualTo(500);
        wireMock.verify(0, postRequestedFor(urlEqualTo("/v1/user/unlink")));
    }

    private KakaoProperties kakaoProperties() {
        return new KakaoProperties(
            "test-native-app-key",
            "https://kauth.kakao.com",
            wireMock.baseUrl() + "/.well-known/jwks.json",
            "test-admin-key",
            wireMock.baseUrl() + "/v1/user/unlink"
        );
    }
}
