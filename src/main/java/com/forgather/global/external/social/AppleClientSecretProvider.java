package com.forgather.global.external.social;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.forgather.global.exception.BaseException;
import com.forgather.global.external.social.config.AppleProperties;

import io.jsonwebtoken.Jwts;

@Component
public class AppleClientSecretProvider {

    private static final long CLIENT_SECRET_VALID_MINUTES = 5L;

    private final AppleProperties appleProperties;
    private final PrivateKey privateKey;

    public AppleClientSecretProvider(AppleProperties appleProperties) {
        this.appleProperties = appleProperties;
        this.privateKey = parsePrivateKey(appleProperties.getPrivateKey());
    }

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
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact();
    }

    private PrivateKey parsePrivateKey(String privateKeyValue) {
        try {
            String normalizedPrivateKey = privateKeyValue
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            byte[] encodedKey = Base64.getDecoder().decode(normalizedPrivateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new BaseException("Apple private key 형식이 올바르지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

}
