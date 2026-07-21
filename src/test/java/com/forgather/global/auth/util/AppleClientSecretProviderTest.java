package com.forgather.global.auth.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.exception.BaseException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

class AppleClientSecretProviderTest {

    @DisplayName("Apple private key로 ES256 client secret을 생성한다")
    @Test
    void generateClientSecret() throws Exception {
        // given
        KeyPair keyPair = generateEcKeyPair();
        AppleClientSecretProvider provider = new AppleClientSecretProvider(
            appleProperties(toPem(keyPair))
        );

        // when
        String clientSecret = provider.generate();

        // then
        Jws<Claims> parsed = Jwts.parser()
            .verifyWith((ECPublicKey)keyPair.getPublic())
            .build()
            .parseSignedClaims(clientSecret);
        Claims claims = parsed.getPayload();
        String payload = new String(Base64.getUrlDecoder().decode(clientSecret.split("\\.")[1]));
        JsonNode audience = new ObjectMapper().readTree(payload).get("aud");
        assertAll(
            () -> assertThat(parsed.getHeader().getKeyId()).isEqualTo("test-key-id"),
            () -> assertThat(claims.getIssuer()).isEqualTo("test-team-id"),
            () -> assertThat(claims.getSubject()).isEqualTo("test-client-id"),
            () -> assertThat(claims.getAudience()).contains("https://appleid.apple.com"),
            () -> assertThat(audience.isTextual()).isTrue(),
            () -> assertThat(audience.textValue()).isEqualTo("https://appleid.apple.com"),
            () -> assertThat(claims.getIssuedAt().toInstant()).isBeforeOrEqualTo(Instant.now()),
            () -> assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt())
        );
    }

    @DisplayName("Apple private key 형식이 올바르지 않으면 Provider를 생성할 수 없다")
    @Test
    void createProviderWithInvalidPrivateKey() {
        // given
        AppleProperties properties = appleProperties("invalid-private-key");

        // when & then
        assertThatThrownBy(() -> new AppleClientSecretProvider(properties))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("Apple private key 형식");
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

    private AppleProperties appleProperties(String privateKey) {
        return new AppleProperties(
            "https://appleid.apple.com/auth/keys",
            "https://appleid.apple.com",
            "test-client-id",
            "test-team-id",
            "test-key-id",
            privateKey,
            "https://appleid.apple.com/auth/token"
        );
    }
}
