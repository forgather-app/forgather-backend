package com.forgather.domain.host.model;

import com.forgather.global.external.social.SocialProvider;

public sealed interface OauthHost permits KakaoHost, AppleHost {
    SocialProvider getProvider();
    String getUserId();
    String getRefreshToken();
}
