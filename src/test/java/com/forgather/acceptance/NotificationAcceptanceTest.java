package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.notification.model.Notification;
import com.forgather.domain.notification.repository.NotificationRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.NotificationFixture;
import com.forgather.global.auth.util.JwtTokenProvider;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;

@DisplayName("인수 테스트: 알림함")
@AutoConfigureMockMvc
class NotificationAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Host host;
    private String accessToken;

    @BeforeEach
    void setUpMockMvc() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        host = hostRepository.save(HostFixture.createHost());
        accessToken = jwtTokenProvider.generateAccessToken(host.getId());
    }

    private Notification saveNotification(Host host) {
        return notificationRepository.save(NotificationFixture.createNotificationWithHost(host));
    }

    /**
     * created_at은 초 단위라 한 테스트에서 저장한 알림은 대부분 같은 값을 가진다. 정렬 검증을 위해 SQL로 직접 바꾼다.
     */
    private void updateCreatedAt(Notification notification, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE notification SET created_at = ? WHERE id = ?", createdAt, notification.getId());
    }

    private JsonPath getNotifications() {
        return RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .when()
            .get("/notifications")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath();
    }

    private long getUnreadCount() {
        return RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .when()
            .get("/notifications/unread-count")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath()
            .getLong("data.unreadCount");
    }

    private void readAll() {
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .when()
            .patch("/notifications/read")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("code", equalTo("SUCCESS"));
    }

    @DisplayName("알림함 목록을 조회하면 200과 함께 페이지 정보와 알림 항목을 응답한다")
    @Test
    void getNotificationsResponseFormat() {
        // given
        Notification notification = saveNotification(host);

        // when
        JsonPath result = getNotifications();

        // then
        assertAll(
            () -> assertThat(result.getString("code")).isEqualTo("SUCCESS"),
            () -> assertThat(result.getMap("data").keySet()).containsExactlyInAnyOrder(
                "notifications", "currentPage", "pageSize", "totalCount", "totalPages"),
            () -> assertThat(result.getMap("data.notifications[0]").keySet()).containsExactlyInAnyOrder(
                "id", "type", "title", "body", "targetType", "targetKey", "readAt", "createdAt"),
            () -> assertThat(result.getLong("data.notifications[0].id")).isEqualTo(notification.getId()),
            () -> assertThat(result.getString("data.notifications[0].type")).isEqualTo("PLACEHOLDER"),
            () -> assertThat(result.getString("data.notifications[0].targetType")).isEqualTo("SPACE"),
            () -> assertThat(result.getString("data.notifications[0].targetKey"))
                .isEqualTo(NotificationFixture.SPACE_CODE),
            () -> assertThat(result.getString("data.notifications[0].readAt")).isNull(),
            () -> assertThat(LocalDateTime.parse(result.getString("data.notifications[0].createdAt"))).isNotNull(),
            () -> assertThat(result.getInt("data.currentPage")).isEqualTo(1),
            () -> assertThat(result.getInt("data.pageSize")).isEqualTo(15),
            () -> assertThat(result.getLong("data.totalCount")).isEqualTo(1),
            () -> assertThat(result.getInt("data.totalPages")).isEqualTo(1)
        );
    }

    @DisplayName("알림함 목록은 삭제된 알림과 다른 호스트의 알림을 빼고 최신순으로 응답한다")
    @Test
    void getNotificationsOrderedAndFiltered() {
        // given
        Notification oldest = saveNotification(host);
        Notification newest = saveNotification(host);
        Notification middle = saveNotification(host);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        updateCreatedAt(oldest, now.minusDays(3));
        updateCreatedAt(newest, now.minusDays(1));
        updateCreatedAt(middle, now.minusDays(2));
        notificationRepository.save(NotificationFixture.createDeletedNotificationWithHost(host));
        saveNotification(hostRepository.save(HostFixture.createHost()));

        // when
        JsonPath result = getNotifications();

        // then
        assertThat(result.getList("data.notifications.id", Long.class))
            .containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    @DisplayName("페이지와 크기를 지정하면 해당 페이지의 알림과 페이지 정보를 응답한다")
    @Test
    void getNotificationsWithPageAndSize() {
        // given
        for (int i = 0; i < 5; i++) {
            saveNotification(host);
        }

        // when
        JsonPath result = RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(accessToken))
            .queryParam("page", 2)
            .queryParam("size", 2)
            .when()
            .get("/notifications")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .jsonPath();

        // then
        assertAll(
            () -> assertThat(result.getList("data.notifications")).hasSize(2),
            () -> assertThat(result.getInt("data.currentPage")).isEqualTo(2),
            () -> assertThat(result.getInt("data.pageSize")).isEqualTo(2),
            () -> assertThat(result.getLong("data.totalCount")).isEqualTo(5),
            () -> assertThat(result.getInt("data.totalPages")).isEqualTo(3)
        );
    }

    @DisplayName("읽지 않은 알림 개수를 조회하면 읽은 알림과 삭제된 알림을 뺀 개수를 응답한다")
    @Test
    void getUnreadCountExcludesReadAndDeleted() {
        // given
        saveNotification(host);
        saveNotification(host);
        notificationRepository.save(NotificationFixture.createReadNotificationWithHost(host));
        notificationRepository.save(NotificationFixture.createDeletedNotificationWithHost(host));
        saveNotification(hostRepository.save(HostFixture.createHost()));

        // when
        long unreadCount = getUnreadCount();

        // then
        assertThat(unreadCount).isEqualTo(2);
    }

    @DisplayName("전체 읽음 처리를 하면 200을 응답하고 읽지 않은 알림 개수가 0이 된다")
    @Test
    void readAllMakesUnreadCountZero() {
        // given
        saveNotification(host);
        saveNotification(host);

        // when
        readAll();

        // then
        assertAll(
            () -> assertThat(getUnreadCount()).isZero(),
            () -> assertThat(getNotifications().getList("data.notifications.readAt")).doesNotContainNull()
        );
    }

    @DisplayName("전체 읽음 처리를 두 번 호출해도 두 번 모두 200을 응답한다")
    @Test
    void readAllIsIdempotent() {
        // given
        saveNotification(host);
        readAll();

        // when
        readAll();

        // then
        assertThat(getUnreadCount()).isZero();
    }

    @DisplayName("목록을 조회한 뒤 들어온 알림도 전체 읽음 처리된다")
    @Test
    void readAllIncludesNotificationsArrivedAfterListing() {
        // given
        saveNotification(host);
        getNotifications();
        saveNotification(host);

        // when
        readAll();

        // then
        assertThat(getUnreadCount()).isZero();
    }

    @DisplayName("다른 호스트의 알림은 전체 읽음 처리되지 않는다")
    @Test
    void readAllDoesNotAffectOtherHosts() {
        // given
        Host otherHost = hostRepository.save(HostFixture.createHost());
        Notification otherHostNotification = saveNotification(otherHost);
        saveNotification(host);

        // when
        readAll();

        // then
        LocalDateTime otherHostReadAt = jdbcTemplate.queryForObject(
            "SELECT read_at FROM notification WHERE id = ?", LocalDateTime.class, otherHostNotification.getId());
        assertThat(otherHostReadAt).isNull();
    }

    @DisplayName("로그인하지 않고 알림함 목록을 조회하면 401 UNAUTHORIZED로 응답한다")
    @Test
    void getNotificationsWithoutLogin() {
        // when & then
        RestAssuredMockMvc.given()
            .when()
            .get("/notifications")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("로그인하지 않고 읽지 않은 알림 개수를 조회하면 401 UNAUTHORIZED로 응답한다")
    @Test
    void getUnreadCountWithoutLogin() {
        // when & then
        RestAssuredMockMvc.given()
            .when()
            .get("/notifications/unread-count")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("로그인하지 않고 전체 읽음 처리를 요청하면 401 UNAUTHORIZED로 응답한다")
    @Test
    void readAllWithoutLogin() {
        // when & then
        RestAssuredMockMvc.given()
            .when()
            .patch("/notifications/read")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }
}
