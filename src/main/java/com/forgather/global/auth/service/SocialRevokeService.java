package com.forgather.global.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.global.auth.client.AppleAuthClient;
import com.forgather.global.auth.client.KakaoUnlinkClient;
import com.forgather.global.auth.model.SocialRevokeFailLog;
import com.forgather.global.auth.repository.SocialRevokeFailLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialRevokeService {

    private final KakaoUnlinkClient kakaoUnlinkClient;
    private final AppleAuthClient appleAuthClient;
    private final SocialRevokeFailLogRepository socialRevokeFailLogRepository;

    /**
     * 실패해도 예외를 전파하지 않는다. 탈퇴는 소셜 연결 해제 실패와 무관하게 진행되어야 한다.
     */
    public void revokeKakao(String userId) {
        try {
            kakaoUnlinkClient.unlink(userId);
        } catch (Exception e) {
            log.warn("Kakao unlink 실패. userId: {}", userId, e);
            socialRevokeFailLogRepository.save(SocialRevokeFailLog.kakao(userId));
        }
    }

    /**
     * 실패해도 예외를 전파하지 않는다. Apple refresh token은 재시도를 위해 실패 로그에 보관한다.
     */
    public void revokeApple(String userId, String refreshToken) {
        try {
            appleAuthClient.revoke(refreshToken);
        } catch (Exception e) {
            log.warn("Apple token revoke 실패. userId: {}", userId, e);
            socialRevokeFailLogRepository.save(SocialRevokeFailLog.apple(userId, refreshToken));
        }
    }

    @Transactional
    public void retryFailedRevokes() {
        List<SocialRevokeFailLog> failLogs = socialRevokeFailLogRepository.findAllByCompletedAtIsNull();
        for (SocialRevokeFailLog failLog : failLogs) {
            retry(failLog);
        }
    }

    private void retry(SocialRevokeFailLog failLog) {
        try {
            switch (failLog.getProvider()) {
                case KAKAO -> kakaoUnlinkClient.unlink(failLog.getSocialUserId());
                case APPLE -> appleAuthClient.revoke(failLog.getAppleRefreshToken());
                default -> throw new IllegalStateException("지원하지 않는 provider입니다. provider: " + failLog.getProvider());
            }
            failLog.complete();
        } catch (Exception e) {
            failLog.increaseFailCount();
            log.warn("소셜 연결 해제 재시도 실패. provider: {}, socialUserId: {}, failCount: {}",
                failLog.getProvider(), failLog.getSocialUserId(), failLog.getFailCount(), e);
        }
    }
}
