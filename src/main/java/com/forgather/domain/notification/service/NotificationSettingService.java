package com.forgather.domain.notification.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.dto.NotificationSettingResponse;
import com.forgather.domain.notification.model.NotificationSetting;
import com.forgather.domain.notification.repository.NotificationSettingRepository;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ConflictException;
import com.forgather.global.exception.ConstraintViolations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 호스트별 알림 설정을 관리한다. 행이 없으면 허용으로 간주하고, 행은 설정을 처음 변경할 때 만든다.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * 설정을 조회한다. 행이 없으면 기본값(허용)을 돌려주며 행을 만들지 않는다.
     */
    public NotificationSettingResponse getSetting(Host host) {
        return new NotificationSettingResponse(isEnabled(host));
    }

    /**
     * 행이 없으면 허용으로 간주한다. 설정 조회 API와 알림 생성 게이트(리스너)가 함께 쓴다.
     * 공지 fan-out처럼 SQL로 대상을 거르는 경로도 이 판정 규칙("행이 없거나 enabled = 1")과 같아야 한다.
     */
    public boolean isEnabled(Host host) {
        validateHost(host);
        return notificationSettingRepository.findByHost(host)
            .map(NotificationSetting::isEnabled)
            .orElse(true);
    }

    /**
     * 행이 있으면 갱신하고, 없으면 저장한다. 호출자와 독립된 트랜잭션에서 실행한다.
     * 같은 호스트의 동시 최초 저장으로 host_id UNIQUE 위반이 나면, rollback-only가 된 트랜잭션을 끝내고
     * 새 트랜잭션에서 한 번 더 시도한다(두 번째 시도는 재조회 뒤 갱신으로 끝난다). 재시도도 실패하면 예외를 그대로 전파한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public NotificationSettingResponse update(Host host, boolean enabled) {
        validateHost(host);
        try {
            transactionTemplate.executeWithoutResult(status -> updateOnce(host, enabled));
        } catch (ConflictException e) {
            log.info("알림 설정 최초 저장이 겹쳐 새 트랜잭션에서 한 번 더 시도합니다. hostId: {}", host.getId());
            transactionTemplate.executeWithoutResult(status -> updateOnce(host, enabled));
        }
        return new NotificationSettingResponse(enabled);
    }

    /**
     * 템플릿이 연 트랜잭션에서 저장 또는 갱신을 시도한다.
     * 존재 여부는 비잠금 조회로 확인하고, 갱신은 변경 감지가 아니라 명시적 UPDATE로 해 마지막에 커밋한 값을 남긴다.
     * host_id UNIQUE 위반만 재시도 대상인 {@link ConflictException}으로 변환한다.
     */
    private void updateOnce(Host host, boolean enabled) {
        if (notificationSettingRepository.findByHost(host).isPresent()) {
            notificationSettingRepository.updateEnabledByHost(host, enabled, LocalDateTime.now());
            return;
        }
        try {
            notificationSettingRepository.saveAndFlush(new NotificationSetting(host, enabled));
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.isViolationOf(e, NotificationSetting.UK_HOST_ID)) {
                throw new ConflictException("알림 설정 저장이 겹쳤습니다. 다시 시도해 주세요.", e);
            }
            throw e;
        }
    }

    private void validateHost(Host host) {
        if (host == null) {
            throw new BaseNullPointerException("알림 설정의 호스트는 null일 수 없습니다.");
        }
    }
}
