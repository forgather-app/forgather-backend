package com.forgather.global.auth.model;

import java.time.LocalDateTime;

import com.forgather.domain.model.BaseTimeEntity;
import com.forgather.global.auth.client.SocialProvider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "social_revoke_fail_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialRevokeFailLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "social_user_id", nullable = false)
    private String socialUserId;

    @Column(name = "apple_refresh_token", length = 512)
    private String appleRefreshToken;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private SocialRevokeFailLog(SocialProvider provider, String socialUserId, String appleRefreshToken) {
        this.provider = provider;
        this.socialUserId = socialUserId;
        this.appleRefreshToken = appleRefreshToken;
        this.failCount = 1;
    }

    public static SocialRevokeFailLog kakao(String socialUserId) {
        return new SocialRevokeFailLog(SocialProvider.KAKAO, socialUserId, null);
    }

    public static SocialRevokeFailLog apple(String socialUserId, String appleRefreshToken) {
        return new SocialRevokeFailLog(SocialProvider.APPLE, socialUserId, appleRefreshToken);
    }

    public void increaseFailCount() {
        this.failCount++;
    }

    public void complete() {
        if (completedAt != null) {
            return;
        }
        this.appleRefreshToken = null;
        this.completedAt = LocalDateTime.now();
    }
}
