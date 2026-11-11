package com.forgather.acceptance;

import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;

@AutoConfigureMockMvc
class SwaggerAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("Swagger의 약관 조회 API에 클라이언트 타입 헤더 기본값 WEB이 포함된다.")
    @Test
    void termsApiHasWebClientTypeHeader() {
        // given
        RestAssuredMockMvc.mockMvc(mockMvc);

        // when
        MockMvcResponse response = RestAssuredMockMvc.given()
            .when()
            .get("/v3/api-docs");

        // then
        response.then()
            .statusCode(200)
            .body("paths.'/terms'.get.parameters.findAll { it.name == 'X-Client-Type' "
                + "&& it.in == 'header' }.schema.default", hasItem("WEB"));
    }
}
