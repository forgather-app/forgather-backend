package com.forgather.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
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
    private final HostRepository hostRepository;
    private final EntityManager entityManager;

    @Autowired
    public NotificationServiceTest(
        NotificationService notificationService,
        NotificationJpaRepository notificationJpaRepository,
        HostRepository hostRepository,
        EntityManager entityManager
    ) {
        this.notificationService = notificationService;
        this.notificationJpaRepository = notificationJpaRepository;
        this.hostRepository = hostRepository;
        this.entityManager = entityManager;
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
        Mockito.when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        NotificationService service = new NotificationService(repository);
        NotificationCommand command =
            NotificationFixture.createCommandWithHostAndDedupeKey(HostFixture.createHostWithId(1L), "race-key");

        // when & then
        assertThatThrownBy(() -> service.create(command))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("race-key");
    }
}
