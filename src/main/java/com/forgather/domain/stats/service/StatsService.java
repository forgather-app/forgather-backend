package com.forgather.domain.stats.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.stats.dto.LandingStatsResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class StatsService {

    private final SpaceRepository spaceRepository;
    private final GuestBookCardRepository guestBookCardRepository;
    private final HostRepository hostRepository;
    private final ProductRepository productRepository;

    public LandingStatsResponse landing() {
        long spaceCount = spaceRepository.count();
        long guestBookCardCount = guestBookCardRepository.count();
        long hostCount = hostRepository.count();
        long productCount = productRepository.count();
        return new LandingStatsResponse(spaceCount, guestBookCardCount, hostCount, productCount);
    }
}
