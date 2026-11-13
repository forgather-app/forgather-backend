package com.forgather.domain.notification.service;

import static com.forgather.fixture.ConstraintViolationFixture.foreignKeyViolation;
import static com.forgather.fixture.ConstraintViolationFixture.uniqueViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.notification.dto.NotificationResponse;
import com.forgather.domain.notification.dto.NotificationsResponse;
import com.forgather.domain.notification.model.Notification;
import com.forgather.domain.notification.model.NotificationTargetType;
import com.forgather.domain.notification.model.NotificationType;
import com.forgather.domain.notification.repository.NotificationRepository;
import com.forgather.domain.notification.repository.jpa.NotificationJpaRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ConflictException;

import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class NotificationServiceTest extends TestOnContainer {

    private final NotificationService notificationService;
    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationRepository notificationRepository;
    private final HostRepository hostRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public NotificationServiceTest(
        NotificationService notificationService,
        NotificationJpaRepository notificationJpaRepository,
        NotificationRepository notificationRepository,
        HostRepository hostRepository,
        EntityManager entityManager,
        JdbcTemplate jdbcTemplate
    ) {
        this.notificationService = notificationService;
        this.notificationJpaRepository = notificationJpaRepository;
        this.notificationRepository = notificationRepository;
        this.hostRepository = hostRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    private Host saveHost() {
        return hostRepository.save(HostFixture.createHost());
    }

    private Notification saveNotification(Host host) {
        return notificationRepository.save(NotificationFixture.createNotificationWithHost(host));
    }

    /**
     * created_at은 초 단위라 한 테스트에서 저장한 알림은 대부분 같은 값을 가진다. 정렬 검증을 위해 SQL로 직접 바꾼다.
     * 테스트 트랜잭션의 연결을 공유하므로 먼저 flush한다.
     */
    private void updateCreatedAt(Notification notification, LocalDateTime createdAt) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE notification SET created_at = ? WHERE id = ?", createdAt, notification.getId());
    }

    private void updateReadAt(Notification notification, LocalDateTime readAt) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE notification SET read_at = ? WHERE id = ?", readAt, notification.getId());
    }

    /**
     * 벌크 UPDATE 결과는 영속성 컨텍스트에 반영되지 않으므로 SQL로 확인한다.
     */
    private LocalDateTime readAtOf(Notification notification) {
        return jdbcTemplate.queryForObject(
            "SELECT read_at FROM notification WHERE id = ?", LocalDateTime.class, notification.getId());
    }

    private LocalDateTime updatedAtOf(Notification notification) {
        return jdbcTemplate.queryForObject(
            "SELECT updated_at FROM notification WHERE id = ?", LocalDateTime.class, notification.getId());
    }

    @DisplayName("알림을 생성하면 종류의 템플릿으로 렌더링한 제목과 본문이 저장된다")
    @Test
    void createRendersTitleAndBody() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        NotificationCommand command = NotificationFixture.createCommandWithHost(host);

        // when
        Notification created = notificationService.create(command);

        // then
        entityManager.flush();
        entityManager.clear();
        Notification saved = notificationJpaRepository.findById(created.getId()).orElseThrow();
        assertAll(
            () -> assertThat(saved.getHost().getId()).isEqualTo(host.getId()),
            () -> assertThat(saved.getType()).isEqualTo(NotificationType.PLACEHOLDER),
            () -> assertThat(saved.getTitle()).isEqualTo("임시 알림"),
            () -> assertThat(saved.getBody()).isEqualTo("임시 알림 본문 " + NotificationFixture.TEMPLATE_ARG),
            () -> assertThat(saved.getTargetType()).isEqualTo(NotificationTargetType.SPACE),
            () -> assertThat(saved.getTargetKey()).isEqualTo(NotificationFixture.SPACE_CODE),
            () -> assertThat(saved.getDedupeKey()).isNull(),
            () -> assertThat(saved.getReadAt()).isNull(),
            () -> assertThat(saved.getSentAt()).isNull(),
            () -> assertThat(saved.getDeletedAt()).isNull()
        );
    }

    @DisplayName("dedupeKey가 있는 알림은 저장 후 같은 키로 다시 조회된다")
    @Test
    void createWithDedupeKey() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        NotificationCommand command = NotificationFixture.createCommandWithHostAndDedupeKey(host, "digest:abc:2026-10-25");

        // when
        Notification created = notificationService.create(command);

        // then
        assertAll(
            () -> assertThat(created.getId()).isNotNull(),
            () -> assertThat(created.getDedupeKey()).isEqualTo("digest:abc:2026-10-25"),
            () -> assertThat(notificationJpaRepository.existsByDedupeKey("digest:abc:2026-10-25")).isTrue()
        );
    }

    @DisplayName("같은 dedupeKey로 알림을 다시 생성하면 409 충돌 예외가 발생한다")
    @Test
    void rejectDuplicateDedupeKey() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String dedupeKey = "digest:abc:2026-10-25";
        notificationService.create(NotificationFixture.createCommandWithHostAndDedupeKey(host, dedupeKey));

        // when & then
        assertThatThrownBy(() -> notificationService.create(
            NotificationFixture.createCommandWithHostAndDedupeKey(host, dedupeKey)))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining(dedupeKey);
    }

    @DisplayName("dedupeKey가 null인 알림은 같은 내용으로 여러 번 생성할 수 있다")
    @Test
    void allowDuplicateWithoutDedupeKey() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        Notification first = notificationService.create(NotificationFixture.createCommandWithHost(host));
        Notification second = notificationService.create(NotificationFixture.createCommandWithHost(host));

        // then
        entityManager.flush();
        assertAll(
            () -> assertThat(first.getId()).isNotEqualTo(second.getId()),
            () -> assertThat(notificationJpaRepository.count()).isEqualTo(2)
        );
    }

    @DisplayName("템플릿 인자가 부족하면 알림을 생성할 수 없다")
    @Test
    void rejectMissingTemplateArgs() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        NotificationCommand command = new NotificationCommand(host, NotificationType.PLACEHOLDER, null, null, null);

        // when & then
        assertThatThrownBy(() -> notificationService.create(command))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("알림 템플릿 인자가 올바르지 않습니다.");
    }

    @DisplayName("생성 요청이 null이면 알림을 생성할 수 없다")
    @Test
    void rejectNullCommand() {
        // when & then
        assertThatThrownBy(() -> notificationService.create(null))
            .isInstanceOf(BaseNullPointerException.class);
    }

    /**
     * 선조회를 통과한 두 트랜잭션이 동시에 INSERT하는 경합은 단일 트랜잭션 테스트로 재현할 수 없어
     * 리포지토리를 대체해 UNIQUE 위반 경로만 검증한다.
     */
    @DisplayName("선조회 이후 동시 생성으로 UNIQUE 위반이 나면 409 충돌 예외로 변환한다")
    @Test
    void translateUniqueViolationToConflict() {
        // given
        NotificationRepository repository = Mockito.mock(NotificationRepository.class);
        Mockito.when(repository.existsByDedupeKey(anyString())).thenReturn(false);
        Mockito.when(repository.saveAndFlush(any()))
            .thenThrow(uniqueViolation("notification." + Notification.UK_DEDUPE_KEY));
        NotificationService service = new NotificationService(repository);
        NotificationCommand command =
            NotificationFixture.createCommandWithHostAndDedupeKey(HostFixture.createHostWithId(1L), "race-key");

        // when & then
        assertThatThrownBy(() -> service.create(command))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("race-key")
            .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("dedupe_key가 아닌 제약 위반은 409로 바꾸지 않고 원본 예외를 그대로 전파한다")
    @Test
    void propagateOtherConstraintViolation() {
        // given
        NotificationRepository repository = Mockito.mock(NotificationRepository.class);
        Mockito.when(repository.existsByDedupeKey(anyString())).thenReturn(false);
        DataIntegrityViolationException violation = foreignKeyViolation("notification.FK_host_TO_notification");
        Mockito.when(repository.saveAndFlush(any())).thenThrow(violation);
        NotificationService service = new NotificationService(repository);
        NotificationCommand command =
            NotificationFixture.createCommandWithHostAndDedupeKey(HostFixture.createHostWithId(1L), "race-key");

        // when & then
        assertThatThrownBy(() -> service.create(command)).isSameAs(violation);
    }

    @DisplayName("알림함 목록은 요청 정렬과 관계없이 생성일 내림차순으로 조회된다")
    @Test
    void getNotificationsOrderedByCreatedAtDesc() {
        // given
        Host host = saveHost();
        Notification oldest = saveNotification(host);
        Notification newest = saveNotification(host);
        Notification middle = saveNotification(host);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        updateCreatedAt(oldest, now.minusDays(3));
        updateCreatedAt(newest, now.minusDays(1));
        updateCreatedAt(middle, now.minusDays(2));
        entityManager.clear();

        // when
        NotificationsResponse response = notificationService.getNotifications(
            host, PageRequest.of(0, 15, Sort.by(Sort.Direction.ASC, "id")));

        // then
        assertThat(response.notifications())
            .extracting(NotificationResponse::id)
            .containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    @DisplayName("생성일이 같은 알림은 id 내림차순으로 조회된다")
    @Test
    void getNotificationsOrderedByIdDescWhenCreatedAtTies() {
        // given
        Host host = saveHost();
        Notification first = saveNotification(host);
        Notification second = saveNotification(host);
        LocalDateTime sameTime = LocalDateTime.now().minusHours(1).withNano(0);
        updateCreatedAt(first, sameTime);
        updateCreatedAt(second, sameTime);
        entityManager.clear();

        // when
        NotificationsResponse response = notificationService.getNotifications(host, PageRequest.of(0, 15));

        // then
        assertThat(response.notifications())
            .extracting(NotificationResponse::id)
            .containsExactly(second.getId(), first.getId());
    }

    @DisplayName("알림함 목록에는 삭제된 알림과 다른 호스트의 알림이 포함되지 않는다")
    @Test
    void getNotificationsExcludesDeletedAndOtherHosts() {
        // given
        Host host = saveHost();
        Host otherHost = saveHost();
        Notification visible = saveNotification(host);
        notificationRepository.save(NotificationFixture.createDeletedNotificationWithHost(host));
        saveNotification(otherHost);

        // when
        NotificationsResponse response = notificationService.getNotifications(host, PageRequest.of(0, 15));

        // then
        assertAll(
            () -> assertThat(response.notifications())
                .extracting(NotificationResponse::id)
                .containsExactly(visible.getId()),
            () -> assertThat(response.totalCount()).isEqualTo(1)
        );
    }

    @DisplayName("알림함 목록은 요청한 페이지 크기로 나누고 1부터 시작하는 페이지 정보를 돌려준다")
    @Test
    void getNotificationsWithPageInfo() {
        // given
        Host host = saveHost();
        for (int i = 0; i < 5; i++) {
            saveNotification(host);
        }

        // when
        NotificationsResponse response = notificationService.getNotifications(host, PageRequest.of(1, 2));

        // then
        assertAll(
            () -> assertThat(response.notifications()).hasSize(2),
            () -> assertThat(response.currentPage()).isEqualTo(2),
            () -> assertThat(response.pageSize()).isEqualTo(2),
            () -> assertThat(response.totalCount()).isEqualTo(5),
            () -> assertThat(response.totalPages()).isEqualTo(3)
        );
    }

    @DisplayName("읽지 않은 알림 개수에는 읽은 알림, 삭제된 알림, 다른 호스트의 알림이 포함되지 않는다")
    @Test
    void countUnreadExcludesReadDeletedAndOtherHosts() {
        // given
        Host host = saveHost();
        Host otherHost = saveHost();
        saveNotification(host);
        saveNotification(host);
        notificationRepository.save(NotificationFixture.createReadNotificationWithHost(host));
        notificationRepository.save(NotificationFixture.createDeletedNotificationWithHost(host));
        saveNotification(otherHost);

        // when
        long unreadCount = notificationService.countUnread(host).unreadCount();

        // then
        assertThat(unreadCount).isEqualTo(2);
    }

    @DisplayName("전체 읽음 처리는 호스트의 읽지 않은 알림만 읽음 시각과 수정 시각을 갱신한다")
    @Test
    void readAllUpdatesOnlyUnreadNotifications() {
        // given
        Host host = saveHost();
        Host otherHost = saveHost();
        Notification unread = saveNotification(host);
        Notification otherUnread = saveNotification(host);
        Notification alreadyRead =
            notificationRepository.save(NotificationFixture.createReadNotificationWithHost(host));
        Notification deleted =
            notificationRepository.save(NotificationFixture.createDeletedNotificationWithHost(host));
        Notification otherHostUnread = saveNotification(otherHost);
        LocalDateTime previousReadAt = LocalDateTime.now().minusDays(1).withNano(0);
        updateReadAt(alreadyRead, previousReadAt);

        // when
        int updatedCount = notificationService.readAll(host);

        // then
        assertAll(
            () -> assertThat(updatedCount).isEqualTo(2),
            () -> assertThat(readAtOf(unread)).isNotNull(),
            () -> assertThat(readAtOf(otherUnread)).isNotNull(),
            () -> assertThat(updatedAtOf(unread)).isEqualTo(readAtOf(unread)),
            () -> assertThat(readAtOf(alreadyRead)).isEqualTo(previousReadAt),
            () -> assertThat(readAtOf(deleted)).isNull(),
            () -> assertThat(readAtOf(otherHostUnread)).isNull()
        );
    }

    @DisplayName("전체 읽음 처리를 다시 호출하면 갱신되는 알림이 없다")
    @Test
    void readAllIsIdempotent() {
        // given
        Host host = saveHost();
        saveNotification(host);
        notificationService.readAll(host);

        // when
        int updatedCount = notificationService.readAll(host);

        // then
        assertThat(updatedCount).isZero();
    }

    /**
     * 전체 읽음은 호출 시점의 미읽음 전체가 대상이다. 목록 조회와 전체 읽음 사이에 들어온 알림도 읽음 처리되는 경계를 고정한다.
     */
    @DisplayName("목록을 조회한 뒤 들어온 알림도 전체 읽음 처리 대상이 된다")
    @Test
    void readAllIncludesNotificationsArrivedAfterListing() {
        // given
        Host host = saveHost();
        saveNotification(host);
        NotificationsResponse listed = notificationService.getNotifications(host, PageRequest.of(0, 15));
        Notification arrivedAfterListing = saveNotification(host);

        // when
        int updatedCount = notificationService.readAll(host);

        // then
        assertAll(
            () -> assertThat(listed.notifications()).hasSize(1),
            () -> assertThat(updatedCount).isEqualTo(2),
            () -> assertThat(readAtOf(arrivedAfterListing)).isNotNull(),
            () -> assertThat(notificationService.countUnread(host).unreadCount()).isZero()
        );
    }
}
