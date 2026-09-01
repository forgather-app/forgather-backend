package com.forgather.domain.auth.dev;

import io.swagger.v3.oas.annotations.media.Schema;

public record DevLoginRequest(

    @Schema(description = "개발용 임시 로그인 아이디", example = "your-dev-login-id")
    String loginId,

    @Schema(description = "개발용 임시 로그인 비밀번호", example = "your-dev-login-password")
    String password
) {

    /**
     * LoggingAspect가 @Service 메서드의 파라미터를 toString()으로 로깅하므로,
     * 이 재정의를 지우면 평문 비밀번호가 app.log에 남는다.
     */
    @Override
    public String toString() {
        return "DevLoginRequest[loginId=" + loginId + ", password=***]";
    }
}
