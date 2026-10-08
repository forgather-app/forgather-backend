package com.forgather.acceptance;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.back_office.dto.AdminLoginRequest;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.fixture.AdminUserFixture;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;

@AutoConfigureMockMvc
@TestPropertySource(properties = "logging.level.com.forgather.global.logging=DEBUG")
class AdminLoginAcceptanceTest extends AcceptanceTest {

    private static final String LOGIN_PASSWORD = "admin-login-test-secret";

    private final Logger logger = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        adminUserRepository.save(AdminUserFixture.createAdminUser("admin", passwordEncoder.encode(LOGIN_PASSWORD)));
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void stopCapturingLogs() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @DisplayName("어드민 로그인에 성공하면 세션 쿠키가 설정되고 로그에 비밀번호가 노출되지 않는다.")
    @Test
    void loginBackOffice() {
        // given
        AdminLoginRequest request = new AdminLoginRequest("admin", LOGIN_PASSWORD);

        // when
        MockMvcResponse response = RestAssuredMockMvc.given()
            .body(request)
            .when()
            .post("/admin/login")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .response();

        String setCookieHeader = response.getHeader("Set-Cookie");

        // then
        assertAll(
            () -> assertThat(setCookieHeader).contains(SESSION_COOKIE_NAME),
            () -> assertLoginLogsExcludePassword(LOGIN_PASSWORD)
        );
    }

    @DisplayName("어드민 로그인에 실패해도 로그에 입력한 비밀번호가 노출되지 않는다.")
    @Test
    void failedLoginDoesNotLogPassword() {
        // given
        String wrongPassword = "incorrect-login-test-secret";
        AdminLoginRequest request = new AdminLoginRequest("admin", wrongPassword);

        // when
        RestAssuredMockMvc.given()
            .body(request)
            .when()
            .post("/admin/login")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());

        // then
        assertLoginLogsExcludePassword(wrongPassword);
    }

    private void assertLoginLogsExcludePassword(String password) {
        List<String> logs = appender.list.stream()
            .map(event -> event.getFormattedMessage() + " " + event.getKeyValuePairs()
                + (event.getThrowableProxy() == null ? "" : ThrowableProxyUtil.asString(event.getThrowableProxy())))
            .toList();
        assertAll(
            () -> assertThat(logs)
                .anySatisfy(message -> assertThat(message).contains("AdminLoginService.login(..)")),
            () -> assertThat(logs).allSatisfy(message -> assertThat(message).doesNotContain(password))
        );
    }
}
