package com.forgather.global.external;

import java.util.Locale;

/**
 * 외부 호출 단위. 멱등성을 선언하는 유일한 자리다.
 * read timeout처럼 "요청은 갔는데 응답을 못 받은" 상황의 재시도 가능 여부가 여기서 갈린다.
 */
public record ExternalOperation(ExternalService service, String name, boolean idempotent) {

    /**
     * authorization code는 1회용이라 read timeout 후 재시도하면 반드시 invalid_grant가 된다.
     */
    public static final ExternalOperation APPLE_TOKEN =
        new ExternalOperation(ExternalService.APPLE, "token", false);

    /**
     * RFC 7009 — 이미 폐기된 토큰에도 200을 반환하므로 멱등하다.
     */
    public static final ExternalOperation APPLE_REVOKE =
        new ExternalOperation(ExternalService.APPLE, "revoke", true);

    public static final ExternalOperation KAKAO_UNLINK =
        new ExternalOperation(ExternalService.KAKAO, "unlink", true);

    public static ExternalOperation jwks(ExternalService service) {
        return new ExternalOperation(service, "jwks", true);
    }

    public String id() {
        return service.name().toLowerCase(Locale.ROOT) + "/" + name;
    }
}
