package com.forgather.domain.host.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.model.Photo;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.model.Host;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostAnonymizeService {

    private final HostRepository hostRepository;
    private final HostPhotoFinder hostPhotoFinder;

    /**
     * 탈퇴 회원의 개인정보와 사진 원본 파일명을 하나의 트랜잭션에서 익명화한다.
     * 익명화된 행은 유지되어 count() 기반 누적 통계가 보존된다.
     * 대상 목록 조회와 별도 트랜잭션에서 실행되므로 detached 엔티티 대신 id로 재조회한다.
     */
    @Transactional
    public void anonymize(Long hostId, LocalDateTime now) {
        Host host = hostRepository.getByIdOrThrow(hostId);
        hostPhotoFinder.findAllIncludingDeleted(host)
            .forEach(Photo::anonymize);
        host.anonymize(now);
    }
}
