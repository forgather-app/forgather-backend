package com.forgather.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LandingStatsResponse(
    @Schema(description = "스페이스 통계")
    LandingSpaceStats spaceStats,

    @Schema(description = "방명록 통계")
    LandingGuestBookStats guestBookStats
) {

    public LandingStatsResponse(long spaceCount, long guestBookCardCount) {
        this(
            new LandingSpaceStats(spaceCount),
            new LandingGuestBookStats(guestBookCardCount)
        );
    }

    public record LandingSpaceStats(
        @Schema(description = "총 스페이스 수", example = "42")
        long spaceCount
    ) {
    }

    public record LandingGuestBookStats(
        @Schema(description = "총 방명록 카드 수", example = "1024")
        long cardCount
    ) {
    }
}
