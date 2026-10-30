package com.forgather.domain.notification.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.notification.model.Notification;
import com.forgather.domain.notification.repository.NotificationRepository;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ConflictException;

import lombok.RequiredArgsConstructor;

/**
 * 알림 생성 계약. 전송(outbox·폴러·FCM)은 후속 이슈에서 다룬다.
 * 기능 도메인은 자기 {@code @Transactional} 안에서 {@link #create(NotificationCommand)}를 직접 호출한다.
 * 의존 방향은 기능 도메인 → notification → host이며, 이 서비스는 호출자를 모른다.
 */
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 종류의 템플릿으로 제목·본문을 렌더링해 알림 한 건을 저장한다.
     * dedupeKey가 있으면 같은 키의 알림이 이미 있을 때 {@link ConflictException}을 던진다.
     * <p>
     * 이 예외는 {@code noRollbackFor}로 지정되어 있어, 선조회에서 중복이 걸린 경우에는 호출자 트랜잭션을
     * rollback-only로 표시하지 않는다. 스케줄 재실행처럼 예상된 중복은 호출자가 예외를 잡고 다음 대상으로 넘어갈 수 있다.
     * <p>
     * 단, 선조회를 통과한 두 트랜잭션이 동시에 저장해 UNIQUE 위반이 난 경우에는 리포지토리 프록시가 이미
     * 호출자 트랜잭션을 rollback-only로 표시한 뒤이므로, 예외를 잡고 계속 진행하면 커밋 시
     * {@code UnexpectedRollbackException}이 난다. 여러 알림을 한 작업에서 만드는 호출자는
     * 알림마다 트랜잭션을 나누는 것이 안전하다.
     */
    @Transactional(noRollbackFor = ConflictException.class)
    public Notification create(NotificationCommand command) {
        if (command == null) {
            throw new BaseNullPointerException("알림 생성 요청은 null일 수 없습니다.");
        }
        Notification notification = Notification.of(
            command.recipient(),
            command.type(),
            command.target(),
            command.dedupeKey(),
            command.templateArgs().toArray()
        );
        if (notification.getDedupeKey() == null) {
            return notificationRepository.save(notification);
        }
        return saveWithDedupeKey(notification);
    }

    private Notification saveWithDedupeKey(Notification notification) {
        String dedupeKey = notification.getDedupeKey();
        // 예상된 중복(스케줄 재실행)은 INSERT를 시도하지 않고 선조회로 거른다. 경합은 아래 UNIQUE 위반으로 잡는다.
        if (notificationRepository.existsByDedupeKey(dedupeKey)) {
            throw alreadyCreated(dedupeKey);
        }
        try {
            return notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException e) {
            throw alreadyCreated(dedupeKey);
        }
    }

    private ConflictException alreadyCreated(String dedupeKey) {
        return new ConflictException("이미 생성된 알림입니다. dedupeKey: " + dedupeKey);
    }
}
