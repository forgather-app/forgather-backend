package com.forgather.acceptance;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.dto.AdminNoticeResponse;
import com.forgather.back_office.dto.NoticeDetailResponse;
import com.forgather.back_office.dto.RegisterNoticeRequest;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.fixture.AdminUserFixture;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.util.JwtTokenProvider;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;

@AutoConfigureMockMvc
class AdminNoticeAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String sessionId;

    @BeforeEach
    void setUp() {
        AdminUser admin = adminUserRepository.save(AdminUserFixture.createAdminUser("공지 관리자"));
        sessionId = sessionManager.createSession(admin.getId(), admin.getUsername()).getSessionId().getValue();
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("관리자가 공지를 생성하면 Location과 목록 및 상세 계약을 반환한다")
    @Test
    void createAndReadNoticeAsAdmin() {
        // given
        String body = "  첫 줄\n\n둘째 줄  \n<script>alert('공지')</script>";

        // when
        ValidatableMockMvcResponse creation = givenWithSession().contentType(ContentType.JSON)
            .body(new RegisterNoticeRequest("새 공지", body)).post("/admin/notices").then().statusCode(201);
        NoticeDetailResponse created = creation.extract().as(NoticeDetailResponse.class);
        AdminNoticeResponse list = givenWithSession().queryParam("page", 1).get("/admin/notices")
            .then().statusCode(200).extract().as(AdminNoticeResponse.class);
        NoticeDetailResponse detail = givenWithSession().get("/admin/notices/{id}", created.id())
            .then().statusCode(200).extract().as(NoticeDetailResponse.class);

        // then
        assertAll(
            () -> assertThat(created.id()).isNotNull(),
            () -> assertThat(creation.extract().header("Location")).isEqualTo("/admin/notices/" + created.id()),
            () -> assertThat(created.title()).isEqualTo("새 공지"),
            () -> assertThat(created.body()).isEqualTo(body),
            () -> assertThat(created.createdAt()).isNotNull(),
            () -> assertThat(list.currentPage()).isEqualTo(1),
            () -> assertThat(list.notices()).hasSize(1),
            () -> assertThat(list.notices().getFirst().title()).isEqualTo("새 공지"),
            () -> assertThat(list.notices().getFirst().createdAt()).isEqualTo(created.createdAt().plusNanos(500_000_000).truncatedTo(ChronoUnit.SECONDS)),
            () -> assertThat(detail.id()).isEqualTo(created.id()),
            () -> assertThat(detail.title()).isEqualTo("새 공지"),
            () -> assertThat(detail.body()).isEqualTo(body),
            () -> assertThat(detail.createdAt()).isEqualTo(list.notices().getFirst().createdAt())
        );
    }

    @DisplayName("제목과 본문이 누락되거나 비어 있거나 제한을 넘으면 400을 반환한다")
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("invalidRequests")
    void rejectInvalidRequest(Map<String, Object> body) {
        // given
        // when & then
        givenWithSession().contentType(ContentType.JSON).body(body).post("/admin/notices")
            .then().statusCode(400);
    }

    static Stream<Arguments> invalidRequests() {
        Stream.Builder<Arguments> requests = Stream.builder();
        for (String field : new String[]{"title", "body"}) {
            Map<String, Object> missing = new HashMap<>(Map.of("title", "제목", "body", "본문"));
            missing.remove(field);
            requests.add(Arguments.of(missing));
            for (String invalid : new String[]{null, "", " \n\t", "가".repeat(field.equals("title") ? 101 : 10001),
                "👨‍👩‍👧‍👦".repeat(field.equals("title") ? 101 : 10001)}) {
                Map<String, Object> request = new HashMap<>(Map.of("title", "제목", "body", "본문"));
                request.put(field, invalid);
                requests.add(Arguments.of(request));
            }
        }
        return requests.build();
    }

    @DisplayName("한글과 복합 이모지 최대 표시 글자 수의 요청은 201을 반환한다")
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("maximumRequests")
    void acceptMaximumRequest(String title, String body) {
        // given
        // when & then
        givenWithSession().contentType(ContentType.JSON).body(new RegisterNoticeRequest(title, body))
            .post("/admin/notices").then().statusCode(201);
    }

    static Stream<Arguments> maximumRequests() {
        return Stream.of(Arguments.of("가".repeat(100), "나".repeat(10000)),
            Arguments.of("👨‍👩‍👧‍👦".repeat(100), "👨‍👩‍👧‍👦".repeat(10000)));
    }

    @DisplayName("관리자 세션이 없거나 잘못되었거나 HOST JWT만 있으면 모든 공지 API는 401이다")
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("unauthorizedRequests")
    void rejectUnauthorizedRequest(String method, String path, String authentication) {
        // given
        MockMvcRequestSpecification request = RestAssuredMockMvc.given().contentType(ContentType.JSON)
            .body(new RegisterNoticeRequest("제목", "본문"));
        if (authentication.equals("invalid")) {
            request.postProcessors(withCookie(SESSION_COOKIE_NAME, "invalid-session"));
        } else if (authentication.equals("host")) {
            Host host = hostRepository.save(HostFixture.createHost());
            request.postProcessors(withAccessToken(jwtTokenProvider.generateAccessToken(host.getId())));
        }

        // when & then
        request.request(method, path).then().statusCode(401);
    }

    static Stream<Arguments> unauthorizedRequests() {
        return Stream.of("none", "invalid", "host").flatMap(authentication -> Stream.of(
            Arguments.of("POST", "/admin/notices", authentication),
            Arguments.of("GET", "/admin/notices", authentication),
            Arguments.of("GET", "/admin/notices/1", authentication)));
    }

    @DisplayName("없는 공지 상세를 요청하면 404를 반환한다")
    @Test
    void getMissingNotice() {
        // given
        // when & then
        givenWithSession().get("/admin/notices/999999").then().statusCode(404);
    }

    @DisplayName("관리자 세션으로 공지 화면을 요청하면 실제 목록과 등록 폼 HTML을 반환한다")
    @Test
    void getNoticeViewAsAdmin() {
        // given
        // when
        String html = givenWithSession().get("/view/admin/notices").then().statusCode(200)
            .contentType(ContentType.HTML).extract().asString();

        // then
        assertThat(html).contains("공지 관리", "id=\"noticeForm\"", "id=\"noticeTitle\"",
            "id=\"noticeBody\"", "/js/admin/notices.js");
    }

    @DisplayName("관리자 세션이 없거나 잘못되었거나 HOST JWT만 있으면 공지 화면은 로그인으로 이동한다")
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"none", "invalid", "host"})
    void redirectNoticeViewWithoutAdminSession(String authentication) {
        // given
        MockMvcRequestSpecification request = RestAssuredMockMvc.given();
        if (authentication.equals("invalid")) {
            request.postProcessors(withCookie(SESSION_COOKIE_NAME, "invalid-session"));
        } else if (authentication.equals("host")) {
            Host host = hostRepository.save(HostFixture.createHost());
            request.postProcessors(withAccessToken(jwtTokenProvider.generateAccessToken(host.getId())));
        }

        // when
        String location = request.get("/view/admin/notices").then().statusCode(302)
            .extract().header("Location");

        // then
        assertThat(location).isEqualTo("/view/admin/login");
    }

    private MockMvcRequestSpecification givenWithSession() {
        return RestAssuredMockMvc.given().postProcessors(withCookie(SESSION_COOKIE_NAME, sessionId));
    }
}
