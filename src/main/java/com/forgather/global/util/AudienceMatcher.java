package com.forgather.global.util;

import java.util.Arrays;
import java.util.Collection;

/**
 * ID 토큰의 aud 클레임이 허용된 클라이언트 ID인지 확인한다.
 * aud는 문자열 또는 배열로 내려올 수 있어 두 형태를 모두 처리한다.
 */
public class AudienceMatcher {

    private AudienceMatcher() {
    }

    public static boolean matches(String clientId, Object audience) {
        if (clientId == null || audience == null) {
            return false;
        }
        if (audience instanceof String value) {
            return clientId.equals(value);
        }
        if (audience instanceof Collection<?> values) {
            return values.stream().anyMatch(clientId::equals);
        }
        if (audience instanceof Object[] values) {
            return Arrays.stream(values).anyMatch(clientId::equals);
        }
        return false;
    }
}
