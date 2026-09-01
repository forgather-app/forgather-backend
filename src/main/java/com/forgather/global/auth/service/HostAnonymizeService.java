package com.forgather.global.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HostAnonymizeService {

    private static final int RETENTION_DAYS = 30;

    private final HostRepository hostRepository;

    /**
     * 탈퇴 후 30일이 지난 회원의 개인정보를 익명화하고 처리 건수를 반환한다.
     * 익명화된 행은 유지되어 count() 기반 누적 통계가 보존된다.
     */
    @Transactional
    public int anonymizeExpiredHosts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<Host> hosts = hostRepository.findAllByDeletedAtBeforeAndAnonymizedAtIsNull(threshold);
        log.info("탈퇴 회원 익명화 시작");
        for (Host host : hosts) {
            host.anonymize();
        }
        return hosts.size();
    }
}
