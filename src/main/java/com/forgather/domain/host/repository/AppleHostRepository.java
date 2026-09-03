package com.forgather.domain.host.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.forgather.domain.host.model.AppleHost;
import com.forgather.domain.host.model.Host;
import com.forgather.global.exception.NotFoundException;

public interface AppleHostRepository extends JpaRepository<AppleHost, Long> {

    Optional<AppleHost> findByUserId(String userId);

    Optional<AppleHost> findByHost(Host host);

    default AppleHost getById(Long id) {
        return findById(id)
            .orElseThrow(() -> new NotFoundException("AppleHost를 찾을 수 없습니다. id: " + id));
    }
}
