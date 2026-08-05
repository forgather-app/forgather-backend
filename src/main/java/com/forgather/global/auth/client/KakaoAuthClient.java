package com.forgather.global.auth.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.forgather.global.config.KakaoProperties;
import com.forgather.global.exception.BaseException;

import lombok.RequiredArgsConstructor;

/**
 * 카카오 인증 관련 클라이언트
 * https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api
 */
@Component
@RequiredArgsConstructor
public class KakaoAuthClient {

    private static final String ADMIN_KEY_PREFIX = "KakaoAK ";

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

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
            restClient.post()
                .uri(kakaoProperties.getUnlinkUrl())
                .header(HttpHeaders.AUTHORIZATION, ADMIN_KEY_PREFIX + kakaoProperties.getAdminKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new BaseException("Kakao unlink에 실패했습니다.", HttpStatus.BAD_GATEWAY, e);
        } catch (RestClientException e) {
            throw new BaseException("Kakao 서버에 연결할 수 없습니다.", HttpStatus.BAD_GATEWAY, e);
        }
    }
}
