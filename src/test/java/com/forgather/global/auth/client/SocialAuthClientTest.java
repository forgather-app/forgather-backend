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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

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

        RestClient restClient = RestClient.create();
        SocialAuthClient socialAuthClient = new SocialAuthClient(
            restClient,
            new KakaoProperties("client-id", wireMock.baseUrl() + "/kakao/.well-known/jwks.json"),
            new GoogleProperties(wireMock.baseUrl() + "/google/.well-known/jwks.json")
        );

        // when
        PublicKey publicKey = socialAuthClient.getPublicKey(SocialProvider.KAKAO, "kakao-key");

        // then
        assertThat(publicKey.getEncoded()).isEqualTo(kakaoJwk.publicKey().getEncoded());
    }

    @DisplayName("provider별 공개키 캐시를 분리해 같은 kid도 provider에 따라 다른 키를 반환한다")
    @Test
    void getPublicKey_separatesCacheByProvider() throws Exception {
        // given
        RsaJwk kakaoJwk = createRsaJwk("same-kid");
        RsaJwk googleJwk = createRsaJwk("same-kid");
        stubJwks("/kakao/.well-known/jwks.json", kakaoJwk);
        stubJwks("/google/.well-known/jwks.json", googleJwk);

        SocialAuthClient socialAuthClient = createSocialAuthClient();

        // when
        PublicKey kakaoPublicKey = socialAuthClient.getPublicKey(SocialProvider.KAKAO, "same-kid");
        PublicKey googlePublicKey = socialAuthClient.getPublicKey(SocialProvider.GOOGLE, "same-kid");

        // then
        assertThat(kakaoPublicKey.getEncoded()).isEqualTo(kakaoJwk.publicKey().getEncoded());
        assertThat(googlePublicKey.getEncoded()).isEqualTo(googleJwk.publicKey().getEncoded());
    }

    @DisplayName("초기 JWKS 조회가 실패해도 생성은 성공하고 캐시는 비워둔다")
    @Test
    void constructor_initialFetchFails_keepsApplicationStartup() {
        // given
        stubJwksFailure("/kakao/.well-known/jwks.json");

        // when
        SocialAuthClient socialAuthClient = createSocialAuthClient();

        // then
        assertThatThrownBy(() -> socialAuthClient.getPublicKey(SocialProvider.KAKAO, "missing-key"))
            .isInstanceOf(JwtBaseException.class)
            .hasMessageContaining("Public key cache is empty");
    }

    @DisplayName("스케줄 갱신 실패 시 기존 provider 캐시를 유지한다")
    @Test
    void updateKeys_fetchFails_keepsExistingCache() throws Exception {
        // given
        RsaJwk kakaoJwk = createRsaJwk("kakao-key");
        stubJwks("/kakao/.well-known/jwks.json", kakaoJwk);
        SocialAuthClient socialAuthClient = createSocialAuthClient();
        wireMock.resetAll();
        stubJwksFailure("/kakao/.well-known/jwks.json");

        // when
        socialAuthClient.updateKeys(SocialProvider.KAKAO);

        // then
        PublicKey publicKey = socialAuthClient.getPublicKey(SocialProvider.KAKAO, "kakao-key");
        assertThat(publicKey.getEncoded()).isEqualTo(kakaoJwk.publicKey().getEncoded());
    }

    private SocialAuthClient createSocialAuthClient() {
        return new SocialAuthClient(
            RestClient.create(),
            new KakaoProperties("client-id", wireMock.baseUrl() + "/kakao/.well-known/jwks.json"),
            new GoogleProperties(wireMock.baseUrl() + "/google/.well-known/jwks.json")
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
