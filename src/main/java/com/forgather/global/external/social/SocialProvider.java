package com.forgather.global.external.social;

import com.forgather.global.external.ExternalService;

public enum SocialProvider {
    KAKAO,
    GOOGLE,
    APPLE;

    public ExternalService toExternalService() {
        return switch (this) {
            case KAKAO -> ExternalService.KAKAO;
            case GOOGLE -> ExternalService.GOOGLE;
            case APPLE -> ExternalService.APPLE;
        };
    }
}
