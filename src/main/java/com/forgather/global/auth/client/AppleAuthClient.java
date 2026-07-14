package com.forgather.global.auth.client;

import org.springframework.beans.factory.annotation.Qualifier;
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

@Component
public class AppleAuthClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AppleProperties appleProperties;
    private final AppleClientSecretProvider clientSecretProvider;

    public AppleAuthClient(
        @Qualifier("appleRestClient") RestClient restClient,
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
            throw new BaseException("Apple token 서버에 연결할 수 없습니다.", HttpStatus.BAD_GATEWAY, e);
        }
    }

    private void validateResponse(AppleTokenResponse response) {
        if (response == null
            || !StringUtils.hasText(response.accessToken())
            || !StringUtils.hasText(response.refreshToken())
            || !StringUtils.hasText(response.idToken())
            || response.expiresIn() == null) {
            throw new BaseException("Apple token 응답이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
        }
    }

    private BaseException toAppleTokenException(RestClientResponseException exception) {
        String error = parseError(exception.getResponseBodyAsString());
        return switch (error) {
            case "invalid_request" ->
                new BaseException("Apple token 요청이 올바르지 않습니다.", HttpStatus.BAD_REQUEST, exception);
            case "invalid_grant" ->
                new BaseException("Apple authorization code가 유효하지 않습니다.", HttpStatus.UNAUTHORIZED, exception);
            case "invalid_scope" ->
                new BaseException("Apple token 요청 scope가 올바르지 않습니다.", HttpStatus.BAD_GATEWAY, exception);
            case "invalid_client", "unauthorized_client", "unsupported_grant_type" ->
                new BaseException("Apple token 서버 인증에 실패했습니다.", HttpStatus.BAD_GATEWAY, exception);
            default -> new BaseException("Apple token 교환에 실패했습니다.", HttpStatus.BAD_GATEWAY, exception);
        };
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
