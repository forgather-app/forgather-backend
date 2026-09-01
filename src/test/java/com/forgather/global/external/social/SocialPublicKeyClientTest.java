package com.forgather.global.external.social;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.forgather.global.config.AppleProperties;
import com.forgather.global.config.GoogleProperties;
import com.forgather.global.config.KakaoProperties;
import com.forgather.global.exception.JwtBaseException;
import com.forgather.global.external.ExternalApiException;
import com.forgather.global.external.FailureType;
import com.forgather.global.external.social.client.SocialPublicKeyClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

class SocialPublicKeyClientTest {

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

        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();

        // when
        PublicKey publicKey = socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "kakao-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(kakaoJwk.publicKey().getEncoded());
    }

    @DisplayName("Apple provider의 JWKS URL에서 공개키를 조회해 APPLE 캐시에 저장한다")
    @Test
    void getPublicKey_loadsAppleKeysByProvider() throws Exception {
        // given
        RsaJwk appleJwk = createRsaJwk("apple-key");
        stubJwks("/apple/auth/keys", appleJwk);

        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();

        // when
        PublicKey publicKey = socialPublicKeyClient.getPublicKey(SocialProvider.APPLE, "apple-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(appleJwk.publicKey().getEncoded());
    }

    @DisplayName("공개키 캐시가 비어 있으면 캐시를 갱신한 뒤 조회한다")
    @Test
    void getPublicKey_cacheEmpty_refreshesKeys() throws Exception {
        // given
        stubJwksFailure("/kakao/.well-known/jwks.json");
        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();
        wireMock.resetAll();
        RsaJwk kakaoJwk = createRsaJwk("kakao-key");
        stubJwks("/kakao/.well-known/jwks.json", kakaoJwk);

        // when
        PublicKey publicKey = socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "kakao-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(kakaoJwk.publicKey().getEncoded());
    }

    @DisplayName("JWKS 응답에 keys가 비어 있으면 응답 계약 위반으로 처리한다")
    @Test
    void getPublicKey_cacheStillEmpty_throwsMalformedResponse() {
        // given
        stubJwksFailure("/kakao/.well-known/jwks.json");
        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();
        wireMock.resetAll();
        stubEmptyJwks("/kakao/.well-known/jwks.json");

        // when & then
        assertThatThrownBy(() -> socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "missing-key"))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.MALFORMED_RESPONSE);
                assertThat(exception.getStatusCode()).isEqualTo(500);
            });
    }

    @DisplayName("kid가 캐시에 없으면 공개키를 한 번 갱신한 뒤 다시 조회한다")
    @Test
    void getPublicKey_kidMiss_refreshesKeys() throws Exception {
        // given
        RsaJwk oldJwk = createRsaJwk("old-key");
        stubJwks("/kakao/.well-known/jwks.json", oldJwk);
        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();
        wireMock.resetAll();
        RsaJwk rotatedJwk = createRsaJwk("rotated-key");
        stubJwks("/kakao/.well-known/jwks.json", rotatedJwk);

        // when
        PublicKey publicKey = socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "rotated-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(rotatedJwk.publicKey().getEncoded());
    }

    @DisplayName("kid가 없어 갱신을 시도했으나 JWKS가 5xx면 외부 장애로 처리한다")
    @Test
    void getPublicKey_refreshFailsOnKidMiss_throwsExternalApiException() throws Exception {
        // given
        RsaJwk oldJwk = createRsaJwk("old-key");
        stubJwks("/kakao/.well-known/jwks.json", oldJwk);
        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();
        wireMock.resetAll();
        stubJwksFailure("/kakao/.well-known/jwks.json");

        // when & then
        assertThatThrownBy(() -> socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "rotated-key"))
            .isInstanceOf(ExternalApiException.class)
            .satisfies(thrown -> {
                ExternalApiException exception = (ExternalApiException)thrown;
                assertThat(exception.getType()).isEqualTo(FailureType.UPSTREAM_ERROR);
                assertThat(exception.getStatusCode()).isEqualTo(503);
            });
    }

    @DisplayName("갱신에 실패해도 기존 캐시는 지워지지 않아 다른 kid 조회가 계속 성공한다")
    @Test
    void getPublicKey_refreshFailure_keepsExistingCache() throws Exception {
        // given
        RsaJwk oldJwk = createRsaJwk("old-key");
        stubJwks("/kakao/.well-known/jwks.json", oldJwk);
        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();
        wireMock.resetAll();
        stubJwksFailure("/kakao/.well-known/jwks.json");

        // when — 캐시에 없는 kid를 요청해 갱신을 유발하고 실패시킨다
        assertThatThrownBy(() -> socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "rotated-key"))
            .isInstanceOf(ExternalApiException.class);

        // then — 기존 kid는 여전히 캐시에서 조회된다
        assertThat(socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "old-key").getEncoded())
            .isEqualTo(oldJwk.publicKey().getEncoded());
    }

    @DisplayName("캐시에 kid가 있으면 JWKS를 다시 조회하지 않는다")
    @Test
    void getPublicKey_cacheHit_doesNotRefetch() throws Exception {
        // given
        RsaJwk jwk = createRsaJwk("kakao-key");
        stubJwks("/kakao/.well-known/jwks.json", jwk);
        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();
        wireMock.resetRequests();

        // when
        socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "kakao-key");

        // then
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/kakao/.well-known/jwks.json")));
    }

    @DisplayName("갱신에 성공했는데도 kid가 없으면 위조 토큰으로 보고 401을 던진다")
    @Test
    void getPublicKey_unknownKidAfterSuccessfulRefresh_throwsUnauthorized() throws Exception {
        // given
        RsaJwk jwk = createRsaJwk("kakao-key");
        stubJwks("/kakao/.well-known/jwks.json", jwk);
        SocialPublicKeyClient socialPublicKeyClient = createSocialPublicKeyClient();

        // when & then
        assertThatThrownBy(() -> socialPublicKeyClient.getPublicKey(SocialProvider.KAKAO, "forged-kid"))
            .isInstanceOf(JwtBaseException.class)
            .extracting("statusCode")
            .isEqualTo(401);
    }

    private SocialPublicKeyClient createSocialPublicKeyClient() {
        return new SocialPublicKeyClient(
            RestClient.create(),
            new KakaoProperties("test-kakao-native-app-key",
                "https://kauth.kakao.com",
                wireMock.baseUrl() + "/kakao/.well-known/jwks.json",
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
