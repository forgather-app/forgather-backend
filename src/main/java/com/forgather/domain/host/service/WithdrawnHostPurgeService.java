package com.forgather.domain.host.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.forgather.domain.model.Photo;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.global.auth.model.Host;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawnHostPurgeService {

    public static final int WITHDRAWAL_RETENTION_DAYS = 30;

    private final HostRepository hostRepository;
    private final HostPhotoFinder hostPhotoFinder;
    private final ContentsStorage contentsStorage;
    private final HostAnonymizeService hostAnonymizeService;

    /**
     * 탈퇴 후 30일이 지난 회원의 저장소 사진을 파기하고 개인정보를 익명화한 뒤 처리 건수를 반환한다.
     * 의도적으로 전체를 하나의 트랜잭션으로 묶지 않는다.
     * S3 삭제는 외부 호출이라 DB 커넥션을 쥔 채 수행하면 안 되고, 호스트 1건 실패가 나머지 처리를 막아서도 안 된다.
     * 사진 삭제가 성공한 뒤에만 완료 마커(anonymizedAt)를 커밋하므로, 중간에 실패한 호스트는
     * 마커가 남지 않아 다음 스케줄에서 자동 재시도된다. S3 삭제는 멱등이라 재실행이 무해하다.
     */
    public int purgeExpiredHosts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusDays(WITHDRAWAL_RETENTION_DAYS);
        List<Host> hosts = hostRepository.findAllByDeletedAtBeforeAndAnonymizedAtIsNull(threshold);
        log.info("탈퇴 회원 purge 시작. 대상: {}건", hosts.size());
        int purgedCount = 0;
        for (Host host : hosts) {
            if (purge(host, now)) {
                purgedCount++;
            }
        }
        log.info("탈퇴 회원 purge 완료. 처리: {}건", purgedCount);
        return purgedCount;
    }

    /**
     * 실패해도 예외를 전파하지 않는다. 다음 스케줄에서 재시도되므로 warn 레벨로만 남긴다.
     */
    private boolean purge(Host host, LocalDateTime now) {
        try {
            List<Photo> photos = hostPhotoFinder.findAllIncludingDeleted(host);
            contentsStorage.deletePhotos(photos);
            hostAnonymizeService.anonymize(host.getId(), now);
            return true;
        } catch (Exception e) {
            log.warn("탈퇴 회원 purge 실패. hostId: {}", host.getId(), e);
            return false;
        }
    }
}
