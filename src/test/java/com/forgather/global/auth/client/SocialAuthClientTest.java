package com.forgather.global.auth.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import com.forgather.global.config.AppleProperties;
import com.forgather.global.config.GoogleProperties;
import com.forgather.global.config.KakaoProperties;
import com.forgather.global.exception.JwtBaseException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class SocialAuthClientTest {

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

    @DisplayName("provider별 JWKS URL에서 공개키를 조회해 provider별 캐시에 저장한다")
    @Test
    void getPublicKey_loadsKeysByProvider() throws Exception {
        // given
        RsaJwk kakaoJwk = createRsaJwk("kakao-key");
        stubJwks("/kakao/.well-known/jwks.json", kakaoJwk);

        SocialAuthClient socialAuthClient = createSocialAuthClient();

        // when
        PublicKey publicKey = socialAuthClient.getPublicKey(SocialProvider.KAKAO, "kakao-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(kakaoJwk.publicKey().getEncoded());
    }

    @DisplayName("Apple provider의 JWKS URL에서 공개키를 조회해 APPLE 캐시에 저장한다")
    @Test
    void getPublicKey_loadsAppleKeysByProvider() throws Exception {
        // given
        RsaJwk appleJwk = createRsaJwk("apple-key");
        stubJwks("/apple/auth/keys", appleJwk);

        SocialAuthClient socialAuthClient = createSocialAuthClient();

        // when
        PublicKey publicKey = socialAuthClient.getPublicKey(SocialProvider.APPLE, "apple-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(appleJwk.publicKey().getEncoded());
    }

    @DisplayName("공개키 캐시가 비어 있으면 캐시를 갱신한 뒤 조회한다")
    @Test
    void getPublicKey_cacheEmpty_refreshesKeys() throws Exception {
        // given
        stubJwksFailure("/kakao/.well-known/jwks.json");
        SocialAuthClient socialAuthClient = createSocialAuthClient();
        wireMock.resetAll();
        RsaJwk kakaoJwk = createRsaJwk("kakao-key");
        stubJwks("/kakao/.well-known/jwks.json", kakaoJwk);

        // when
        PublicKey publicKey = socialAuthClient.getPublicKey(SocialProvider.KAKAO, "kakao-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(kakaoJwk.publicKey().getEncoded());
    }

    @DisplayName("공개키 캐시 갱신 후에도 비어 있으면 서버 에러로 처리한다")
    @Test
    void getPublicKey_cacheStillEmpty_throwsInternalServerError() {
        // given
        stubJwksFailure("/kakao/.well-known/jwks.json");
        SocialAuthClient socialAuthClient = createSocialAuthClient();
        wireMock.resetAll();
        stubEmptyJwks("/kakao/.well-known/jwks.json");

        // then
        assertThatThrownBy(() -> socialAuthClient.getPublicKey(SocialProvider.KAKAO, "missing-key"))
            .isInstanceOf(JwtBaseException.class)
            .hasMessageContaining("Public key cache is empty")
            .satisfies(exception ->
                assertThat(((JwtBaseException)exception).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    @DisplayName("kid가 캐시에 없으면 공개키를 한 번 갱신한 뒤 다시 조회한다")
    @Test
    void getPublicKey_kidMiss_refreshesKeys() throws Exception {
        // given
        RsaJwk oldJwk = createRsaJwk("old-key");
        stubJwks("/kakao/.well-known/jwks.json", oldJwk);
        SocialAuthClient socialAuthClient = createSocialAuthClient();
        wireMock.resetAll();
        RsaJwk rotatedJwk = createRsaJwk("rotated-key");
        stubJwks("/kakao/.well-known/jwks.json", rotatedJwk);

        // when
        PublicKey publicKey = socialAuthClient.getPublicKey(SocialProvider.KAKAO, "rotated-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(rotatedJwk.publicKey().getEncoded());
    }

    private SocialAuthClient createSocialAuthClient() {
        return new SocialAuthClient(
            RestClient.create(),
            new KakaoProperties("client-id", wireMock.baseUrl() + "/kakao/.well-known/jwks.json",
                "test-admin-key", wireMock.baseUrl() + "/kakao/v1/user/unlink"),
            new GoogleProperties(wireMock.baseUrl() + "/google/.well-known/jwks.json"),
            new AppleProperties(
                wireMock.baseUrl() + "/apple/auth/keys",
                "https://appleid.apple.com",
                "test-apple-audience",
                "test-team-id",
                "test-key-id",
                "test-private-key",
                wireMock.baseUrl() + "/apple/auth/token",
                wireMock.baseUrl() + "/apple/auth/revoke"
            )
        );
    }

    private void stubJwks(String path, RsaJwk jwk) {
        wireMock.stubFor(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "keys": [
                        {
                          "kid": "%s",
                          "kty": "RSA",
                          "alg": "RS256",
                          "use": "sig",
                          "n": "%s",
                          "e": "%s"
                        }
                      ]
                    }
                    """.formatted(jwk.kid(), jwk.modulus(), jwk.exponent()))));
    }

    private void stubEmptyJwks(String path) {
        wireMock.stubFor(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "keys": []
                    }
                    """)));
    }

    private void stubJwksFailure(String path) {
        wireMock.stubFor(get(urlPathEqualTo(path))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "error": "temporary unavailable"
                    }
                    """)));
    }

    private RsaJwk createRsaJwk(String kid) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPublicKey publicKey = (RSAPublicKey)generator.generateKeyPair().getPublic();

        return new RsaJwk(
            kid,
            publicKey,
            encodeUnsigned(publicKey.getModulus()),
            encodeUnsigned(publicKey.getPublicExponent())
        );
    }

    private String encodeUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record RsaJwk(String kid, PublicKey publicKey, String modulus, String exponent) {
    }
}
