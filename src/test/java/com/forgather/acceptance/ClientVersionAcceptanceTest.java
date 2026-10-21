package com.forgather.acceptance;

import static com.forgather.global.client.ClientVersionInterceptor.APP_VERSION_HEADER;
import static com.forgather.global.client.ClientVersionInterceptor.CLIENT_TYPE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "client-version.enabled=true",
    "client-version.minimum-app-version.ios=1.2.0",
    "client-version.minimum-app-version.android=1.3.0"
})
class ClientVersionAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("클라이언트 타입 헤더가 없는 구버전 앱 요청은 426과 UPGRADE_REQUIRED 코드로 응답한다.")
    @Test
    void requestWithoutHeadersIsUpgradeRequired() {
        // when
        ApiResponse<Void> result = RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/terms")
            .then()
            .statusCode(426)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.UPGRADE_REQUIRED),
            () -> assertThat(result.message()).isNotBlank(),
            () -> assertThat(result.data()).isNull()
        );
    }

    @DisplayName("최소 지원 버전 미만인 앱 요청은 426으로 응답한다.")
    @Test
    void outdatedAppRequestIsUpgradeRequired() {
        RestAssuredMockMvc.given()
            .header(CLIENT_TYPE_HEADER, "ANDROID")
            .header(APP_VERSION_HEADER, "1.2.9")
            .when()
            .get("/terms")
            .then()
            .statusCode(426);
    }

    @DisplayName("웹 요청은 버전 헤더 없이 처리된다.")
    @Test
    void webRequestIsProcessed() {
        RestAssuredMockMvc.given()
            .header(CLIENT_TYPE_HEADER, "WEB")
            .when()
            .get("/terms")
            .then()
            .statusCode(200);
    }

    @DisplayName("최소 지원 버전 이상인 앱 요청은 처리된다.")
    @Test
    void supportedAppRequestIsProcessed() {
        RestAssuredMockMvc.given()
            .header(CLIENT_TYPE_HEADER, "IOS")
            .header(APP_VERSION_HEADER, "1.2.0")
            .when()
            .get("/terms")
            .then()
            .statusCode(200);
    }

    @DisplayName("검증 제외 경로는 헤더가 없어도 차단하지 않는다.")
    @Test
    void excludedPathIsNotBlocked() {
        int statusCode = RestAssuredMockMvc.given()
            .when()
            .get("/actuator/health")
            .then()
            .extract()
            .statusCode();

        assertThat(statusCode).isNotEqualTo(426);
    }
}
