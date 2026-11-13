package com.forgather.domain.notification.service;

import static com.forgather.fixture.ConstraintViolationFixture.foreignKeyViolation;
import static com.forgather.fixture.ConstraintViolationFixture.uniqueViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.notification.dto.NotificationSettingResponse;
import com.forgather.domain.notification.model.NotificationSetting;
import com.forgather.domain.notification.repository.NotificationSettingRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationSettingFixture;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ConflictException;

/**
 * 설정 변경은 서비스가 연 트랜잭션에서 실제로 커밋되므로 테스트 트랜잭션 없이 실행하고 cleanup.sql로 정리한다.
 * 행 수와 컬럼 값은 SQL로 확인한다.
 * <p>
 * 경합 재현에는 리포지토리 빈을 대체(@MockitoSpyBean)하지 않고, 실제 빈에 위임하는 모의 객체를 만들어 서비스에 직접 주입한다.
 * 빈을 대체하면 이 클래스만을 위한 테스트 컨텍스트와 커넥션 풀이 새로 생겨, 전체 테스트 실행 시 MySQL 연결 수 한도를 넘을 수 있다.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class NotificationSettingServiceTest extends TestOnContainer {

    @Autowired
    private NotificationSettingService notificationSettingService;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    private Host saveHost() {
        return hostRepository.save(HostFixture.createHost());
    }

    /**
     * 실제 리포지토리 빈(JDK 프록시)에 위임하는 답변. "이번 호출부터는 실제 동작"을 표현할 때 쓴다.
     */
    private Answer<Object> realRepository() {
        return AdditionalAnswers.delegatesTo(notificationSettingRepository);
    }

    /**
     * 기본 동작은 실제 리포지토리에 위임하고, 테스트가 필요한 호출만 바꿔 끼울 수 있는 모의 리포지토리.
     */
    private NotificationSettingRepository delegatingRepository() {
        return Mockito.mock(NotificationSettingRepository.class, realRepository());
    }

    private long countSettings() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification_setting", Long.class);
        return count == null ? 0 : count;
    }

    private Boolean enabledOf(Host host) {
        return jdbcTemplate.queryForObject(
            "SELECT enabled FROM notification_setting WHERE host_id = ?", Boolean.class, host.getId());
    }

    private LocalDateTime updatedAtOf(Host host) {
        return jdbcTemplate.queryForObject(
            "SELECT updated_at FROM notification_setting WHERE host_id = ?", LocalDateTime.class, host.getId());
    }

    /**
     * created_at·updated_at은 초 단위라 같은 초 안의 갱신을 구분할 수 없으므로, 갱신 전 시각을 과거로 돌려 둔다.
     */
    private LocalDateTime makeUpdatedAtStale(Host host) {
        LocalDateTime staleTime = LocalDateTime.now().minusDays(1).withNano(0);
        jdbcTemplate.update(
            "UPDATE notification_setting SET updated_at = ? WHERE host_id = ?", staleTime, host.getId());
        return staleTime;
    }

    @DisplayName("설정 행이 없는 호스트의 설정을 조회하면 허용을 돌려주고 행을 만들지 않는다")
    @Test
    void getDefaultSettingWithoutRow() {
        // given
        Host host = saveHost();

        // when
        NotificationSettingResponse response = notificationSettingService.getSetting(host);

        // then
        assertAll(
            () -> assertThat(response.enabled()).isTrue(),
            () -> assertThat(countSettings()).isZero()
        );
    }

    @DisplayName("설정 행이 있는 호스트의 설정을 조회하면 저장된 값을 돌려준다")
    @Test
    void getStoredSetting() {
        // given
        Host host = saveHost();
        notificationSettingRepository.save(NotificationSettingFixture.createDisabled(host));

        // when
        NotificationSettingResponse response = notificationSettingService.getSetting(host);

        // then
        assertThat(response.enabled()).isFalse();
    }

    @DisplayName("설정 행이 없으면 알림을 허용한 것으로 판정한다")
    @Test
    void isEnabledWithoutRow() {
        // given
        Host host = saveHost();

        // when & then
        assertThat(notificationSettingService.isEnabled(host)).isTrue();
    }

    @DisplayName("알림을 켠 설정 행이 있으면 알림을 허용한 것으로 판정한다")
    @Test
    void isEnabledWithEnabledRow() {
        // given
        Host host = saveHost();
        notificationSettingRepository.save(NotificationSettingFixture.createEnabled(host));

        // when & then
        assertThat(notificationSettingService.isEnabled(host)).isTrue();
    }

    @DisplayName("알림을 끈 설정 행이 있으면 알림을 허용하지 않은 것으로 판정한다")
    @Test
    void isEnabledWithDisabledRow() {
        // given
        Host host = saveHost();
        notificationSettingRepository.save(NotificationSettingFixture.createDisabled(host));

        // when & then
        assertThat(notificationSettingService.isEnabled(host)).isFalse();
    }

    @DisplayName("설정 행이 없는 호스트의 설정을 변경하면 행을 만들고 요청한 값을 저장한다")
    @Test
    void updateCreatesRow() {
        // given
        Host host = saveHost();

        // when
        NotificationSettingResponse response = notificationSettingService.update(host, false);

        // then
        assertAll(
            () -> assertThat(response.enabled()).isFalse(),
            () -> assertThat(countSettings()).isEqualTo(1),
            () -> assertThat(enabledOf(host)).isFalse()
        );
    }

    @DisplayName("설정 행이 있는 호스트의 설정을 변경하면 행을 늘리지 않고 값과 수정 시각을 갱신한다")
    @Test
    void updateExistingRow() {
        // given
        Host host = saveHost();
        notificationSettingRepository.save(NotificationSettingFixture.createEnabled(host));
        LocalDateTime staleTime = makeUpdatedAtStale(host);

        // when
        NotificationSettingResponse response = notificationSettingService.update(host, false);

        // then
        assertAll(
            () -> assertThat(response.enabled()).isFalse(),
            () -> assertThat(countSettings()).isEqualTo(1),
            () -> assertThat(enabledOf(host)).isFalse(),
            () -> assertThat(updatedAtOf(host)).isAfter(staleTime)
        );
    }

    /**
     * 변경 감지 방식이면 같은 값 대입은 UPDATE를 내지 않는다. 명시적 UPDATE가 실행되는지 수정 시각으로 확인한다.
     */
    @DisplayName("저장된 값과 같은 값으로 변경해도 성공하고 수정 시각이 갱신된다")
    @Test
    void updateWithSameValueStillUpdates() {
        // given
        Host host = saveHost();
        notificationSettingRepository.save(NotificationSettingFixture.createEnabled(host));
        LocalDateTime staleTime = makeUpdatedAtStale(host);

        // when
        NotificationSettingResponse response = notificationSettingService.update(host, true);

        // then
        assertAll(
            () -> assertThat(response.enabled()).isTrue(),
            () -> assertThat(countSettings()).isEqualTo(1),
            () -> assertThat(enabledOf(host)).isTrue(),
            () -> assertThat(updatedAtOf(host)).isAfter(staleTime)
        );
    }

    /**
     * 상대 요청이 저장한 행을 커밋하고 첫 조회만 빈 결과로 만들어 실제 UNIQUE 위반을 재현한다.
     * 두 번째 조회부터는 실제 리포지토리를 사용한다.
     */
    @DisplayName("동시 최초 저장 경합에 지면 새 트랜잭션에서 한 번 더 시도해 상대가 저장한 행을 요청값으로 갱신한다")
    @Test
    void retryAfterConcurrentFirstSaveUpdatesExistingRow() {
        // given
        Host host = saveHost();
        notificationSettingRepository.save(NotificationSettingFixture.createEnabled(host));
        NotificationSettingRepository repository = delegatingRepository();
        Mockito.doReturn(Optional.empty()).doAnswer(realRepository())
            .when(repository).findByHost(any());
        NotificationSettingService service = new NotificationSettingService(repository, transactionTemplate);

        // when
        assertThatCode(() -> service.update(host, false))
            .doesNotThrowAnyException();

        // then
        assertAll(
            () -> assertThat(countSettings()).isEqualTo(1),
            () -> assertThat(enabledOf(host)).isFalse(),
            () -> Mockito.verify(repository, Mockito.times(1)).saveAndFlush(any()),
            () -> Mockito.verify(repository, Mockito.times(1)).updateEnabledByHost(any(), eq(false), any())
        );
    }

    /**
     * 요청 B가 비잠금 조회를 마친 직후, 별도 트랜잭션에서 요청 A가 반대 값을 커밋하도록 끼워 넣는다.
     * B는 조회 시점 값(true)과 같은 값(true)을 보내므로, 변경 감지 방식이었다면 UPDATE를 내지 않아 A의 false가 남는다.
     * 명시적 UPDATE는 A의 커밋 뒤 행 락을 잡아 실행되므로 마지막에 커밋한 B의 값이 남아야 한다.
     */
    @DisplayName("기존 행에 동시 변경이 겹치면 마지막에 커밋한 요청의 값이 남는다")
    @Test
    void lastCommittedUpdateWinsOnConcurrentChange() {
        // given
        Host host = saveHost();
        notificationSettingRepository.save(NotificationSettingFixture.createEnabled(host));
        TransactionTemplate otherRequest = new TransactionTemplate(transactionManager);
        otherRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        NotificationSettingRepository repository = delegatingRepository();
        Mockito.doAnswer(invocation -> {
            Object found = realRepository().answer(invocation);
            otherRequest.executeWithoutResult(status ->
                notificationSettingRepository.updateEnabledByHost(host, false, LocalDateTime.now()));
            return found;
        }).doAnswer(realRepository()).when(repository).findByHost(any());
        NotificationSettingService service = new NotificationSettingService(repository, transactionTemplate);

        // when
        NotificationSettingResponse response = service.update(host, true);

        // then
        assertAll(
            () -> assertThat(response.enabled()).isTrue(),
            () -> assertThat(countSettings()).isEqualTo(1),
            () -> assertThat(enabledOf(host)).isTrue()
        );
    }

    /**
     * 모의 리포지토리로 두 번 연속 UNIQUE 위반을 만들어 재시도 상한과 예외 전파를 확인한다.
     */
    @DisplayName("재시도에서도 host_id UNIQUE 위반이 나면 409 충돌 예외를 전파한다")
    @Test
    void translateUniqueViolationToConflictAfterRetry() {
        // given
        NotificationSettingRepository repository = Mockito.mock(NotificationSettingRepository.class);
        Mockito.when(repository.findByHost(any())).thenReturn(Optional.empty());
        Mockito.when(repository.saveAndFlush(any()))
            .thenThrow(uniqueViolation("notification_setting." + NotificationSetting.UK_HOST_ID));
        NotificationSettingService service = new NotificationSettingService(repository, transactionTemplate);

        // when & then
        assertThatThrownBy(() -> service.update(HostFixture.createHostWithId(1L), false))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("알림 설정 저장이 겹쳤습니다.")
            .hasCauseInstanceOf(DataIntegrityViolationException.class);
        Mockito.verify(repository, Mockito.times(2)).saveAndFlush(any());
        Mockito.verify(repository, Mockito.never()).updateEnabledByHost(any(), anyBoolean(), any());
    }

    @DisplayName("host_id UNIQUE가 아닌 제약 위반은 재시도하지 않고 원본 예외를 그대로 전파한다")
    @Test
    void propagateOtherConstraintViolationWithoutRetry() {
        // given
        NotificationSettingRepository repository = Mockito.mock(NotificationSettingRepository.class);
        Mockito.when(repository.findByHost(any())).thenReturn(Optional.empty());
        DataIntegrityViolationException violation =
            foreignKeyViolation("notification_setting.FK_host_TO_notification_setting");
        Mockito.when(repository.saveAndFlush(any())).thenThrow(violation);
        NotificationSettingService service = new NotificationSettingService(repository, transactionTemplate);

        // when & then
        assertThatThrownBy(() -> service.update(HostFixture.createHostWithId(1L), false))
            .isSameAs(violation);
        Mockito.verify(repository, Mockito.times(1)).saveAndFlush(any());
    }

    @DisplayName("호스트가 null이면 설정을 변경할 수 없다")
    @Test
    void rejectNullHost() {
        // when & then
        assertThatThrownBy(() -> notificationSettingService.update(null, false))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("알림 설정의 호스트는 null일 수 없습니다.");
    }
}
