package com.forgather.acceptance;

import static com.forgather.fixture.AdminUserFixture.RAW_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.back_office.dto.AdminLoginRequest;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.fixture.AdminUserFixture;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;

@AutoConfigureMockMvc
class AdminLoginAcceptanceTest extends AcceptanceTest {

    private static final String SESSION_COOKIE_NAME = "ADMIN_SESSION_ID";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("백오피스 어드민 로그인을 하면 세션 ID가 반환되고 쿠키가 설정된다.")
    @Test
    void loginBackOffice() {
        // given
        adminUserRepository.save(AdminUserFixture.createAdminUser("admin"));
        AdminLoginRequest request = new AdminLoginRequest("admin", RAW_PASSWORD);

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
        assertThat(setCookieHeader).contains(SESSION_COOKIE_NAME);
    }
}
