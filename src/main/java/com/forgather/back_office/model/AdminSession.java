package com.forgather.back_office.model;

import java.time.LocalDateTime;
import java.util.Objects;

import com.forgather.global.util.RandomCodeGenerator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class AdminSession {

    private static final int DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS = 30 * 60;  // 30분

    private final SessionId sessionId;
    private final Long adminUserId;
    private final String username;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastAccessedAt;
    private final int maxInactiveIntervalSeconds;  // 초 단위

    public static AdminSession create(Long adminUserId, String username, RandomCodeGenerator randomCodeGenerator) {
        LocalDateTime now = LocalDateTime.now();
        return new AdminSession(
            SessionId.generate(randomCodeGenerator),
            adminUserId,
            username,
            now,
            now,
            DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS
        );
    }

    public boolean isExpired(LocalDateTime standardDateTime) {
        LocalDateTime expirationTime = lastAccessedAt.plusSeconds(maxInactiveIntervalSeconds);
        return expirationTime.isBefore(standardDateTime);
    }

    public AdminSession refresh() {
        return new AdminSession(
            this.sessionId,
            this.adminUserId,
            this.username,
            this.createdAt,
            LocalDateTime.now(),
            this.maxInactiveIntervalSeconds
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AdminSession that))
            return false;
        return Objects.equals(getSessionId(), that.getSessionId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSessionId());
    }
}
