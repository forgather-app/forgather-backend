package com.forgather.domain.stats.dto;

public record LandingStatsResponse(LandingSpaceStats spaceStats, LandingGuestBookStats guestBookStats) {

    public LandingStatsResponse(long spaceCount, long guestBookCardCount) {
        this(
            new LandingSpaceStats(spaceCount),
            new LandingGuestBookStats(guestBookCardCount)
        );
    }

    static record LandingSpaceStats(long spaceCount) {
    }

    static record LandingGuestBookStats(long cardCount) {
    }
}
