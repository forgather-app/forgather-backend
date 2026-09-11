package com.forgather.domain.host.service;

import com.forgather.domain.host.model.OauthHost;
import com.forgather.global.external.social.SocialProvider;

public record SocialRevokeCommand(
    Long hostId,
    SocialProvider provider,
    String userId,
    String refreshToken
) {
    public SocialRevokeCommand(Long hostId, OauthHost oauthHost) {
        this(
            hostId,
            oauthHost.getProvider(),
            oauthHost.getUserId(),
            oauthHost.getRefreshToken()
        );
    }
}
