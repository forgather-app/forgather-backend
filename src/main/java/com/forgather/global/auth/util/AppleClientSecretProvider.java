package com.forgather.global.auth.util;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.forgather.global.config.AppleProperties;
import com.forgather.global.exception.BaseException;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppleClientSecretProvider {

    private static final long CLIENT_SECRET_VALID_MINUTES = 5L;

    private final AppleProperties appleProperties;

    public String generate() {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(CLIENT_SECRET_VALID_MINUTES, ChronoUnit.MINUTES);

        return Jwts.builder()
            .header()
            .keyId(appleProperties.getKeyId())
            .and()
            .issuer(appleProperties.getTeamId())
            .subject(appleProperties.getClientId())
            .audience()
            .single(appleProperties.getIssuer())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(parsePrivateKey(), Jwts.SIG.ES256)
            .compact();
    }

    private PrivateKey parsePrivateKey() {
        try {
            String privateKey = appleProperties.getPrivateKey()
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            byte[] encodedKey = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new BaseException("Apple private key 형식이 올바르지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

}
