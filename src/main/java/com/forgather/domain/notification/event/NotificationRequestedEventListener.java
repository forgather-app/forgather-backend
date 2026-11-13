package com.forgather.domain.notification.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.service.NotificationCommand;
import com.forgather.domain.notification.service.NotificationService;
import com.forgather.domain.notification.service.NotificationSettingService;
import com.forgather.global.exception.ConflictException;

import lombok.extern.slf4j.Slf4j;

/**
 * 기능 트랜잭션이 커밋된 뒤 알림을 만든다. 알림 생성·발송 실패가 기능의 실패로 전파되지 않게 하기 위한 경계다.
 * 알림 행과 그 발송 작업(outbox, 후속 이슈)은 이 리스너가 여는 트랜잭션 안에서 함께 커밋된다.
 * <p>
 * 단건 알림 생성은 모두 이 리스너를 거친다. 수신 호스트의 알림 설정 게이트가 여기에 있기 때문이다.
 */
@Slf4j
@Component
public class NotificationRequestedEventListener {

    private final NotificationService notificationService;
    private final NotificationSettingService notificationSettingService;
    private final TransactionTemplate transactionTemplate;

    public NotificationRequestedEventListener(
        NotificationService notificationService,
        NotificationSettingService notificationSettingService,
        PlatformTransactionManager transactionManager
    ) {
        this.notificationService = notificationService;
        this.notificationSettingService = notificationSettingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 요청 스레드에서 커밋 직후 동기로 실행한다. 비동기 실행은 배포 종료 시 대기 중인 작업을 잃을 수 있어 쓰지 않는다.
     * <p>
     * 반드시 새 트랜잭션(REQUIRES_NEW)에서 만든다.
     * 커밋 실패까지 잡기 위해 리스너에 {@code @Transactional}을 붙이지 않고 트랜잭션 바깥에서 예외를 처리한다.
     * <p>
     * 실패는 로그만 남기고 삼킨다. 기능은 이미 커밋됐고, 커밋 직후 장애로 인한 유실은 허용한다.
     * 트랜잭션 밖에서 발행하면 즉시 새 트랜잭션에서 만든다(fallbackExecution).
     * 수신 호스트가 알림 설정을 꺼 두었으면 같은 트랜잭션에서 확인한 뒤 만들지 않는다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(NotificationRequestedEvent event) {
        NotificationCommand command = event.command();
        try {
            transactionTemplate.executeWithoutResult(status -> createIfEnabled(command));
        } catch (ConflictException e) {
            log.info("이미 생성된 알림이라 건너뜁니다. type: {}, dedupeKey: {}", command.type(), command.dedupeKey());
        } catch (RuntimeException e) {
            log.error("알림 생성에 실패했습니다. 기능 트랜잭션은 커밋된 상태입니다. type: {}, hostId: {}",
                command.type(), command.recipient().getId(), e);
            // 유실되면 안 되는 알림 종류가 생기면 outbox 저장
        }
    }

    /**
     * 수신 호스트가 알림 설정을 꺼 두었으면 알림을 만들지 않는다.
     */
    private void createIfEnabled(NotificationCommand command) {
        Host recipient = command.recipient();
        if (!notificationSettingService.isEnabled(recipient)) {
            log.info("알림 설정이 꺼진 호스트라 알림을 만들지 않습니다. type: {}, hostId: {}",
                command.type(), recipient.getId());
            return;
        }
        notificationService.create(command);
    }
}
