package com.forgather.domain.notification.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.notification.dto.NotificationsResponse;
import com.forgather.domain.notification.dto.UnreadNotificationCountResponse;
import com.forgather.domain.notification.model.Notification;
import com.forgather.domain.notification.repository.NotificationRepository;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ConflictException;
import com.forgather.global.exception.ConstraintViolations;

import lombok.RequiredArgsConstructor;

/**
 * 알림 생성과 알림함 조회·읽음 처리. 전송(outbox·폴러·FCM)은 후속 이슈에서 다룬다.
 * <p>
 * 기능 도메인은 이 서비스를 자기 트랜잭션 안에서 직접 호출하지 않는다. 알림 생성·발송은 기능의 성공과 분리해야 하므로,
 * 기능 트랜잭션 안에서 {@link com.forgather.domain.notification.event.NotificationRequestedEvent}를 발행하면 커밋 뒤
 * {@link com.forgather.domain.notification.event.NotificationRequestedEventListener}가 별도 트랜잭션에서 {@link #create(NotificationCommand)}를 호출한다.
 * 알림 생성이 실패해도 기능은 롤백되지 않고, 기능이 롤백되면 알림을 만들지 않는다.
 * <p>
 * 단건 알림 생성은 모두 {@link com.forgather.domain.notification.event.NotificationRequestedEvent}를 발행해 리스너를 거친다.
 * 스케줄러도 같다. 트랜잭션 밖에서 발행하면 fallbackExecution으로 즉시 처리되고, 이벤트마다 새 트랜잭션이 열리며
 * 중복 키 충돌은 건너뛴다. 수신 호스트의 알림 설정 게이트가 리스너에 있으므로 {@link #create(NotificationCommand)}를
 * 직접 호출하면 설정을 우회한다. 직접 호출은 리스너와 테스트만 한다.
 * 호스트 전원 대상의 대량 생성(공지 fan-out)은 이 메서드 대신 {@code INSERT … SELECT}를 쓰고,
 * 그 WHERE에서 설정이 꺼진 호스트를 제외한다.
 * <p>
 * 의존 방향은 기능 도메인 → notification → host이며, 이 서비스는 호출자를 모른다.
 */
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 종류의 템플릿으로 제목·본문을 렌더링해 알림 한 건을 저장한다. 호출자가 연 트랜잭션에 참여한다.
     * dedupeKey가 있으면 같은 키의 알림이 이미 있을 때 {@link ConflictException}을 던진다.
     * <p>
     * 한 트랜잭션에서 여러 알림을 만드는 호출자를 위한 규칙:
     * 선조회로 걸러진 중복은 {@code noRollbackFor} 덕분에 트랜잭션을 rollback-only로 만들지 않으므로,
     * 예외를 잡고 다음 대상으로 넘어갈 수 있다. 선조회를 통과한 뒤 동시 저장으로 UNIQUE 위반이 나면
     * 리포지토리 프록시가 이미 rollback-only로 표시한 뒤라, 잡고 계속 진행하면 커밋 시
     * {@code UnexpectedRollbackException}이 난다. 그래서 알림마다 트랜잭션을 나누는 것이 안전하며,
     * 리스너가 이벤트마다 새 트랜잭션을 여는 이유이기도 하다.
     * <p>
     * UNIQUE 위반은 {@code UK_notification_dedupe_key} 위반일 때만 {@link ConflictException}으로 바꾼다.
     * {@code saveAndFlush}는 호출자 트랜잭션에 쌓인 다른 테이블의 변경도 함께 flush하므로,
     * 그 밖의 제약 위반은 원인이 가려지지 않도록 원본 예외를 그대로 전파한다.
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

    /**
     * 호스트의 알림함 목록. 삭제된 알림은 제외한다.
     * 요청 정렬은 무시하고 createdAt·id 내림차순으로 고정하며, 페이지 크기는 요청값을 쓴다.
     */
    public NotificationsResponse getNotifications(Host host, Pageable pageable) {
        PageRequest fixedSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return NotificationsResponse.from(notificationRepository.findAllByHostAndDeletedAtIsNull(host, fixedSort));
    }

    /**
     * 호스트의 읽지 않은 알림 개수. 삭제된 알림은 제외한다.
     */
    public UnreadNotificationCountResponse countUnread(Host host) {
        return new UnreadNotificationCountResponse(
            notificationRepository.countByHostAndReadAtIsNullAndDeletedAtIsNull(host));
    }

    /**
     * 호출 시점에 읽지 않은 호스트의 알림을 모두 읽음 처리한다. 멱등이다.
     * 벌크 UPDATE 한 문장만 실행하고 같은 트랜잭션에서 알림을 다시 읽지 않아 영속성 컨텍스트와 어긋나지 않는다.
     *
     * @return 갱신된 행 수
     */
    @Transactional
    public int readAll(Host host) {
        return notificationRepository.readAllByHost(host, LocalDateTime.now());
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
            if (ConstraintViolations.isViolationOf(e, Notification.UK_DEDUPE_KEY)) {
                throw alreadyCreated(dedupeKey, e);
            }
            throw e;
        }
    }

    private ConflictException alreadyCreated(String dedupeKey) {
        return new ConflictException(alreadyCreatedMessage(dedupeKey));
    }

    private ConflictException alreadyCreated(String dedupeKey, Throwable cause) {
        return new ConflictException(alreadyCreatedMessage(dedupeKey), cause);
    }

    private String alreadyCreatedMessage(String dedupeKey) {
        return "이미 생성된 알림입니다. dedupeKey: " + dedupeKey;
    }
}
