package com.forgather.acceptance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.forgather.container.TestOnContainer;
import com.forgather.global.auth.util.AuthCookieProvider;

import io.restassured.RestAssured;
import jakarta.servlet.http.Cookie;

@ActiveProfiles("test")
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AcceptanceTest extends TestOnContainer {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() throws IOException {
        if (RestAssured.port == RestAssured.UNDEFINED_PORT) {
            RestAssured.port = port;
        }
    }

    /**
     * access token 쿠키를 요청에 추가한다.
     * <p>
     * RestAssuredMockMvc의 {@code .cookie()}는 리플렉션으로 {@code MockHttpServletRequestBuilder.cookie(Cookie...)}를
     * 찾는데, Spring 6.2에서는 같은 시그니처의 bridge 메서드가 함께 존재해 JVM 실행마다 잡히는 메서드가 달라진다.
     * bridge 메서드가 잡히면 "argument type mismatch"로 실패하므로 {@link RequestPostProcessor}로 직접 쿠키를 넣는다.
     */
    protected static RequestPostProcessor withAccessToken(String accessToken) {
        return withCookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, accessToken);
    }

    protected static RequestPostProcessor withRefreshToken(String refreshToken) {
        return withCookie(AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME, refreshToken);
    }

    protected static RequestPostProcessor withCookie(String name, String value) {
        return request -> {
            List<Cookie> cookies = new ArrayList<>();
            if (request.getCookies() != null) {
                cookies.addAll(Arrays.asList(request.getCookies()));
            }
            cookies.add(new Cookie(name, value));
            request.setCookies(cookies.toArray(Cookie[]::new));
            return request;
        };
    }
}
