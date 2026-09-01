package com.forgather.global.external.social;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.external.ExternalApiException;
import com.forgather.global.external.FailureType;
import com.forgather.global.external.social.dto.AppleTokenResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;

class AppleApiClientTest {

    private WireMockServer wireMock;
    private AppleApiClient appleApiClient;

    @BeforeEach
    void setUp() throws Exception {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        AppleProperties appleProperties = appleProperties();
        appleApiClient = new AppleApiClient(
            RestClient.create(),
            new ObjectMapper(),
            appleProperties,
            new AppleClientSecretProvider(appleProperties)
        );
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @DisplayName("Apple authorization code를 form 요청으로 교환하고 token 응답을 반환한다")
    @Test
    void exchangeAuthorizationCode() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/token"))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {
                      "access_token": "apple-access-token",
                      "token_type": "Bearer",
                      "expires_in": 3600,
                      "refresh_token": "apple-refresh-token",
                      "id_token": "apple-id-token"
                    }
                    """)));

        // when
        AppleTokenResponse response = appleApiClient.exchangeAuthorizationCode("authorization-code");

        // then
        assertAll(
            () -> assertThat(response.accessToken()).isEqualTo("apple-access-token"),
            () -> assertThat(response.refreshToken()).isEqualTo("apple-refresh-token"),
            () -> assertThat(response.idToken()).isEqualTo("apple-id-token")
        );
        wireMock.verify(postRequestedFor(urlEqualTo("/auth/token"))
            .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .withRequestBody(containing("client_id=test-client-id"))
            .withRequestBody(containing("code=authorization-code"))
            .withRequestBody(containing("grant_type=authorization_code")));
    }

    @DisplayName("Apple이 invalid_grant를 반환하면 사용자 입력 문제로 분류한다")
    @Test
    void exchangeAuthorizationCodeWithInvalidGrant() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/token"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"error":"invalid_grant"}
                    """)));

        // when & then
        assertThatThrownBy(() -> appleApiClient.exchangeAuthorizationCode("invalid-code"))
            .isInstanceOf(ExternalApiException.class)
            .extracting("type")
            .isEqualTo(FailureType.AUTH_REJECTED);
    }

    @DisplayName("Apple이 invalid_client를 반환하면 우리 설정 오류로 분류한다")
    @Test
    void exchangeAuthorizationCodeWithInvalidClient() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/token"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"error":"invalid_client"}
                    """)));

        // when & then
        assertThatThrownBy(() -> appleApiClient.exchangeAuthorizationCode("code"))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.CALLER_ERROR);
                assertThat(exception.getStatusCode()).isEqualTo(500);
            });
    }

    @DisplayName("200이지만 필수 필드가 비면 우리 DTO 문제로 보고 MALFORMED_RESPONSE로 분류한다")
    @Test
    void exchangeAuthorizationCodeWithIncompleteBody() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/token"))
            .willReturn(aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"access_token":"only-access-token"}
                    """)));

        // when & then
        assertThatThrownBy(() -> appleApiClient.exchangeAuthorizationCode("code"))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.MALFORMED_RESPONSE);
                assertThat(exception.getStatusCode()).isEqualTo(500);
            });
    }

    @DisplayName("Apple이 invalid_scope를 반환하면 token 요청 설정 예외를 던진다")
    @Test
    void exchangeAuthorizationCodeWithInvalidScope() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/token"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"error":"invalid_scope"}
                    """)));

        // when & then
        assertThatThrownBy(() -> appleApiClient.exchangeAuthorizationCode("authorization-code"))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.CALLER_ERROR);
                assertThat(exception.getStatusCode()).isEqualTo(500);
            });
    }

    @DisplayName("Apple token 서버가 5xx를 반환하면 외부 장애로 분류한다")
    @Test
    void exchangeAuthorizationCodeWithServerError() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/token"))
            .willReturn(aResponse().withStatus(503)));

        // when & then
        assertThatThrownBy(() -> appleApiClient.exchangeAuthorizationCode("code"))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.UPSTREAM_ERROR);
                assertThat(exception.getStatusCode()).isEqualTo(503);
            });
    }

    @DisplayName("Apple token 서버 연결에 실패하면 재시도 없이 외부 서비스 장애 예외를 던진다")
    @Test
    void exchangeAuthorizationCodeWithConnectionFailure() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/token"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        // when & then
        assertThatThrownBy(() -> appleApiClient.exchangeAuthorizationCode("authorization-code"))
            .isInstanceOf(ExternalApiException.class)
            .extracting("statusCode")
            .isEqualTo(503);
        wireMock.verify(1, postRequestedFor(urlEqualTo("/auth/token")));
    }

    @DisplayName("Apple revoke가 4xx로 실패하면 우리 쪽 오류로 500을 던진다")
    @Test
    void revokeWithClientError() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/revoke"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("""
                    {"error":"invalid_client"}
                    """)));

        // when & then
        assertThatThrownBy(() -> appleApiClient.revoke("apple-refresh-token"))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.CALLER_ERROR);
                assertThat(exception.getStatusCode()).isEqualTo(500);
            });
    }

    @DisplayName("Apple revoke가 5xx로 실패하면 외부 서비스 장애 예외를 던진다")
    @Test
    void revokeWithServerError() {
        // given
        wireMock.stubFor(post(urlEqualTo("/auth/revoke"))
            .willReturn(aResponse().withStatus(500)));

        // when & then
        assertThatThrownBy(() -> appleApiClient.revoke("apple-refresh-token"))
            .isInstanceOf(ExternalApiException.class)
            .extracting("statusCode")
            .isEqualTo(503);
    }

    private AppleProperties appleProperties() throws Exception {
        return new AppleProperties(
            wireMock.baseUrl() + "/auth/keys",
            "https://appleid.apple.com",
            "test-client-id",
            "test-team-id",
            "test-key-id",
            toPem(generateEcKeyPair()),
            wireMock.baseUrl() + "/auth/token",
            wireMock.baseUrl() + "/auth/revoke"
        );
    }

    private KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private String toPem(KeyPair keyPair) {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
            .encodeToString(keyPair.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
    }
}
