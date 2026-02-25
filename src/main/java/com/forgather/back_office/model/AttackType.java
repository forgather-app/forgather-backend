package com.forgather.back_office.model;

import lombok.Getter;

@Getter
public enum AttackType {

    CREDENTIAL_SCAN("credential_scan", "인증 정보 탈취 시도"),
    CMS_SCAN("cms_scan", "CMS 관리자 경로 탐색"),
    VCS_DEBUG_SCAN("vcs_debug_scan", "버전 관리/디버그 파일 노출 탐색"),
    EXTENSION_SCAN("extension_scan", "민감한 파일 확장자 탐색"),
    RCE_SCAN("rce_scan", "원격 코드 실행 시도"),
    OTHER("other", "기타 의심 요청"),
    ;

    private final String key;
    private final String description;

    AttackType(String key, String description) {
        this.key = key;
        this.description = description;
    }
}
