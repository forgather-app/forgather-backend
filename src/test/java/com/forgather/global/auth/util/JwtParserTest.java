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
import java.util.List;

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

    private static final String KAKAO_RAW_NONCE = "kakao-raw-nonce";

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
            .issuer("https://kauth.kakao.com")
            .claim("aud", "test-kakao-native-app-key")
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when
        KakaoIdToken kakaoIdToken = jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE);

        // then
        assertThat(kakaoIdToken.sub()).isEqualTo("12345");
        assertThat(kakaoIdToken.nickname()).isEqualTo("forgather");
        assertThat(kakaoIdToken.email()).isEqualTo("forgather@example.com");
    }

    @DisplayName("Kakao id token의 audience가 배열이어도 앱 키가 포함되면 통과한다")
    @Test
    void parseKakaoIdToken_audienceAsArray() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", List.of("other-audience", "test-kakao-native-app-key"))
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when
        KakaoIdToken kakaoIdToken = jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE);

        // then
        assertThat(kakaoIdToken.sub()).isEqualTo("12345");
    }

    @DisplayName("Kakao id token의 issuer가 다르면 실패한다")
    @Test
    void parseKakaoIdToken_invalidIssuer() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com.evil.example.com")
            .claim("aud", "test-kakao-native-app-key")
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Kakao issuer");
    }

    @DisplayName("Kakao id token의 audience가 앱 키와 다르면 실패한다")
    @Test
    void parseKakaoIdToken_invalidAudience() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", "other-audience")
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Kakao audience");
    }

    @DisplayName("Kakao id token에 만료 시간이 없으면 실패한다")
    @Test
    void parseKakaoIdToken_withoutExpiration() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", "test-kakao-native-app-key")
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("만료 시간");
    }

    @DisplayName("Kakao id token에 사용자 식별자가 없으면 실패한다")
    @Test
    void parseKakaoIdToken_withoutSubject() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", "test-kakao-native-app-key")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("사용자 식별자");
    }

    @DisplayName("Kakao id token에 닉네임이 없으면 실패한다")
    @Test
    void parseKakaoIdToken_withoutNickname() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", "test-kakao-native-app-key")
            .subject("12345")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Kakao 닉네임");
    }

    @DisplayName("Kakao id token에 email이 없으면 실패한다")
    @Test
    void parseKakaoIdToken_withoutEmail() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", "test-kakao-native-app-key")
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Kakao email");
    }

    @DisplayName("Kakao id token의 nonce가 rawNonce 해시와 다르면 실패한다")
    @Test
    void parseKakaoIdToken_nonceMismatch() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", "test-kakao-native-app-key")
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex("different-raw-nonce"))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Kakao nonce");
    }

    @DisplayName("Kakao id token에 nonce가 없으면 실패한다")
    @Test
    void parseKakaoIdToken_withoutNonce() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", "test-kakao-native-app-key")
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, KAKAO_RAW_NONCE))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Kakao nonce");
    }

    @DisplayName("Kakao 로그인 요청에 rawNonce가 없으면 실패한다")
    @Test
    void parseKakaoIdToken_withoutRawNonce() throws Exception {
        // given
        KeyPair keyPair = generateRsaKeyPair();
        stubJwks("/kakao/.well-known/jwks.json", "kakao-key", (RSAPublicKey)keyPair.getPublic());
        JwtParser jwtParser = createJwtParser();
        String idToken = Jwts.builder()
            .header()
            .keyId("kakao-key")
            .and()
            .issuer("https://kauth.kakao.com")
            .claim("aud", "test-kakao-native-app-key")
            .subject("12345")
            .claim("nickname", "forgather")
            .claim("email", "forgather@example.com")
            .claim("nonce", sha256Hex(KAKAO_RAW_NONCE))
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith((RSAPrivateKey)keyPair.getPrivate())
            .compact();

        // when & then
        assertThatThrownBy(() -> jwtParser.parseKakaoIdToken(idToken, null))
            .isInstanceOf(JwtParseException.class)
            .hasMessageContaining("Kakao nonce");
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
            wireMock.baseUrl() + "/apple/auth/token",
            wireMock.baseUrl() + "/apple/auth/revoke"
        );
        KakaoProperties kakaoProperties = new KakaoProperties(
            "test-kakao-native-app-key",
            "https://kauth.kakao.com",
            wireMock.baseUrl() + "/kakao/.well-known/jwks.json",
            "test-admin-key",
            wireMock.baseUrl() + "/kakao/v1/user/unlink"
        );
        SocialAuthClient socialAuthClient = new SocialAuthClient(
            RestClient.create(),
            kakaoProperties,
            new GoogleProperties(wireMock.baseUrl() + "/google/.well-known/jwks.json"),
            appleProperties
        );
        return new JwtParser(new ObjectMapper(), socialAuthClient, appleProperties, kakaoProperties);
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
