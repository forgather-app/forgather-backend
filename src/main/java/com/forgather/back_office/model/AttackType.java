package com.forgather.back_office.model;

public enum AttackType {

    CREDENTIAL_SCAN("인증 정보 탈취 시도"),
    CMS_SCAN("CMS 관리자 경로 탐색"),
    VCS_DEBUG_SCAN("버전 관리/디버그 파일 노출 탐색"),
    EXTENSION_SCAN("민감한 파일 확장자 탐색"),
    RCE_SCAN("원격 코드 실행 시도"),
    OTHER("기타 의심 요청"),
    ;

    private final String description;

    AttackType(String description) {
        this.description = description;
    }
}
