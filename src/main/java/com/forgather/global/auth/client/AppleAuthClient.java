package com.forgather.global.auth.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.auth.dto.AppleTokenErrorResponse;
import com.forgather.global.auth.dto.AppleTokenResponse;
import com.forgather.global.auth.util.AppleClientSecretProvider;
import com.forgather.global.config.AppleProperties;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.ExternalApiException;

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

        try {
            AppleTokenResponse response = restClient.post()
                .uri(appleProperties.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(AppleTokenResponse.class);
            validateResponse(response);
            return response;
        } catch (RestClientResponseException e) {
            throw toAppleTokenException(e);
        } catch (RestClientException e) {
            throw new ExternalApiException("Apple token 서버에 연결할 수 없습니다.", e);
        }
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

        try {
            restClient.post()
                .uri(appleProperties.getRevokeUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw toAppleRevokeException(e);
        } catch (RestClientException e) {
            throw new ExternalApiException("Apple token 서버에 연결할 수 없습니다.", e);
        }
    }

    private void validateResponse(AppleTokenResponse response) {
        if (response == null
            || !StringUtils.hasText(response.accessToken())
            || !StringUtils.hasText(response.refreshToken())
            || !StringUtils.hasText(response.idToken())
            || response.expiresIn() == null) {
            throw new ExternalApiException("Apple token 응답이 올바르지 않습니다.");
        }
    }

    private BaseException toAppleTokenException(RestClientResponseException exception) {
        if (exception.getStatusCode().is5xxServerError()) {
            return new ExternalApiException("Apple token 서버에 장애가 발생했습니다.", exception);
        }
        String error = parseError(exception.getResponseBodyAsString());
        return switch (error) {
            case "invalid_grant" ->
                new BaseException("Apple authorization code가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED, exception);
            case "invalid_request" ->
                new BaseException("Apple token 요청이 올바르지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR, exception);
            case "invalid_scope" ->
                new BaseException("Apple token 요청 scope가 올바르지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR, exception);
            case "invalid_client", "unauthorized_client", "unsupported_grant_type" ->
                new BaseException("Apple token 서버 인증에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, exception);
            default -> new BaseException("Apple token 교환에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, exception);
        };
    }

    /**
     * 4xx는 client_secret 설정 오류나 저장된 refresh token이 잘못된 우리 쪽 문제이므로 500으로 남긴다.
     * 5xx만 외부 장애로 분류한다.
     */
    private BaseException toAppleRevokeException(RestClientResponseException exception) {
        if (exception.getStatusCode().is5xxServerError()) {
            return new ExternalApiException("Apple token 서버에 장애가 발생했습니다.", exception);
        }
        return new BaseException("Apple token revoke에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, exception);
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
