package com.forgather.global.auth.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.config.KakaoProperties;
import com.forgather.global.exception.BaseException;
import com.forgather.global.external.ExternalApiException;
import com.forgather.global.external.ExternalCalls;
import com.forgather.global.external.ExternalOperation;
import com.forgather.global.external.FailureType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 카카오 인증 관련 클라이언트
 * https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoAuthClient {

    private static final String ADMIN_KEY_PREFIX = "KakaoAK ";
    private static final int ALREADY_UNLINKED_CODE = -101;

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;
    private final ObjectMapper objectMapper;

    /**
     * 카카오 연결 끊기(unlink)
     * https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#unlink
     */
    public void unlink(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BaseException("Kakao user id가 필요합니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", userId);

        try {
            ExternalCalls.execute(ExternalOperation.KAKAO_UNLINK, () ->
                restClient.post()
                    .uri(kakaoProperties.getUnlinkUrl())
                    .header(HttpHeaders.AUTHORIZATION, ADMIN_KEY_PREFIX + kakaoProperties.getAdminKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity());
        } catch (ExternalApiException e) {
            if (e.getType() == FailureType.CALLER_ERROR && isAlreadyUnlinked(e.getResponseBody())) {
                log.atInfo()
                    .addKeyValue("service", "KAKAO")
                    .addKeyValue("operation", "unlink")
                    .addKeyValue("result", "alreadyUnlinked")
                    .log("Kakao unlink 대상이 이미 해제되어 있습니다.");
                return;
            }
            throw e;
        }
    }

    /**
     * -101은 이미 앱과 연결이 끊긴 사용자로 unlink의 목적이 이미 달성된 상태다.
     */
    private boolean isAlreadyUnlinked(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return false;
        }
        try {
            JsonNode code = objectMapper.readTree(responseBody).get("code");
            return code != null && code.asInt() == ALREADY_UNLINKED_CODE;
        } catch (Exception e) {
            return false;
        }
    }
}
