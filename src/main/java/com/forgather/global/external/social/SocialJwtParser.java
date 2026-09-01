package com.forgather.global.external.social;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.config.KakaoProperties;
import com.forgather.global.exception.JwtParseException;
import com.forgather.global.external.social.client.SocialPublicKeyClient;
import com.forgather.global.external.social.dto.AppleIdToken;
import com.forgather.global.external.social.dto.KakaoIdToken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SocialJwtParser {

    private final ObjectMapper objectMapper;
    private final SocialPublicKeyClient socialPublicKeyClient;
    private final AppleProperties appleProperties;
    private final KakaoProperties kakaoProperties;

    public KakaoIdToken parseKakaoIdToken(String idToken, String rawNonce) {
        try {
            Claims claims = parseClaims(idToken, SocialProvider.KAKAO);
            KakaoIdToken kakaoIdToken = objectMapper.convertValue(claims, KakaoIdToken.class);
            validateKakaoIdToken(kakaoIdToken, rawNonce);
            return kakaoIdToken;
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
        if (idToken == null || idToken.isBlank()) {
            throw new JwtParseException("ID Token이 제공되지 않았습니다.", HttpStatus.UNAUTHORIZED);
        }

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

        PublicKey publicKey = socialPublicKeyClient.getPublicKey(provider, kid);

        try {
            return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(idToken)
                .getPayload();
        } catch (JwtException e) {
            throw new JwtParseException("JWT 검증에 실패했습니다.", HttpStatus.UNAUTHORIZED, e);
        }
    }

    /**
     * 카카오 콘솔에서 닉네임과 카카오계정(이메일)을 필수 동의로 설정했으므로 두 클레임이 항상 존재한다고 보고 검증한다.
     * nonce는 애플과 동일하게, 클라이언트가 원본을 SHA-256 해싱해 카카오에 전달했다고 보고 해시를 대조한다.
     */
    private void validateKakaoIdToken(KakaoIdToken idToken, String rawNonce) {
        if (!kakaoProperties.getIssuer().equals(idToken.iss())) {
            throw new JwtParseException("Kakao issuer가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!kakaoProperties.isAllowedAudience(idToken.aud())) {
            throw new JwtParseException("Kakao audience가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (idToken.exp() == null) {
            throw new JwtParseException("Kakao token 만료 시간이 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(idToken.sub())) {
            throw new JwtParseException("Kakao 사용자 식별자가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(idToken.nickname())) {
            throw new JwtParseException("Kakao 닉네임이 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(idToken.email())) {
            throw new JwtParseException("Kakao email이 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(rawNonce) || !StringUtils.hasText(idToken.nonce())) {
            throw new JwtParseException("Kakao nonce가 없습니다.", HttpStatus.UNAUTHORIZED);
        }
        if (!hashRawNonce(rawNonce).equals(idToken.nonce())) {
            throw new JwtParseException("Kakao nonce가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
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
            throw new JwtParseException("nonce hash에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
