package com.forgather.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.model.DevicePlatform;
import com.forgather.domain.notification.model.NotificationDevice;
import com.forgather.domain.notification.repository.NotificationDeviceRepository;
import com.forgather.global.exception.ConflictException;

import lombok.RequiredArgsConstructor;

/**
 * 호스트의 푸시 디바이스 토큰 관리.
 * 토큰은 호스트가 아니라 기기에 속하므로, 같은 토큰을 다른 호스트가 등록하면 소유자가 바뀐다.
 */
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NotificationDeviceService {

    private final NotificationDeviceRepository notificationDeviceRepository;

    /**
     * 토큰을 등록한다. 멱등이다.
     * 같은 토큰이 이미 있으면(soft delete된 행 포함) 그 행의 소유 호스트·플랫폼·활동 시각을 갱신하고 복구한다.
     * 없으면 새로 저장한다. 같은 토큰을 두 요청이 동시에 처음 등록하면 한쪽은 UNIQUE 위반이 나며 409로 응답한다.
     */
    @Transactional
    public void register(Host host, DevicePlatform platform, String token) {
        NotificationDevice existing = notificationDeviceRepository.findByToken(token).orElse(null);
        if (existing != null) {
            existing.reassign(host, platform);
            return;
        }
        try {
            notificationDeviceRepository.saveAndFlush(new NotificationDevice(host, platform, token));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("디바이스 등록이 겹쳤습니다. 다시 시도해 주세요.");
        }
    }

    /**
     * 호스트 본인의 활성 토큰만 soft delete한다. 멱등이다.
     * 토큰이 없거나 다른 호스트 소유면 아무것도 하지 않는다 (소유 여부를 응답으로 드러내지 않는다).
     */
    @Transactional
    public void deregister(Host host, String token) {
        notificationDeviceRepository.findByTokenAndHostAndDeletedAtIsNull(token, host)
            .ifPresent(NotificationDevice::delete);
    }

    /**
     * 호스트의 활성 디바이스를 전부 soft delete한다 (탈퇴용).
     *
     * @return soft delete된 행 수
     */
    @Transactional
    public int deleteAllByHost(Host host) {
        return notificationDeviceRepository.softDeleteAllByHost(host, LocalDateTime.now());
    }

    /**
     * 전송 대상 조회. 전송 폴러(후속 이슈)가 사용한다.
     */
    public List<NotificationDevice> findActiveDevices(Host host) {
        return notificationDeviceRepository.findAllByHostAndDeletedAtIsNull(host);
    }
}
