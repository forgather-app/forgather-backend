package com.forgather.global.auth.service;

import org.springframework.stereotype.Service;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final KakaoHostRepository kakaoHostRepository;
    private final AppleHostRepository appleHostRepository;
    private final SocialRevokeService socialRevokeService;
    private final HostDeleteService hostDeleteService;

    /**
     * 소셜 연결 해제는 외부 API 호출이므로 삭제 트랜잭션 밖에서 먼저 수행한다.
     * 해제 실패는 실패 로그로 기록되고 탈퇴는 계속 진행된다.
     */
    public void withdraw(Host host) {
        revokeSocialConnections(host);
        hostDeleteService.delete(host.getId());
    }

    private void revokeSocialConnections(Host host) {
        kakaoHostRepository.findByHost(host)
            .ifPresent(kakaoHost -> socialRevokeService.revokeKakao(kakaoHost.getUserId()));
        appleHostRepository.findByHost(host)
            .ifPresent(appleHost -> socialRevokeService.revokeApple(
                appleHost.getUserId(), appleHost.getRefreshToken()));
    }
}
