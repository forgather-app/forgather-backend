package com.forgather.domain.notification.model;

/**
 * 푸시 디바이스 플랫폼.
 * 웹 푸시는 토큰 체계(VAPID)가 달라 제외한다. 필요해지면 WEB을 추가한다.
 */
public enum DevicePlatform {
    IOS,
    ANDROID,
    ;
}
