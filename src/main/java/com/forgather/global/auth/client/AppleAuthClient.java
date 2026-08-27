package com.forgather.global.auth.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.auth.dto.AppleTokenErrorResponse;
import com.forgather.global.auth.dto.AppleTokenResponse;
import com.forgather.global.auth.util.AppleClientSecretProvider;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.ExternalApiException;
import com.forgather.global.external.ExternalCalls;
import com.forgather.global.external.ExternalOperation;
import com.forgather.global.external.FailureType;

@Component
public class AppleAuthClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppleProperties appleProperties;
    private final AppleClientSecretProvider clientSecretProvider;

    public AppleAuthClient(
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
            throw refine(e);
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

        // revoke의 4xx는 client_secret 설정이나 저장된 refresh token이 잘못된 우리 쪽 문제이므로
        // 1차 분류(CALLER_ERROR → 500 error)를 그대로 쓴다. 세분화할 provider 지식이 없다.
        ExternalCalls.execute(ExternalOperation.APPLE_REVOKE, () ->
            restClient.post()
                .uri(appleProperties.getRevokeUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity());
    }

    /**
     * token 교환의 2차 세분화. 응답 본문의 error 코드를 읽어야만 아는 것만 다룬다.
     * 5xx·타임아웃 분류는 provider 지식이 필요 없으므로 손대지 않는다.
     */
    private ExternalApiException refine(ExternalApiException exception) {
        if (exception.getType() != FailureType.CALLER_ERROR) {
            return exception;
        }
        // invalid_grant는 이미 쓴 code를 다시 보냈거나 만료된 경우로 사용자가 재로그인해야 한다.
        if ("invalid_grant".equals(parseError(exception.getResponseBody()))) {
            return exception.as(FailureType.AUTH_REJECTED);
        }
        return exception;
    }

    /**
     * 200인데 필수 필드가 비었다면 애플이 계약을 바꿨거나 우리 DTO가 어긋난 것이다.
     * 외부 장애로 위장되지 않도록 error로 분류한다.
     */
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
