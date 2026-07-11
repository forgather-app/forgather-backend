package com.forgather.global.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.auth.client.SocialAuthClient;
import com.forgather.global.auth.client.SocialProvider;
import com.forgather.global.auth.dto.AppleIdToken;
import com.forgather.global.auth.dto.KakaoIdToken;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.exception.JwtParseException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtParser {

    private final ObjectMapper objectMapper;
    private final SocialAuthClient socialAuthClient;
    private final AppleProperties appleProperties;

    public KakaoIdToken parseKakaoIdToken(String idToken) {
        try {
            Claims claims = parseClaims(idToken, SocialProvider.KAKAO);
            return objectMapper.convertValue(claims, KakaoIdToken.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new JwtParseException("Kakao ID Token 형식이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED, e);
        }
    }

    public AppleIdToken parseAppleIdToken(String idToken, String rawNonce) {
        try {
            Claims claims = parseClaims(idToken, SocialProvider.APPLE);
            AppleIdToken appleIdToken = objectMapper.convertValue(claims, AppleIdToken.class);
            validateAppleIdToken(appleIdToken, rawNonce);
            return appleIdToken;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new JwtParseException("Apple ID Token 형식이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED, e);
        }
    }

    private Claims parseClaims(String idToken, SocialProvider provider) throws JsonProcessingException {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new JwtParseException("Invalid JWT format", HttpStatus.UNAUTHORIZED);
        }

        String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> headerMap = objectMapper.readValue(header, Map.class);
        String kid = (String)headerMap.get("kid");

        if (kid == null) {
            throw new JwtParseException("Missing kid in JWT header", HttpStatus.UNAUTHORIZED);
        }

        PublicKey publicKey = socialAuthClient.getPublicKey(provider, kid);

        return Jwts.parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(idToken)
            .getPayload();
    }

    private void validateAppleIdToken(AppleIdToken idToken, String rawNonce) {
        if (!appleProperties.getIssuer().equals(idToken.iss())) {
            throw new JwtParseException("Apple issuer가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!appleProperties.isAllowedAudience(idToken.aud())) {
            throw new JwtParseException("Apple audience가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (idToken.exp() == null) {
            throw new JwtParseException("Apple token 만료 시간이 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(idToken.sub())) {
            throw new JwtParseException("Apple 사용자 식별자가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(idToken.email())) {
            throw new JwtParseException("Apple email이 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!idToken.isEmailVerified()) {
            throw new JwtParseException("Apple email이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(rawNonce) || !StringUtils.hasText(idToken.nonce())) {
            throw new JwtParseException("Apple nonce가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!hashRawNonce(rawNonce).equals(idToken.nonce())) {
            throw new JwtParseException("Apple nonce가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
    }

    private String hashRawNonce(String rawNonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawNonce.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new JwtParseException("Apple nonce hash failed", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
