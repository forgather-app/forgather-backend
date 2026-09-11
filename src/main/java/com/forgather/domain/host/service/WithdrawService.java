package com.forgather.domain.host.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.exhibition.service.ExhibitionService;
import com.forgather.domain.host.model.AppleHost;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.model.OauthHost;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.domain.space.service.SpaceService;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final KakaoHostRepository kakaoHostRepository;
    private final AppleHostRepository appleHostRepository;
    private final HostProfilePhotoRepository hostProfilePhotoRepository;
    private final HostRepository hostRepository;
    private final SpaceService spaceService;
    private final ExhibitionService exhibitionService;
    private final OutboxService outboxService;

    @Transactional
    public void withdraw(Host loginHost) {
        Host host = hostRepository.getByIdOrThrow(loginHost.getId());
        OauthHost oauthHost = getOauthHost(host);
        deleteHost(host, oauthHost);
        if (oauthHost != null) {
            outboxService.save(OutboxType.SOCIAL_REVOKE, new SocialRevokeCommand(host.getId(), oauthHost));
        }
    }

    private OauthHost getOauthHost(Host host) {
        KakaoHost kakaoHost = kakaoHostRepository.findByHost(host).orElse(null);
        if (kakaoHost != null) {
            return kakaoHost;
        }

        AppleHost appleHost = appleHostRepository.findByHost(host).orElse(null);
        if (appleHost != null) {
            return appleHost;
        }

        return null;
    }

    private void deleteHost(Host host, OauthHost oauthHost) {
        deleteOauthHost(oauthHost);
        hostProfilePhotoRepository.findByHostAndDeletedAtIsNull(host).ifPresent(HostProfilePhoto::delete);
        spaceService.deleteAllByHost(host);
        exhibitionService.deleteAllByHost(host);
        host.delete();
    }

    private void deleteOauthHost(OauthHost oauthHost) {
        switch (oauthHost) {
            case null -> {}
            case KakaoHost kakaoHost -> kakaoHostRepository.delete(kakaoHost);
            case AppleHost appleHost -> appleHostRepository.delete(appleHost);
        }
    }
}
