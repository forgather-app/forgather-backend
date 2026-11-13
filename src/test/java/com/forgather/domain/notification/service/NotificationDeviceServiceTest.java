package com.forgather.domain.notification.service;

import static com.forgather.fixture.ConstraintViolationFixture.foreignKeyViolation;
import static com.forgather.fixture.ConstraintViolationFixture.uniqueViolation;
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
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.notification.model.DevicePlatform;
import com.forgather.domain.notification.model.NotificationDevice;
import com.forgather.domain.notification.repository.NotificationDeviceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationDeviceFixture;
import com.forgather.global.exception.ConflictException;

/**
 * 실제 커밋 결과를 확인하기 위해 테스트 트랜잭션 없이 실행하고 cleanup.sql로 정리한다.
 * 리포지토리를 인터페이스 타입의 스파이로 사용하므로 행 수와 컬럼 값은 SQL로 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class NotificationDeviceServiceTest extends TestOnContainer {

    private static final String TOKEN = NotificationDeviceFixture.TOKEN;
    private static final String OTHER_TOKEN = NotificationDeviceFixture.OTHER_TOKEN;

    @Autowired
    private NotificationDeviceService notificationDeviceService;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private NotificationDeviceRepository notificationDeviceRepository;

    private Host saveHost() {
        return hostRepository.save(HostFixture.createHost());
    }

    /**
     * 스파이는 JDK 프록시 빈을 인터페이스 타입으로 감싸 실제 빈에 위임하므로 doCallRealMethod를 쓸 수 없다.
     * 스파이의 기본 답변(실제 빈 위임)을 꺼내 "이번 호출부터는 실제 동작"을 표현한다.
     */
    private Answer<Object> realRepository() {
        Answer<?> delegate = Mockito.mockingDetails(notificationDeviceRepository)
            .getMockCreationSettings()
            .getDefaultAnswer();
        return delegate::answer;
    }

    private NotificationDevice getByToken(String token) {
        return notificationDeviceRepository.findByToken(token).orElseThrow();
    }

    private long countDevices() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification_device", Long.class);
        return count == null ? 0 : count;
    }

    private Long idOf(String token) {
        return jdbcTemplate.queryForObject("SELECT id FROM notification_device WHERE token = ?", Long.class, token);
    }

    private Long hostIdOf(String token) {
        return jdbcTemplate.queryForObject("SELECT host_id FROM notification_device WHERE token = ?", Long.class, token);
    }

    private String platformOf(String token) {
        return jdbcTemplate.queryForObject("SELECT platform FROM notification_device WHERE token = ?", String.class, token);
    }

    @DisplayName("새 토큰을 등록하면 호스트의 디바이스로 저장된다")
    @Test
    void registerNewDevice() {
        // given
        Host host = saveHost();

        // when
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);

        // then
        List<NotificationDevice> devices = notificationDeviceService.findAllByHost(host);
        assertThat(devices).hasSize(1);
        NotificationDevice device = devices.getFirst();
        assertAll(
            () -> assertThat(hostIdOf(TOKEN)).isEqualTo(host.getId()),
            () -> assertThat(device.getPlatform()).isEqualTo(DevicePlatform.IOS),
            () -> assertThat(device.getToken()).isEqualTo(TOKEN),
            () -> assertThat(device.getLastActiveAt()).isNotNull()
        );
    }

    @DisplayName("같은 호스트가 같은 토큰을 다시 등록하면 행이 늘지 않고 활동 시각만 갱신된다")
    @Test
    void reRegisterSameHostUpdatesLastActiveAt() {
        // given
        Host host = saveHost();
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);
        LocalDateTime staleTime = LocalDateTime.now().minusDays(1).withNano(0);
        jdbcTemplate.update("UPDATE notification_device SET last_active_at = ? WHERE token = ?", staleTime, TOKEN);

        // when
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);

        // then
        assertAll(
            () -> assertThat(countDevices()).isEqualTo(1),
            () -> assertThat(getByToken(TOKEN).getLastActiveAt()).isAfter(staleTime)
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
        assertAll(
            () -> assertThat(countDevices()).isEqualTo(1),
            () -> assertThat(hostIdOf(TOKEN)).isEqualTo(newOwner.getId()),
            () -> assertThat(platformOf(TOKEN)).isEqualTo(DevicePlatform.ANDROID.name()),
            () -> assertThat(notificationDeviceService.findAllByHost(previousOwner)).isEmpty(),
            () -> assertThat(notificationDeviceService.findAllByHost(newOwner)).hasSize(1)
        );
    }

    @DisplayName("해제한 토큰을 다시 등록하면 새 행으로 저장된다")
    @Test
    void reRegisterAfterDeregisterCreatesNewDevice() {
        // given
        Host host = saveHost();
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);
        Long previousId = idOf(TOKEN);
        notificationDeviceService.deregister(host, TOKEN);

        // when
        notificationDeviceService.register(host, DevicePlatform.ANDROID, TOKEN);

        // then
        assertAll(
            () -> assertThat(countDevices()).isEqualTo(1),
            () -> assertThat(idOf(TOKEN)).isNotEqualTo(previousId),
            () -> assertThat(hostIdOf(TOKEN)).isEqualTo(host.getId()),
            () -> assertThat(platformOf(TOKEN)).isEqualTo(DevicePlatform.ANDROID.name()),
            () -> assertThat(notificationDeviceService.findAllByHost(host)).hasSize(1)
        );
    }

    /**
     * 기존 행을 커밋하고 첫 조회만 빈 결과로 만들어 실제 UNIQUE 위반을 재현한다.
     * 두 번째 조회부터는 실제 리포지토리를 사용한다.
     */
    @DisplayName("동시 최초 등록 경합에 지면 새 트랜잭션에서 한 번 더 시도해 상대가 저장한 행의 소유자를 갱신한다")
    @Test
    void retryAfterConcurrentFirstRegistrationReassignsExistingDevice() {
        // given
        Host winner = saveHost();
        Host loser = saveHost();
        notificationDeviceService.register(winner, DevicePlatform.IOS, TOKEN);
        Mockito.clearInvocations(notificationDeviceRepository);
        Mockito.doReturn(Optional.empty()).doAnswer(realRepository())
            .when(notificationDeviceRepository).findByToken(TOKEN);

        // when
        assertThatCode(() -> notificationDeviceService.register(loser, DevicePlatform.ANDROID, TOKEN))
            .doesNotThrowAnyException();

        // then
        assertAll(
            () -> assertThat(countDevices()).isEqualTo(1),
            () -> assertThat(hostIdOf(TOKEN)).isEqualTo(loser.getId()),
            () -> assertThat(platformOf(TOKEN)).isEqualTo(DevicePlatform.ANDROID.name()),
            () -> Mockito.verify(notificationDeviceRepository, Mockito.times(1)).saveAndFlush(any())
        );
    }

    /**
     * 첫 조회 직후 별도 트랜잭션에서 행을 삭제해, 갱신 실패 후 새 행으로 복구되는지 확인한다.
     */
    @DisplayName("조회한 행이 먼저 삭제되면 새 트랜잭션에서 재시도해 새 행으로 저장한다")
    @Test
    void retryAfterDeviceDeletedUnderneathSavesNewDevice() {
        // given
        Host previousOwner = saveHost();
        Host host = saveHost();
        notificationDeviceService.register(previousOwner, DevicePlatform.IOS, TOKEN);
        Long previousId = idOf(TOKEN);
        TransactionTemplate otherTransaction = new TransactionTemplate(transactionManager);
        otherTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Mockito.doAnswer(invocation -> {
            Object found = realRepository().answer(invocation);
            otherTransaction.executeWithoutResult(status ->
                jdbcTemplate.update("DELETE FROM notification_device WHERE token = ?", TOKEN));
            return found;
        }).doAnswer(realRepository()).when(notificationDeviceRepository).findByToken(TOKEN);

        // when
        assertThatCode(() -> notificationDeviceService.register(host, DevicePlatform.ANDROID, TOKEN))
            .doesNotThrowAnyException();

        // then
        assertAll(
            () -> assertThat(countDevices()).isEqualTo(1),
            () -> assertThat(idOf(TOKEN)).isNotEqualTo(previousId),
            () -> assertThat(hostIdOf(TOKEN)).isEqualTo(host.getId()),
            () -> assertThat(platformOf(TOKEN)).isEqualTo(DevicePlatform.ANDROID.name())
        );
    }

    /**
     * token 컬럼은 utf8mb4_bin이다. 기본 collation(utf8mb4_0900_ai_ci)이면 대소문자만 다른 토큰이
     * 같은 행으로 조회되어 다른 기기의 토큰이 새 호스트로 넘어가므로, MySQL 컨테이너에서 정확 일치를 확인한다.
     */
    @DisplayName("토큰은 대소문자를 구분해 조회하고 저장한다")
    @Test
    void tokenIsCaseSensitive() {
        // given
        Host host = saveHost();
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);
        String upperCaseToken = TOKEN.toUpperCase();

        // when
        notificationDeviceService.register(host, DevicePlatform.ANDROID, upperCaseToken);

        // then
        assertAll(
            () -> assertThat(countDevices()).isEqualTo(2),
            () -> assertThat(platformOf(TOKEN)).isEqualTo(DevicePlatform.IOS.name()),
            () -> assertThat(platformOf(upperCaseToken)).isEqualTo(DevicePlatform.ANDROID.name())
        );
    }

    @DisplayName("본인 토큰을 해제하면 디바이스 행이 삭제된다")
    @Test
    void deregisterOwnDevice() {
        // given
        Host host = saveHost();
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);

        // when
        notificationDeviceService.deregister(host, TOKEN);

        // then
        assertAll(
            () -> assertThat(notificationDeviceService.findAllByHost(host)).isEmpty(),
            () -> assertThat(notificationDeviceRepository.findByToken(TOKEN)).isEmpty()
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
            () -> assertThat(notificationDeviceService.findAllByHost(owner)).hasSize(1),
            () -> assertThat(notificationDeviceRepository.findByToken(TOKEN)).isPresent()
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

    @DisplayName("호스트의 디바이스를 모두 삭제하면 그 호스트의 디바이스 행만 삭제된다")
    @Test
    void deleteAllByHost() {
        // given
        Host host = saveHost();
        Host other = saveHost();
        notificationDeviceService.register(host, DevicePlatform.IOS, TOKEN);
        notificationDeviceService.register(host, DevicePlatform.ANDROID, OTHER_TOKEN);
        notificationDeviceService.register(other, DevicePlatform.IOS, "fcm-registration-token-0003");

        // when
        int deletedCount = notificationDeviceService.deleteAllByHost(host);

        // then
        assertAll(
            () -> assertThat(deletedCount).isEqualTo(2),
            () -> assertThat(notificationDeviceService.findAllByHost(host)).isEmpty(),
            () -> assertThat(notificationDeviceService.findAllByHost(other)).hasSize(1),
            () -> assertThat(notificationDeviceRepository.findByToken(TOKEN)).isEmpty(),
            () -> assertThat(notificationDeviceRepository.findByToken(OTHER_TOKEN)).isEmpty()
        );
    }

    @DisplayName("디바이스가 없는 호스트의 디바이스를 모두 삭제하면 0건을 돌려준다")
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
     * 모의 리포지토리로 두 번 연속 UNIQUE 위반을 만들어 재시도 상한과 예외 전파를 확인한다.
     */
    @DisplayName("재시도에서도 UNIQUE 위반이 나면 409 충돌 예외로 변환한다")
    @Test
    void translateUniqueViolationToConflictAfterRetry() {
        // given
        NotificationDeviceRepository repository = Mockito.mock(NotificationDeviceRepository.class);
        Mockito.when(repository.findByToken(anyString())).thenReturn(Optional.empty());
        Mockito.when(repository.saveAndFlush(any()))
            .thenThrow(uniqueViolation("notification_device." + NotificationDevice.UK_TOKEN));
        NotificationDeviceService service = new NotificationDeviceService(repository, transactionTemplate);

        // when & then
        assertThatThrownBy(() -> service.register(HostFixture.createHostWithId(1L), DevicePlatform.IOS, TOKEN))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("디바이스 등록이 겹쳤습니다.")
            .hasCauseInstanceOf(DataIntegrityViolationException.class);
        Mockito.verify(repository, Mockito.times(2)).saveAndFlush(any());
    }

    @DisplayName("token UNIQUE가 아닌 제약 위반은 재시도하지 않고 원본 예외를 그대로 전파한다")
    @Test
    void propagateOtherConstraintViolationWithoutRetry() {
        // given
        NotificationDeviceRepository repository = Mockito.mock(NotificationDeviceRepository.class);
        Mockito.when(repository.findByToken(anyString())).thenReturn(Optional.empty());
        DataIntegrityViolationException violation =
            foreignKeyViolation("notification_device.FK_host_TO_notification_device");
        Mockito.when(repository.saveAndFlush(any())).thenThrow(violation);
        NotificationDeviceService service = new NotificationDeviceService(repository, transactionTemplate);

        // when & then
        assertThatThrownBy(() -> service.register(HostFixture.createHostWithId(1L), DevicePlatform.IOS, TOKEN))
            .isSameAs(violation);
        Mockito.verify(repository, Mockito.times(1)).saveAndFlush(any());
    }
}
