package com.forgather.domain.notification.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.DevicePlatform;
import com.forgather.domain.notification.model.NotificationDevice;
import com.forgather.domain.notification.repository.NotificationDeviceRepository;
import com.forgather.global.exception.ConflictException;
import com.forgather.global.exception.ConstraintViolations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기기에 속한 푸시 토큰을 관리한다. 다른 호스트가 재등록하면 소유자를 변경한다.
 * 해제 시 행을 물리 삭제하며, 해제한 토큰은 재등록 시 새로 저장한다.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NotificationDeviceService {

    private final NotificationDeviceRepository notificationDeviceRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * 같은 토큰이 있으면 소유자·플랫폼·활동 시각을 갱신하고, 없으면 저장한다.
     * 호출자와 독립된 트랜잭션에서 실행하며, 토큰 UNIQUE 또는 삭제·갱신 경합은 한 번 재시도한다.
     * 실패한 트랜잭션은 rollback-only이므로 종료한 뒤 새 트랜잭션에서 재조회한다.
     * 재시도도 실패하면 예외를 그대로 전파한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void register(Host host, DevicePlatform platform, String token) {
        try {
            transactionTemplate.executeWithoutResult(status -> registerOnce(host, platform, token));
        } catch (ConflictException | ObjectOptimisticLockingFailureException e) {
            log.info("디바이스 등록 경합으로 새 트랜잭션에서 한 번 더 시도합니다. hostId: {}, cause: {}",
                host.getId(), e.getClass().getSimpleName());
            transactionTemplate.executeWithoutResult(status -> registerOnce(host, platform, token));
        }
    }

    /**
     * 템플릿이 연 트랜잭션에서 등록을 시도한다.
     * 토큰 UNIQUE 위반만 재시도 대상인 {@link ConflictException}으로 변환한다.
     */
    private void registerOnce(Host host, DevicePlatform platform, String token) {
        NotificationDevice device = notificationDeviceRepository.findByToken(token).orElse(null);
        if (device != null) {
            device.reassign(host, platform);
            return;
        }
        try {
            notificationDeviceRepository.saveAndFlush(new NotificationDevice(host, platform, token));
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.isViolationOf(e, NotificationDevice.UK_TOKEN)) {
                throw new ConflictException("디바이스 등록이 겹쳤습니다. 다시 시도해 주세요.", e);
            }
            throw e;
        }
    }

    /**
     * 호스트 본인의 토큰만 삭제한다. 멱등이다.
     * 토큰이 없거나 다른 호스트 소유면 아무것도 하지 않는다 (소유 여부를 응답으로 드러내지 않는다).
     */
    @Transactional
    public void deregister(Host host, String token) {
        notificationDeviceRepository.deleteByTokenAndHost(token, host);
    }

    /**
     * 호스트의 디바이스를 전부 삭제한다 (탈퇴용).
     *
     * @return 삭제된 행 수
     */
    @Transactional
    public int deleteAllByHost(Host host) {
        return notificationDeviceRepository.deleteAllByHost(host);
    }

    /**
     * 전송 대상 조회. 전송 폴러(후속 이슈)가 사용한다.
     */
    public List<NotificationDevice> findAllByHost(Host host) {
        return notificationDeviceRepository.findAllByHost(host);
    }
}
