package com.forgather.global.external.social.client;

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

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.ExternalApiException;
import com.forgather.global.exception.ExternalFailureType;
import com.forgather.global.exception.JwtBaseException;
import com.forgather.global.external.ExternalCalls;
import com.forgather.global.external.ExternalOperation;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.external.social.config.AppleProperties;
import com.forgather.global.external.social.config.GoogleProperties;
import com.forgather.global.external.social.config.KakaoProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SocialPublicKeyClient {

    private final RestClient restClient;
    private final Map<SocialProvider, String> jwksUrls;
    private final Map<SocialProvider, Object> keyUpdateLocks;
    private final Map<SocialProvider, List<Map<String, Object>>> keyCaches = new ConcurrentHashMap<>();

    public SocialPublicKeyClient(
        RestClient restClient,
        KakaoProperties kakaoProperties,
        GoogleProperties googleProperties,
        AppleProperties appleProperties
    ) {
        this.restClient = restClient;
        this.jwksUrls = new EnumMap<>(SocialProvider.class);
        this.jwksUrls.put(SocialProvider.KAKAO, kakaoProperties.getJwksUrl());
        this.jwksUrls.put(SocialProvider.GOOGLE, googleProperties.getJwksUrl());
        this.jwksUrls.put(SocialProvider.APPLE, appleProperties.getJwksUrl());
        this.keyUpdateLocks = new EnumMap<>(SocialProvider.class);
        for (SocialProvider provider : SocialProvider.values()) {
            this.keyUpdateLocks.put(provider, new Object());
        }
        updateAllKeys();
    }

    /**
     * 캐시 폴백 우선. JWKS가 흔들려도 캐시에 유효한 키가 있으면 로그인이 살아있게 한다.
     */
    public PublicKey getPublicKey(SocialProvider provider, String kid) {
        Optional<PublicKey> cached = findInCache(provider, kid);
        if (cached.isPresent()) {
            return cached.get();
        }

        synchronized (getKeyUpdateLock(provider)) {
            Optional<PublicKey> rechecked = findInCache(provider, kid);
            if (rechecked.isPresent()) {
                return rechecked.get();
            }
            refreshKeys(provider);
        }

        return findInCache(provider, kid)
            .orElseThrow(() -> new JwtBaseException(
                "Public key not found for provider: %s, kid: %s".formatted(provider, kid),
                HttpStatus.UNAUTHORIZED));
    }

    private Optional<PublicKey> findInCache(SocialProvider provider, String kid) {
        List<Map<String, Object>> keys = keyCaches.get(provider);
        if (keys == null || keys.isEmpty()) {
            return Optional.empty();
        }
        return keys.stream()
            .filter(key -> kid.equals(key.get("kid")))
            .findFirst()
            .map(this::toPublicKey);
    }

    /**
     * 배치 갱신용. 실패해도 기존 캐시로 검증이 계속되므로 전파하지 않는다.
     */
    public void updateKeys(SocialProvider provider) {
        try {
            refreshKeys(provider);
        } catch (Exception e) {
            log.warn("Failed to update social public keys. provider={}", provider, e);
        }
    }

    /**
     * 토큰 검증 경로용. fetch에 성공했을 때만 캐시를 교체하므로 갱신 실패가 기존 캐시를 지우지 않는다.
     */
    private void refreshKeys(SocialProvider provider) {
        keyCaches.put(provider, fetchKeys(provider));
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
            throw new BaseException(
                "JWKS URL이 설정되지 않았습니다. provider: " + provider, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ExternalOperation operation = ExternalOperation.jwks(provider.toExternalService());
        Map<String, Object> jwks = ExternalCalls.execute(operation, () ->
            restClient.get()
                .uri(jwksUrl)
                .retrieve()
                .body(Map.class));

        if (jwks == null || !(jwks.get("keys") instanceof List<?> keys) || keys.isEmpty()) {
            throw new ExternalApiException(
                operation,
                ExternalFailureType.MALFORMED_RESPONSE,
                "JWKS 응답에 keys가 없습니다. provider: " + provider);
        }
        return (List<Map<String, Object>>)keys;
    }
}
