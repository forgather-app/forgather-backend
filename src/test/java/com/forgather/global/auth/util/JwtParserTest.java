package com.forgather.global.auth.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.auth.client.SocialAuthClient;
import com.forgather.global.auth.dto.AppleIdToken;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.config.GoogleProperties;
import com.forgather.global.config.KakaoProperties;
import com.forgather.global.exception.JwtParseException;
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
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
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
        KakaoIdToken kakaoIdToken = jwtParser.parseKakaoIdToken(idToken);

        // then
        assertThat(kakaoIdToken.sub()).isEqualTo("12345");
        assertThat(kakaoIdToken.nickname()).isEqualTo("forgather");
        assertThat(kakaoIdToken.picture()).isEqualTo("https://example.com/profile.png");
    }

    @DisplayName("Apple id token을 검증하고 claim을 반환한다")
    @Test
    void parseAppleIdToken_validToken() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/apple/auth/keys", "apple-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String rawNonce = "raw-nonce";
        String idToken = Jwts.builder()
            .header()
            .keyId("apple-key")
            .and()
            .issuer("https://appleid.apple.com")
            .claim("aud", "test-apple-audience")
            .subject("apple-sub")
            .claim("email", "apple@example.com")
            .claim("email_verified", "true")
            .claim("nonce", sha256Hex(rawNonce))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when
        AppleIdToken appleIdToken = jwtParser.parseAppleIdToken(idToken, rawNonce);

        // then
        assertThat(appleIdToken.sub()).isEqualTo("apple-sub");
        assertThat(appleIdToken.email()).isEqualTo("apple@example.com");
    }

    @DisplayName("Apple id token의 nonce가 rawNonce 해시와 다르면 실패한다")
    @Test
    void parseAppleIdToken_nonceMismatch() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/apple/auth/keys", "apple-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("apple-key")
            .and()
            .issuer("https://appleid.apple.com")
            .claim("aud", "test-apple-audience")
            .subject("apple-sub")
            .claim("email", "apple@example.com")
            .claim("email_verified", true)
            .claim("nonce", sha256Hex("different-raw-nonce"))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // then
        assertThatThrownBy(() -> jwtParser.parseAppleIdToken(idToken, "raw-nonce"))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Apple nonce");
    }

    @DisplayName("Apple id token의 audience가 허용 목록에 없으면 실패한다")
    @Test
    void parseAppleIdToken_invalidAudience() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/apple/auth/keys", "apple-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String rawNonce = "raw-nonce";
        String idToken = Jwts.builder()
            .header()
            .keyId("apple-key")
            .and()
            .issuer("https://appleid.apple.com")
            .claim("aud", "other-audience")
            .subject("apple-sub")
            .claim("email", "apple@example.com")
            .claim("email_verified", true)
            .claim("nonce", sha256Hex(rawNonce))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // then
        assertThatThrownBy(() -> jwtParser.parseAppleIdToken(idToken, rawNonce))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Apple audience");
    }

    @DisplayName("Apple id token의 email_verified가 true가 아니면 실패한다")
    @Test
    void parseAppleIdToken_emailNotVerified() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/apple/auth/keys", "apple-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String rawNonce = "raw-nonce";
        String idToken = Jwts.builder()
            .header()
            .keyId("apple-key")
            .and()
            .issuer("https://appleid.apple.com")
            .claim("aud", "test-apple-audience")
            .subject("apple-sub")
            .claim("email", "apple@example.com")
            .claim("email_verified", false)
            .claim("nonce", sha256Hex(rawNonce))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // then
        assertThatThrownBy(() -> jwtParser.parseAppleIdToken(idToken, rawNonce))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Apple email");
    }

    @DisplayName("Apple id token에 사용자 식별자가 없으면 실패한다")
    @Test
    void parseAppleIdToken_withoutSubject() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/apple/auth/keys", "apple-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String rawNonce = "raw-nonce";
        String idToken = Jwts.builder()
            .header()
            .keyId("apple-key")
            .and()
            .issuer("https://appleid.apple.com")
            .claim("aud", "test-apple-audience")
            .claim("email", "apple@example.com")
            .claim("email_verified", true)
            .claim("nonce", sha256Hex(rawNonce))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseAppleIdToken(idToken, rawNonce))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("사용자 식별자");
    }

    private JwtParser createJwtParser() {
        AppleProperties appleProperties = new AppleProperties(
            wireMock.baseUrl() + "/apple/auth/keys",
            "https://appleid.apple.com",
            "test-apple-audience",
            "test-team-id",
            "test-key-id",
            "test-private-key",
            wireMock.baseUrl() + "/apple/auth/token"
        );
        SocialAuthClient socialAuthClient = new SocialAuthClient(
            RestClient.create(),
            new KakaoProperties("client-id", wireMock.baseUrl() + "/kakao/.well-known/jwks.json"),
            new GoogleProperties(wireMock.baseUrl() + "/google/.well-known/jwks.json"),
            appleProperties
        );
        return new JwtParser(new ObjectMapper(), socialAuthClient, appleProperties);
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private void stubJwks(String path, String kid, RSAPublicKey publicKey) {
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

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
