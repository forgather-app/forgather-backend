package com.forgather.domain.stats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.forgather.domain.guestbook.repository.GuestBookCardCountRepository;
import com.forgather.domain.host.repository.HostCountRepository;
import com.forgather.domain.product.repository.ProductCountRepository;
import com.forgather.domain.space.repository.SpaceCountRepository;
import com.forgather.domain.stats.dto.LandingStatsResponse;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private SpaceCountRepository spaceCountRepository;

    @Mock
    private GuestBookCardCountRepository guestBookCardCountRepository;

    @Mock
    private HostCountRepository hostCountRepository;

    @Mock
    private ProductCountRepository productCountRepository;

    private StatsService statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsService(spaceCountRepository, guestBookCardCountRepository,
            hostCountRepository, productCountRepository);
    }

    @DisplayName("각 도메인의 누적 집계 값을 해당 랜딩 통계 응답에 담는다")
    @Test
    void landing() {
        // given
        when(spaceCountRepository.count()).thenReturn(42L);
        when(guestBookCardCountRepository.count()).thenReturn(1024L);
        when(hostCountRepository.count()).thenReturn(100L);
        when(productCountRepository.count()).thenReturn(200L);

        // when
        LandingStatsResponse result = statsService.landing();

        // then
        assertAll(
            () -> assertThat(result.spaceStats().spaceCount()).isEqualTo(42L),
            () -> assertThat(result.guestBookStats().cardCount()).isEqualTo(1024L),
            () -> assertThat(result.hostStats().hostCount()).isEqualTo(100L),
            () -> assertThat(result.productStats().productCount()).isEqualTo(200L)
        );
    }

    @DisplayName("모든 도메인의 누적 집계 값이 0이면 랜딩 통계도 모두 0이다")
    @Test
    void landingWithoutData() {
        // given
        when(spaceCountRepository.count()).thenReturn(0L);
        when(guestBookCardCountRepository.count()).thenReturn(0L);
        when(hostCountRepository.count()).thenReturn(0L);
        when(productCountRepository.count()).thenReturn(0L);

        // when
        LandingStatsResponse result = statsService.landing();

        // then
        assertAll(
            () -> assertThat(result.spaceStats().spaceCount()).isZero(),
            () -> assertThat(result.guestBookStats().cardCount()).isZero(),
            () -> assertThat(result.hostStats().hostCount()).isZero(),
            () -> assertThat(result.productStats().productCount()).isZero()
        );
    }
}
