package com.forgather.global.auth.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.auth.client.SocialAuthClient;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.config.GoogleProperties;
import com.forgather.global.config.KakaoProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.jsonwebtoken.Jwts;

class JwtParserTest {

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

    @DisplayName("Kakao id token 파싱 시 SocialAuthClient의 KAKAO 공개키를 사용한다")
    @Test
    void parseIdToken_usesSocialAuthClientKakaoPublicKey() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("kakao-key", (RSAPublicKey)keyPair.getPublic());
        SocialAuthClient socialAuthClient = new SocialAuthClient(
            RestClient.create(),
            new KakaoProperties("client-id", wireMock.baseUrl() + "/kakao/.well-known/jwks.json"),
            new GoogleProperties(wireMock.baseUrl() + "/google/.well-known/jwks.json")
        );
        JwtParser jwtParser = new JwtParser(new ObjectMapper(), socialAuthClient);
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .claim("sub", "12345")
            .claim("nickname", "forgather")
            .claim("picture", "https://example.com/profile.png")
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when
        KakaoIdToken kakaoIdToken = jwtParser.parseIdToken(idToken);

        // then
        assertThat(kakaoIdToken.sub()).isEqualTo("12345");
        assertThat(kakaoIdToken.nickname()).isEqualTo("forgather");
        assertThat(kakaoIdToken.picture()).isEqualTo("https://example.com/profile.png");
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private void stubJwks(String kid, RSAPublicKey publicKey) {
        wireMock.stubFor(get(urlPathEqualTo("/kakao/.well-known/jwks.json"))
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
                    """.formatted(
                        kid,
                        encodeUnsigned(publicKey.getModulus()),
                        encodeUnsigned(publicKey.getPublicExponent())
                    ))));
    }

    private String encodeUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
