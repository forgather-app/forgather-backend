package com.forgather.domain.notification.model;

import static com.forgather.fixture.NotificationDeviceFixture.createDevice;
import static com.forgather.fixture.NotificationDeviceFixture.createDeviceWithHost;
import static com.forgather.fixture.NotificationDeviceFixture.createDeviceWithToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.host.model.Host;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationDeviceFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class NotificationDeviceTest {

    @DisplayName("호스트, 플랫폼, 토큰으로 생성하면 활동 시각이 기록된다")
    @Test
    void createDeviceRecordsLastActiveAt() {
        // given
        Host host = HostFixture.createHost();
        LocalDateTime before = LocalDateTime.now();

        // when
        NotificationDevice device = new NotificationDevice(host, DevicePlatform.ANDROID, "token-1");

        // then
        assertAll(
            () -> assertThat(device.getHost()).isSameAs(host),
            () -> assertThat(device.getPlatform()).isEqualTo(DevicePlatform.ANDROID),
            () -> assertThat(device.getToken()).isEqualTo("token-1"),
            () -> assertThat(device.getLastActiveAt()).isAfterOrEqualTo(before),
            () -> assertThat(device.getDeletedAt()).isNull()
        );
    }

    @Nested
    @DisplayName("필수 필드 검증")
    class RequiredFields {

        @DisplayName("호스트가 null이면 생성할 수 없다")
        @Test
        void createWithNullHost() {
            // when & then
            assertThatThrownBy(() -> createDeviceWithHost(null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("디바이스 호스트는 null일 수 없습니다.");
        }

        @DisplayName("플랫폼이 null이면 요청 오류로 분류한 예외를 던진다")
        @Test
        void createWithNullPlatform() {
            // when & then
            assertThatThrownBy(() -> new NotificationDevice(HostFixture.createHost(), null, "token-1"))
                .isInstanceOfSatisfying(BaseNullPointerException.class, exception ->
                    assertThat(exception.getStatusCode()).isEqualTo(400))
                .hasMessageContaining("디바이스 플랫폼은 null일 수 없습니다.");
        }

        @DisplayName("토큰이 null이면 요청 오류로 분류한 예외를 던진다")
        @Test
        void createWithNullToken() {
            // when & then
            assertThatThrownBy(() -> createDeviceWithToken(null))
                .isInstanceOfSatisfying(BaseNullPointerException.class, exception ->
                    assertThat(exception.getStatusCode()).isEqualTo(400))
                .hasMessageContaining("디바이스 토큰은 null일 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("토큰 검증")
    class Token {

        @DisplayName("토큰이 공백만이면 예외를 던진다")
        @Test
        void throwExceptionWhenTokenIsBlank() {
            // when & then
            assertThatThrownBy(() -> createDeviceWithToken("   "))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("디바이스 토큰은 공백만 입력할 수 없습니다.");
        }

        @DisplayName("토큰에 공백이 섞여 있으면 예외를 던진다")
        @ValueSource(strings = {"token 1", " token1", "token1\n", "to\tken"})
        @ParameterizedTest
        void throwExceptionWhenTokenContainsWhitespace(String token) {
            // when & then
            assertThatThrownBy(() -> createDeviceWithToken(token))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("디바이스 토큰에는 공백을 포함할 수 없습니다.");
        }

        @DisplayName("토큰은 512자까지 허용한다")
        @Test
        void allowTokenAtMaxLength() {
            // given
            String token = "a".repeat(512);

            // when & then
            assertThatCode(() -> createDeviceWithToken(token)).doesNotThrowAnyException();
        }

        @DisplayName("토큰이 512자를 초과하면 예외를 던진다")
        @Test
        void throwExceptionWhenTokenExceedsMaxLength() {
            // given
            String token = "a".repeat(513);

            // when & then
            assertThatThrownBy(() -> createDeviceWithToken(token))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("디바이스 토큰은 최대 512자까지 가능합니다.");
        }
    }

    @Nested
    @DisplayName("재등록")
    class Reassign {

        @DisplayName("재등록하면 소유 호스트와 플랫폼이 바뀌고 활동 시각이 갱신된다")
        @Test
        void reassignChangesOwnerAndPlatform() {
            // given
            NotificationDevice device = createDevice();
            Host newHost = HostFixture.createHostWithName("새 호스트");
            LocalDateTime past = LocalDateTime.now().minusDays(1);
            ReflectionTestUtils.setField(device, "lastActiveAt", past);

            // when
            device.reassign(newHost, DevicePlatform.ANDROID);

            // then
            assertAll(
                () -> assertThat(device.getHost()).isSameAs(newHost),
                () -> assertThat(device.getPlatform()).isEqualTo(DevicePlatform.ANDROID),
                () -> assertThat(device.getToken()).isEqualTo(NotificationDeviceFixture.TOKEN),
                () -> assertThat(device.getLastActiveAt()).isAfter(past)
            );
        }

        @DisplayName("논리 삭제된 디바이스를 재등록하면 복구된다")
        @Test
        void reassignRestoresDeletedDevice() {
            // given
            NotificationDevice device = createDevice();
            device.delete();

            // when
            device.reassign(HostFixture.createHost(), DevicePlatform.IOS);

            // then
            assertThat(device.getDeletedAt()).isNull();
        }

        @DisplayName("재등록 호스트가 null이면 예외를 던진다")
        @Test
        void throwExceptionWhenReassignHostIsNull() {
            // given
            NotificationDevice device = createDevice();

            // when & then
            assertThatThrownBy(() -> device.reassign(null, DevicePlatform.IOS))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("디바이스 호스트는 null일 수 없습니다.");
        }

        @DisplayName("재등록 플랫폼이 null이면 예외를 던진다")
        @Test
        void throwExceptionWhenReassignPlatformIsNull() {
            // given
            NotificationDevice device = createDevice();

            // when & then
            assertThatThrownBy(() -> device.reassign(HostFixture.createHost(), null))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("디바이스 플랫폼은 null일 수 없습니다.");
        }
    }
}
