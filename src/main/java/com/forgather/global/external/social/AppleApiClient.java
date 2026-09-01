package com.forgather.global.external.social;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.ExternalApiException;
import com.forgather.global.external.ExternalCalls;
import com.forgather.global.external.ExternalOperation;
import com.forgather.global.external.FailureType;
import com.forgather.global.external.social.AppleClientSecretProvider;
import com.forgather.global.external.social.dto.AppleTokenErrorResponse;
import com.forgather.global.external.social.dto.AppleTokenResponse;

@Component
public class AppleApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppleProperties appleProperties;
    private final AppleClientSecretProvider clientSecretProvider;

    public AppleApiClient(
        RestClient restClient,
        ObjectMapper objectMapper,
        AppleProperties appleProperties,
        AppleClientSecretProvider clientSecretProvider
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.appleProperties = appleProperties;
        this.clientSecretProvider = clientSecretProvider;
    }

    public AppleTokenResponse exchangeAuthorizationCode(String authorizationCode) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new BaseException("Apple authorization code가 필요합니다.", HttpStatus.BAD_REQUEST);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", appleProperties.getClientId());
        form.add("client_secret", clientSecretProvider.generate());
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");

        AppleTokenResponse response;
        try {
            response = ExternalCalls.execute(ExternalOperation.APPLE_TOKEN, () ->
                restClient.post()
                    .uri(appleProperties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class));
        } catch (ExternalApiException e) {
            // invalid_grant는 이미 쓴 code를 다시 보냈거나 만료된 경우로 사용자가 재로그인해야 한다.
            if (e.getType() == FailureType.CALLER_ERROR
                && "invalid_grant".equals(parseError(e.getResponseBody()))) {
                throw e.as(FailureType.AUTH_REJECTED);
            }
            throw e;
        }
        validateResponse(response);
        return response;
    }

    public void revoke(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BaseException("Apple refresh token이 필요합니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", appleProperties.getClientId());
        form.add("client_secret", clientSecretProvider.generate());
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        ExternalCalls.execute(ExternalOperation.APPLE_REVOKE, () ->
            restClient.post()
                .uri(appleProperties.getRevokeUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity());
    }

    private void validateResponse(AppleTokenResponse response) {
        if (response == null
            || !StringUtils.hasText(response.accessToken())
            || !StringUtils.hasText(response.refreshToken())
            || !StringUtils.hasText(response.idToken())
            || response.expiresIn() == null) {
            throw new ExternalApiException(
                ExternalOperation.APPLE_TOKEN,
                FailureType.MALFORMED_RESPONSE,
                "Apple token 응답이 올바르지 않습니다.");
        }
    }

    private String parseError(String responseBody) {
        try {
            AppleTokenErrorResponse response = objectMapper.readValue(responseBody, AppleTokenErrorResponse.class);
            return response.error();
        } catch (Exception e) {
            return "unknown_error";
        }
    }
}
