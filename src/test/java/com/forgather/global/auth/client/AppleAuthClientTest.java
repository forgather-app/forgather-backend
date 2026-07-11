package com.forgather.global.auth.client;

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
import com.forgather.global.auth.dto.AppleTokenResponse;
import com.forgather.global.auth.util.AppleClientSecretProvider;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.exception.BaseException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class AppleAuthClientTest {

    private WireMockServer wireMock;
    private AppleAuthClient appleAuthClient;

    @BeforeEach
    void setUp() throws Exception {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        AppleProperties appleProperties = appleProperties();
        appleAuthClient = new AppleAuthClient(
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
        AppleTokenResponse response = appleAuthClient.exchangeAuthorizationCode("authorization-code");

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

    @DisplayName("Apple이 invalid_grant를 반환하면 유효하지 않은 authorization code 예외를 던진다")
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
        assertThatThrownBy(() -> appleAuthClient.exchangeAuthorizationCode("invalid-code"))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("authorization code")
            .extracting("statusCode")
            .isEqualTo(401);
    }

    private AppleProperties appleProperties() throws Exception {
        return new AppleProperties(
            wireMock.baseUrl() + "/auth/keys",
            "https://appleid.apple.com",
            "test-client-id",
            "test-team-id",
            "test-key-id",
            toPem(generateEcKeyPair()),
            wireMock.baseUrl() + "/auth/token"
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
