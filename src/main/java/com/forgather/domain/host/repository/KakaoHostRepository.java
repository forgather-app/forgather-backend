package com.forgather.domain.host.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.global.exception.NotFoundException;

public interface KakaoHostRepository extends JpaRepository<KakaoHost, Long> {

    Optional<KakaoHost> findByUserId(String userId);

    Optional<KakaoHost> findByHost(Host host);

    default KakaoHost getById(Long id) {
        return findById(id)
            .orElseThrow(() -> new NotFoundException("KakaoHost를 찾을 수 없습니다. id: " + id));
    }
}
