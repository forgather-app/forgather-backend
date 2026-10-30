package com.forgather.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.notification.model.DevicePlatform;
import com.forgather.domain.notification.model.NotificationDevice;
import com.forgather.domain.notification.repository.NotificationDeviceRepository;
import com.forgather.domain.notification.repository.jpa.NotificationDeviceJpaRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationDeviceFixture;
import com.forgather.global.exception.ConflictException;

import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class NotificationDeviceServiceTest extends TestOnContainer {

    private static final String TOKEN = NotificationDeviceFixture.TOKEN;
    private static final String OTHER_TOKEN = NotificationDeviceFixture.OTHER_TOKEN;

    private final NotificationDeviceService notificationDeviceService;
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final NotificationDeviceJpaRepository notificationDeviceJpaRepository;
    private final HostRepository hostRepository;
    private final EntityManager entityManager;

    @Autowired
    public NotificationDeviceServiceTest(
        NotificationDeviceService notificationDeviceService,
        NotificationDeviceRepository notificationDeviceRepository,
        NotificationDeviceJpaRepository notificationDeviceJpaRepository,
        HostRepository hostRepository,
        EntityManager entityManager
    ) {
        this.notificationDeviceService = notificationDeviceService;
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.notificationDeviceJpaRepository = notificationDeviceJpaRepository;
        this.hostRepository = hostRepository;
        this.entityManager = entityManager;
    }

    private Host saveHost() {
        return hostRepository.save(HostFixture.createHost());
    }

    private NotificationDevice getByToken(String token) {
        return notificationDeviceRepository.findByToken(token).orElseThrow();
    }

    @DisplayName("새 토큰을 등록하면 호스트의 활성 디바이스로 저장된다")
    @Test
    void registerNewDevice() {
        // given
        Host host = saveHost();

        // when
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);

        // then
        List<NotificationDevice> devices = notificationDeviceService.findActiveDevices(host);
        assertThat(devices).hasSize(1);
        NotificationDevice device = devices.getFirst();
        assertAll(
            () -> assertThat(device.getHost().getId()).isEqualTo(host.getId()),
            () -> assertThat(device.getPlatform()).isEqualTo(DevicePlatform.IOS),
            () -> assertThat(device.getToken()).isEqualTo(TOKEN),
            () -> assertThat(device.getLastActiveAt()).isNotNull(),
            () -> assertThat(device.getDeletedAt()).isNull()
        );
    }

    @DisplayName("같은 호스트가 같은 토큰을 다시 등록하면 행이 늘지 않고 활동 시각만 갱신된다")
    @Test
    void reRegisterSameHostUpdatesLastActiveAt() {
        // given
        Host host = saveHost();
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);
        NotificationDevice device = getByToken(TOKEN);
        LocalDateTime staleTime = LocalDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(device, "lastActiveAt", staleTime);
        entityManager.flush();
        entityManager.clear();

        // when
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);

        // then
        // 1차 캐시가 아니라 DB에 반영된 값을 읽는다
        entityManager.flush();
        entityManager.clear();
        NotificationDevice reloaded = notificationDeviceJpaRepository.findById(device.getId()).orElseThrow();
        assertAll(
            () -> assertThat(notificationDeviceJpaRepository.count()).isEqualTo(1),
            () -> assertThat(reloaded.getLastActiveAt()).isAfter(staleTime)
        );
    }

    @DisplayName("다른 호스트가 같은 토큰을 등록하면 디바이스 소유자가 새 호스트로 바뀐다")
    @Test
    void reRegisterByOtherHostTransfersOwnership() {
        // given
        Host previousOwner = saveHost();
        Host newOwner = saveHost();
        notificationDeviceService.register(previousOwner, DevicePlatform.IOS, TOKEN);

        // when
        notificationDeviceService.register(newOwner, DevicePlatform.ANDROID, TOKEN);

        // then
        NotificationDevice device = getByToken(TOKEN);
        assertAll(
            () -> assertThat(notificationDeviceJpaRepository.count()).isEqualTo(1),
            () -> assertThat(device.getHost().getId()).isEqualTo(newOwner.getId()),
            () -> assertThat(device.getPlatform()).isEqualTo(DevicePlatform.ANDROID),
            () -> assertThat(notificationDeviceService.findActiveDevices(previousOwner)).isEmpty(),
            () -> assertThat(notificationDeviceService.findActiveDevices(newOwner)).hasSize(1)
        );
    }

    @DisplayName("soft delete된 토큰을 다시 등록하면 기존 행이 복구된다")
    @Test
    void reRegisterRestoresSoftDeletedDevice() {
        // given
        Host host = saveHost();
        NotificationDevice deleted = notificationDeviceRepository.save(
            NotificationDeviceFixture.createDevice(host, DevicePlatform.IOS, TOKEN));
        deleted.delete();
        entityManager.flush();

        // when
        notificationDeviceService.register(host, DevicePlatform.ANDROID, TOKEN);

        // then
        // 1차 캐시가 아니라 DB에 반영된 값을 읽는다
        entityManager.flush();
        entityManager.clear();
        NotificationDevice restored = notificationDeviceJpaRepository.findById(deleted.getId()).orElseThrow();
        assertAll(
            () -> assertThat(notificationDeviceJpaRepository.count()).isEqualTo(1),
            () -> assertThat(restored.getHost().getId()).isEqualTo(host.getId()),
            () -> assertThat(restored.getDeletedAt()).isNull(),
            () -> assertThat(restored.getPlatform()).isEqualTo(DevicePlatform.ANDROID),
            () -> assertThat(notificationDeviceService.findActiveDevices(host)).hasSize(1)
        );
    }

    @DisplayName("본인 토큰을 해제하면 디바이스가 soft delete된다")
    @Test
    void deregisterOwnDevice() {
        // given
        Host host = saveHost();
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);

        // when
        notificationDeviceService.deregister(host, TOKEN);

        // then
        assertAll(
            () -> assertThat(notificationDeviceService.findActiveDevices(host)).isEmpty(),
            () -> assertThat(getByToken(TOKEN).getDeletedAt()).isNotNull()
        );
    }

    @DisplayName("다른 호스트 소유의 토큰을 해제하면 아무 일도 일어나지 않는다")
    @Test
    void deregisterIgnoresOtherHostDevice() {
        // given
        Host owner = saveHost();
        Host other = saveHost();
        notificationDeviceService.register(owner, DevicePlatform.IOS, TOKEN);

        // when
        notificationDeviceService.deregister(other, TOKEN);

        // then
        assertAll(
            () -> assertThat(notificationDeviceService.findActiveDevices(owner)).hasSize(1),
            () -> assertThat(getByToken(TOKEN).getDeletedAt()).isNull()
        );
    }

    @DisplayName("존재하지 않는 토큰을 해제해도 예외가 발생하지 않는다")
    @Test
    void deregisterIgnoresUnknownToken() {
        // given
        Host host = saveHost();

        // when & then
        assertThatCode(() -> notificationDeviceService.deregister(host, "unknown-token"))
            .doesNotThrowAnyException();
    }

    @DisplayName("호스트의 디바이스를 모두 삭제하면 그 호스트의 활성 디바이스만 soft delete된다")
    @Test
    void deleteAllByHost() {
        // given
        Host host = saveHost();
        Host other = saveHost();
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);
        notificationDeviceService.register(host, DevicePlatform.ANDROID, OTHER_TOKEN);
        notificationDeviceService.register(other, DevicePlatform.IOS, "fcm-registration-token-0003");
        NotificationDevice device = getByToken(TOKEN);

        // when
        int deletedCount = notificationDeviceService.deleteAllByHost(host);

        // then
        // 벌크 UPDATE는 1차 캐시에 반영되지 않으므로 DB 값으로 다시 읽는다
        entityManager.refresh(device);
        assertAll(
            () -> assertThat(deletedCount).isEqualTo(2),
            () -> assertThat(notificationDeviceService.findActiveDevices(host)).isEmpty(),
            () -> assertThat(notificationDeviceService.findActiveDevices(other)).hasSize(1),
            () -> assertThat(device.getDeletedAt()).isNotNull(),
            () -> assertThat(device.getUpdatedAt()).isEqualTo(device.getDeletedAt())
        );
    }

    @DisplayName("활성 디바이스가 없는 호스트의 디바이스를 모두 삭제하면 0건을 돌려준다")
    @Test
    void deleteAllByHostWithoutDevices() {
        // given
        Host host = saveHost();

        // when
        int deletedCount = notificationDeviceService.deleteAllByHost(host);

        // then
        assertThat(deletedCount).isZero();
    }

    /**
     * 같은 토큰의 동시 최초 등록은 단일 트랜잭션 테스트로 재현할 수 없어
     * 리포지토리를 대체해 UNIQUE 위반 경로만 검증한다.
     */
    @DisplayName("동시 최초 등록으로 UNIQUE 위반이 나면 409 충돌 예외로 변환한다")
    @Test
    void translateUniqueViolationToConflict() {
        // given
        NotificationDeviceRepository repository = Mockito.mock(NotificationDeviceRepository.class);
        Mockito.when(repository.findByToken(anyString())).thenReturn(Optional.empty());
        Mockito.when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        NotificationDeviceService service = new NotificationDeviceService(repository);

        // when & then
        assertThatThrownBy(() -> service.register(HostFixture.createHostWithId(1L), DevicePlatform.IOS, TOKEN))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("디바이스 등록이 겹쳤습니다.");
    }
}
