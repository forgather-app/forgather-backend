package com.forgather.back_office.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class AttackIpInfoTest {

    @DisplayName("유효한 IPv4 주소와 양수 횟수로 AttackIpInfo를 생성할 수 있다")
    @Test
    void createWithValidIpAndCount() {
        // given
        String ip = "192.168.0.1";
        long count = 5;

        // when & then
        assertThatCode(() -> new AttackIpInfo(ip, count))
            .doesNotThrowAnyException();
    }

    @DisplayName("경계값 IP 주소(0.0.0.0, 255.255.255.255)로 생성할 수 있다")
    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "255.255.255.255", "10.0.0.1", "172.16.0.1"})
    void createWithBoundaryIp(String ip) {
        // when & then
        assertThatCode(() -> new AttackIpInfo(ip, 1))
            .doesNotThrowAnyException();
    }

    @DisplayName("IP 주소가 null이면 예외를 던진다")
    @Test
    void createWithNullIp() {
        // when & then
        assertThatThrownBy(() -> new AttackIpInfo(null, 1))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("IP 주소는 null일 수 없습니다");
    }

    @DisplayName("공격 횟수가 음수이면 예외를 던진다")
    @Test
    void createWithNegativeCount() {
        // when & then
        assertThatThrownBy(() -> new AttackIpInfo("192.168.0.1", -1))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("공격 횟수는 음수일 수 없습니다");
    }

    @DisplayName("옥텟 값이 255를 초과하면 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"256.0.0.1", "1.2.3.999", "0.0.300.0"})
    void createWithOctetExceeding255(String ip) {
        // when & then
        assertThatThrownBy(() -> new AttackIpInfo(ip, 1))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("유효하지 않은 IP 주소 형식입니다");
    }

    @DisplayName("옥텟 개수가 4개가 아니면 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"192.168.0", "1.2.3.4.5", "192", ""})
    void createWithInvalidOctetCount(String ip) {
        // when & then
        assertThatThrownBy(() -> new AttackIpInfo(ip, 1))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("유효하지 않은 IP 주소 형식입니다");
    }

    @DisplayName("옥텟에 숫자가 아닌 문자가 포함되면 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"abc.def.ghi.jkl", "192.168.0.a", "1.2.3.4a"})
    void createWithNonNumericOctet(String ip) {
        // when & then
        assertThatThrownBy(() -> new AttackIpInfo(ip, 1))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("유효하지 않은 IP 주소 형식입니다");
    }
}
