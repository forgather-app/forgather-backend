package com.forgather.domain.stats.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LandingStatsResponse(
    @Schema(description = "스페이스 통계")
    LandingSpaceStats spaceStats,

    @Schema(description = "방명록 통계")
    LandingGuestBookStats guestBookStats,

    @Schema(description = "회원 통계")
    LandingHostStats hostStats,

    @Schema(description = "작품 통계")
    LandingProductStats productStats
) {

    public LandingStatsResponse(long spaceCount, long guestBookCardCount, long hostCount, long productCount) {
        this(
            new LandingSpaceStats(spaceCount),
            new LandingGuestBookStats(guestBookCardCount),
            new LandingHostStats(hostCount),
            new LandingProductStats(productCount)
        );
    }

    public record LandingSpaceStats(
        @Schema(description = "누적 스페이스 수", example = "42")
        long spaceCount
    ) {
    }

    public record LandingGuestBookStats(
        @Schema(description = "누적 방명록 카드 수", example = "1024")
        long cardCount
    ) {
    }

    public record LandingHostStats(
        @Schema(description = "누적 회원 수", example = "100")
        long hostCount
    ) {
    }

    public record LandingProductStats(
        @Schema(description = "누적 작품 수", example = "200")
        long productCount
    ) {
    }
}
