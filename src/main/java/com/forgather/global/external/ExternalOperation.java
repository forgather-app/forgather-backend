package com.forgather.global.external;

import java.util.Locale;

public enum ExternalOperation {

    /**
     * authorization code는 1회용이라 read timeout 후 재시도하면 반드시 invalid_grant가 된다.
     */
    APPLE_TOKEN(ExternalService.APPLE, "token", false),

    /**
     * RFC 7009 — 이미 폐기된 토큰에도 200을 반환하므로 멱등하다.
     */
    APPLE_REVOKE(ExternalService.APPLE, "revoke", true),

    KAKAO_UNLINK(ExternalService.KAKAO, "unlink", true),

    APPLE_JWKS(ExternalService.APPLE, "jwks", true),

    KAKAO_JWKS(ExternalService.KAKAO, "jwks", true),

    GOOGLE_JWKS(ExternalService.GOOGLE, "jwks", true);

    private final ExternalService service;
    private final String operationName;
    private final boolean idempotent;

    ExternalOperation(ExternalService service, String operationName, boolean idempotent) {
        this.service = service;
        this.operationName = operationName;
        this.idempotent = idempotent;
    }

    public static ExternalOperation jwks(ExternalService service) {
        return switch (service) {
            case APPLE -> APPLE_JWKS;
            case KAKAO -> KAKAO_JWKS;
            case GOOGLE -> GOOGLE_JWKS;
        };
    }

    public ExternalService service() {
        return service;
    }

    /**
     * enum의 name()은 상수 이름(APPLE_TOKEN)을 돌려주므로 별도 접근자를 둔다.
     */
    public String operationName() {
        return operationName;
    }

    public boolean idempotent() {
        return idempotent;
    }

    public String id() {
        return service.name().toLowerCase(Locale.ROOT) + "/" + operationName;
    }
}
