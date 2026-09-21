package com.forgather.domain.host.service;

import com.forgather.domain.host.model.OauthHost;
import com.forgather.global.external.social.SocialProvider;

public record SocialRevokePayload(
    Long hostId,
    SocialProvider provider,
    String userId,
    String refreshToken
) {
    public SocialRevokePayload(Long hostId, OauthHost oauthHost) {
        this(
            hostId,
            oauthHost.getProvider(),
            oauthHost.getUserId(),
            oauthHost.getRefreshToken()
        );
    }
}
