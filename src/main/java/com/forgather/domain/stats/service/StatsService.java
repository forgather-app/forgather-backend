package com.forgather.domain.stats.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.guestbook.repository.GuestBookCardCountRepository;
import com.forgather.domain.host.repository.HostCountRepository;
import com.forgather.domain.product.repository.ProductCountRepository;
import com.forgather.domain.space.repository.SpaceCountRepository;
import com.forgather.domain.stats.dto.LandingStatsResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class StatsService {

    private final SpaceCountRepository spaceCountRepository;
    private final GuestBookCardCountRepository guestBookCardCountRepository;
    private final HostCountRepository hostCountRepository;
    private final ProductCountRepository productCountRepository;

    public LandingStatsResponse landing() {
        long spaceCount = spaceCountRepository.count();
        long guestBookCardCount = guestBookCardCountRepository.count();
        long hostCount = hostCountRepository.count();
        long productCount = productCountRepository.count();
        return new LandingStatsResponse(spaceCount, guestBookCardCount, hostCount, productCount);
    }
}
