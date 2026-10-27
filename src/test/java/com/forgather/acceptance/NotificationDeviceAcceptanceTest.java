package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.notification.model.DevicePlatform;
import com.forgather.domain.notification.model.NotificationDevice;
import com.forgather.domain.notification.repository.NotificationDeviceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.util.JwtTokenProvider;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@DisplayName("인수 테스트: 푸시 디바이스 토큰")
@AutoConfigureMockMvc
class NotificationDeviceAcceptanceTest extends AcceptanceTest {

    /**
     * FCM 토큰 문자 집합(영숫자, ':', '_', '-')이 URL 경로에 그대로 들어가는지 함께 확인한다.
     */
    private static final String TOKEN = "dGVzdC10b2tlbg:APA91bExample_Token-0001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private NotificationDeviceRepository notificationDeviceRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Host host;
    private String accessToken;

    @BeforeEach
    void setUpMockMvc() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        host = hostRepository.save(HostFixture.createHost());
        accessToken = jwtTokenProvider.generateAccessToken(host.getId());
    }

    private Map<String, Object> registerRequest(Object platform, Object token) {
        Map<String, Object> body = new HashMap<>();
        body.put("platform", platform);
        body.put("token", token);
        return body;
    }

    private void register(String accessToken, Map<String, Object> body) {
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .put("/notifications/devices")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("code", equalTo("SUCCESS"));
    }

    private void deregister(String accessToken, String token) {
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .when()
            .delete("/notifications/devices/{token}", token)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("code", equalTo("SUCCESS"));
    }

    private List<NotificationDevice> activeDevicesOf(Host host) {
        return notificationDeviceRepository.findAllByHostAndDeletedAtIsNull(host);
    }

    @DisplayName("푸시 디바이스 토큰을 등록하면 200과 함께 활성 디바이스가 생성된다")
    @Test
    void registerDevice() {
        // given
        Map<String, Object> body = registerRequest("IOS", TOKEN);

        // when
        register(accessToken, body);

        // then
        List<NotificationDevice> devices = activeDevicesOf(host);
        assertThat(devices).hasSize(1);
        assertAll(
            () -> assertThat(devices.getFirst().getPlatform()).isEqualTo(DevicePlatform.IOS),
            () -> assertThat(devices.getFirst().getToken()).isEqualTo(TOKEN)
        );
    }

    @DisplayName("같은 토큰을 다시 등록하면 디바이스가 늘어나지 않는다")
    @Test
    void registerDeviceIsIdempotent() {
        // given
        register(accessToken, registerRequest("IOS", TOKEN));

        // when
        register(accessToken, registerRequest("IOS", TOKEN));

        // then
        assertThat(activeDevicesOf(host)).hasSize(1);
    }

    @DisplayName("다른 호스트가 같은 토큰을 등록하면 디바이스 소유자가 새 호스트로 바뀐다")
    @Test
    void registerDeviceByOtherHostTransfersOwnership() {
        // given
        register(accessToken, registerRequest("IOS", TOKEN));
        Host otherHost = hostRepository.save(HostFixture.createHost());
        String otherAccessToken = jwtTokenProvider.generateAccessToken(otherHost.getId());

        // when
        register(otherAccessToken, registerRequest("ANDROID", TOKEN));

        // then
        assertAll(
            () -> assertThat(activeDevicesOf(host)).isEmpty(),
            () -> assertThat(activeDevicesOf(otherHost)).hasSize(1),
            () -> assertThat(activeDevicesOf(otherHost).getFirst().getPlatform()).isEqualTo(DevicePlatform.ANDROID)
        );
    }

    @DisplayName("해제한 토큰을 다시 등록하면 기존 디바이스가 복구된다")
    @Test
    void reRegisterRestoresDeregisteredDevice() {
        // given
        register(accessToken, registerRequest("IOS", TOKEN));
        Long deviceId = notificationDeviceRepository.findByToken(TOKEN).orElseThrow().getId();
        deregister(accessToken, TOKEN);

        // when
        register(accessToken, registerRequest("ANDROID", TOKEN));

        // then
        NotificationDevice restored = notificationDeviceRepository.findByToken(TOKEN).orElseThrow();
        assertAll(
            () -> assertThat(restored.getId()).isEqualTo(deviceId),
            () -> assertThat(restored.getDeletedAt()).isNull(),
            () -> assertThat(restored.getPlatform()).isEqualTo(DevicePlatform.ANDROID),
            () -> assertThat(activeDevicesOf(host)).hasSize(1)
        );
    }

    @DisplayName("로그인 없이 푸시 디바이스 토큰을 등록할 수 없다")
    @Test
    void registerDeviceWithoutLogin() {
        // when & then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .body(registerRequest("IOS", TOKEN))
            .when()
            .put("/notifications/devices")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("플랫폼 값이 enum과 일치하지 않으면 400 BAD_REQUEST로 응답한다")
    @Test
    void rejectUnknownPlatform() {
        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .contentType(ContentType.JSON)
            .body(registerRequest("WINDOWS", TOKEN))
            .when()
            .put("/notifications/devices")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("BAD_REQUEST"))
            .body("message", containsString("'DevicePlatform' 형식이 올바르지 않습니다."));
    }

    @DisplayName("플랫폼이 없으면 400 VALIDATION_FAILED로 응답한다")
    @Test
    void rejectNullPlatform() {
        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .contentType(ContentType.JSON)
            .body(registerRequest(null, TOKEN))
            .when()
            .put("/notifications/devices")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("토큰이 공백이면 400 VALIDATION_FAILED로 응답한다")
    @Test
    void rejectBlankToken() {
        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .contentType(ContentType.JSON)
            .body(registerRequest("IOS", "   "))
            .when()
            .put("/notifications/devices")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("토큰 중간에 공백이 있으면 400 BAD_REQUEST로 응답한다")
    @Test
    void rejectTokenWithWhitespace() {
        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .contentType(ContentType.JSON)
            .body(registerRequest("IOS", "fcm token with space"))
            .when()
            .put("/notifications/devices")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("BAD_REQUEST"))
            .body("message", containsString("디바이스 토큰에는 공백을 포함할 수 없습니다."));
    }

    @DisplayName("토큰이 512자를 초과하면 400 VALIDATION_FAILED로 응답한다")
    @Test
    void rejectTooLongToken() {
        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .contentType(ContentType.JSON)
            .body(registerRequest("IOS", "a".repeat(NotificationDevice.MAX_TOKEN_LENGTH + 1)))
            .when()
            .put("/notifications/devices")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("본인 토큰을 해제하면 200과 함께 디바이스가 soft delete된다")
    @Test
    void deregisterDevice() {
        // given
        register(accessToken, registerRequest("IOS", TOKEN));

        // when
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .when()
            .delete("/notifications/devices/{token}", TOKEN)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("code", equalTo("SUCCESS"));

        // then
        assertAll(
            () -> assertThat(activeDevicesOf(host)).isEmpty(),
            () -> assertThat(notificationDeviceRepository.findByToken(TOKEN).orElseThrow().getDeletedAt()).isNotNull()
        );
    }

    @DisplayName("다른 호스트 소유의 토큰을 해제하면 200으로 응답하지만 디바이스는 유지된다")
    @Test
    void deregisterOtherHostDeviceIsIgnored() {
        // given
        register(accessToken, registerRequest("IOS", TOKEN));
        Host otherHost = hostRepository.save(HostFixture.createHost());
        String otherAccessToken = jwtTokenProvider.generateAccessToken(otherHost.getId());

        // when
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(otherAccessToken))
            .when()
            .delete("/notifications/devices/{token}", TOKEN)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("code", equalTo("SUCCESS"));

        // then
        assertAll(
            () -> assertThat(activeDevicesOf(host)).hasSize(1),
            () -> assertThat(notificationDeviceRepository.findByToken(TOKEN).orElseThrow().getDeletedAt()).isNull()
        );
    }

    @DisplayName("존재하지 않는 토큰을 해제해도 200으로 응답한다")
    @Test
    void deregisterUnknownTokenReturnsOk() {
        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .when()
            .delete("/notifications/devices/{token}", "unknown-token")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("code", equalTo("SUCCESS"));
    }

    @DisplayName("로그인 없이 푸시 디바이스 토큰을 해제할 수 없다")
    @Test
    void deregisterDeviceWithoutLogin() {
        // when & then
        RestAssuredMockMvc.given()
            .when()
            .delete("/notifications/devices/{token}", TOKEN)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }
}
