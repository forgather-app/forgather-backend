package com.forgather.global.auth.client;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.forgather.global.config.GoogleProperties;
import com.forgather.global.config.KakaoProperties;
import com.forgather.global.exception.JwtBaseException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SocialAuthClient {

    private final RestClient restClient;
    private final Map<SocialProvider, String> jwksUrls;
    private final Map<SocialProvider, Object> keyUpdateLocks;
    private final Map<SocialProvider, List<Map<String, Object>>> keyCaches = new ConcurrentHashMap<>();

    public SocialAuthClient(
        RestClient restClient,
        KakaoProperties kakaoProperties,
        GoogleProperties googleProperties
    ) {
        this.restClient = restClient;
        this.jwksUrls = new EnumMap<>(SocialProvider.class);
        this.jwksUrls.put(SocialProvider.KAKAO, kakaoProperties.getJwksUrl());
        this.jwksUrls.put(SocialProvider.GOOGLE, googleProperties.getJwksUrl());
        this.keyUpdateLocks = new EnumMap<>(SocialProvider.class);
        for (SocialProvider provider : SocialProvider.values()) {
            this.keyUpdateLocks.put(provider, new Object());
        }
        updateAllKeys();
    }

    public PublicKey getPublicKey(SocialProvider provider, String kid) {
        List<Map<String, Object>> keys = keyCaches.get(provider);
        if (keys == null || keys.isEmpty()) {
            synchronized (getKeyUpdateLock(provider)) {
                keys = keyCaches.get(provider);
                if (keys == null || keys.isEmpty()) {
                    updateKeys(provider);
                    keys = keyCaches.get(provider);
                }
            }
        }

        Optional<PublicKey> publicKey = findPublicKey(keys, kid);
        if (publicKey.isPresent()) {
            return publicKey.get();
        }

        // kid 없는 경우 갱신
        List<Map<String, Object>> refreshedKeys;
        synchronized (getKeyUpdateLock(provider)) {
            refreshedKeys = keyCaches.get(provider);
            if (refreshedKeys != null && !refreshedKeys.isEmpty()) {
                publicKey = findPublicKey(refreshedKeys, kid);
                if (publicKey.isPresent()) {
                    return publicKey.get();
                }
            }

            updateKeys(provider);
            refreshedKeys = keyCaches.get(provider);
        }

        return findPublicKey(refreshedKeys, kid)
            .orElseThrow(() ->
                new JwtBaseException(
                    "Public key not found for provider: %s, kid: %s".formatted(provider, kid),
                    HttpStatus.UNAUTHORIZED)
            );
    }

    private Optional<PublicKey> findPublicKey(List<Map<String, Object>> keys, String kid) {
        if (keys == null || keys.isEmpty()) {
            throw new JwtBaseException("Public key cache is empty", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return keys.stream()
            .filter(key -> kid.equals(key.get("kid")))
            .findFirst()
            .map(this::toPublicKey);
    }

    public void updateKeys(SocialProvider provider) {
        try {
            keyCaches.put(provider, fetchKeys(provider));
        } catch (Exception e) {
            log.error("Failed to update social public keys. provider={}", provider, e);
        }
    }

    public void updateAllKeys() {
        for (SocialProvider provider : SocialProvider.values()) {
            updateKeys(provider);
        }
    }

    private Object getKeyUpdateLock(SocialProvider provider) {
        return keyUpdateLocks.get(provider);
    }

    private PublicKey toPublicKey(Map<String, Object> key) {
        try {
            String n = (String)key.get("n");
            String e = (String)key.get("e");

            byte[] nBytes = Base64.getUrlDecoder().decode(n);
            byte[] eBytes = Base64.getUrlDecoder().decode(e);

            BigInteger modulus = new BigInteger(1, nBytes);
            BigInteger exponent = new BigInteger(1, eBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JwtBaseException("Failed to get social public key", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchKeys(SocialProvider provider) {
        String jwksUrl = jwksUrls.get(provider);
        if (!StringUtils.hasText(jwksUrl)) {
            throw new IllegalStateException("JWKS URL is not configured for provider: " + provider);
        }

        Map<String, Object> jwks = restClient.get()
            .uri(jwksUrl)
            .retrieve()
            .body(Map.class);
        if (jwks == null) {
            throw new IllegalStateException("Failed to fetch JWKS for provider: " + provider);
        }

        Object keys = jwks.get("keys");
        if (!(keys instanceof List<?>)) {
            throw new IllegalStateException("JWKS keys are missing for provider: " + provider);
        }

        return (List<Map<String, Object>>)keys;
    }
}
