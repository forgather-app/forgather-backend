package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;

@AutoConfigureMockMvc
@Sql("/notice-list-data.sql")
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class NoticeAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("로그인 없이 공지 목록을 조회하면 공개 요약 필드와 최초 작성일을 반환한다")
    @Test
    void getNoticesWithoutLogin() {
        // given

        // when
        JsonPath result = getNotices("");

        // then
        assertAll(
            () -> assertThat(result.getMap("").keySet()).containsExactlyInAnyOrder("code", "message", "data"),
            () -> assertThat(result.getString("code")).isEqualTo("SUCCESS"),
            () -> assertThat(result.getString("message")).isNull(),
            () -> assertThat(result.getMap("data").keySet()).containsExactlyInAnyOrder(
                "notices", "currentPage", "pageSize", "totalCount", "totalPages"),
            () -> assertThat(result.getList("data.notices")).hasSize(15),
            () -> assertThat(result.getMap("data.notices[0]").keySet())
                .containsExactlyInAnyOrder("id", "title", "createdAt"),
            () -> assertThat(result.getLong("data.notices[0].id")).isEqualTo(1L),
            () -> assertThat(result.getString("data.notices[0].title")).isEqualTo("  공지 제목  "),
            () -> assertThat(LocalDateTime.parse(result.getString("data.notices[0].createdAt")))
                .isEqualTo(LocalDateTime.of(2026, 1, 21, 0, 0))
        );
    }

    @DisplayName("로그인 없이 공지 상세를 조회하면 본문 원문과 최초 작성일을 반환한다")
    @Test
    void getNoticeWithoutLogin() {
        // given
        String body = "  첫 줄\n\n둘째 줄  \n<script>alert('공지')</script>";

        // when
        JsonPath result = RestAssuredMockMvc.given().get("/notices/{id}", 1L)
            .then().statusCode(200).extract().jsonPath();

        // then
        assertAll(
            () -> assertThat(result.getString("code")).isEqualTo("SUCCESS"),
            () -> assertThat(result.getString("message")).isNull(),
            () -> assertThat(result.getMap("data").keySet())
                .containsExactlyInAnyOrder("id", "title", "body", "createdAt"),
            () -> assertThat(result.getLong("data.id")).isEqualTo(1L),
            () -> assertThat(result.getString("data.title")).isEqualTo("  공지 제목  "),
            () -> assertThat(result.getString("data.body")).isEqualTo(body),
            () -> assertThat(LocalDateTime.parse(result.getString("data.createdAt")))
                .isEqualTo(LocalDateTime.of(2026, 1, 21, 0, 0))
        );
    }

    @DisplayName("공지가 없으면 기본 첫 페이지와 빈 목록을 반환한다")
    @Test
    @Sql("/cleanup.sql")
    void getEmptyNotices() {
        // given
        // when
        JsonPath result = getNotices("");

        // then
        assertThat(result.getList("data.notices")).isEmpty();
        assertMetadata(result, 1, 0, 0);
    }

    @DisplayName("크기와 정렬 요청에 관계없이 생성일과 id 내림차순으로 15개씩 조회한다")
    @Test
    void listUsesFixedSizeAndStableLatestOrder() {
        // given
        List<Long> expectedFirstPage = List.of(1L, 16L, 15L, 14L, 13L, 12L, 11L, 10L,
            9L, 8L, 7L, 6L, 5L, 4L, 3L);

        // when
        JsonPath first = getNotices("?page=1&size=1&sort=title,asc");
        JsonPath second = getNotices("?page=2&size=50");
        JsonPath third = getNotices("?page=3");
        List<Long> firstIds = first.getList("data.notices.id", Long.class);
        List<Long> secondIds = second.getList("data.notices.id", Long.class);

        // then
        assertAll(
            () -> assertThat(firstIds).containsExactlyElementsOf(expectedFirstPage),
            () -> assertThat(secondIds).containsExactly(2L),
            () -> assertThat(firstIds).doesNotContainAnyElementsOf(secondIds),
            () -> assertThat(third.getList("data.notices")).isEmpty()
        );
        assertMetadata(first, 1, 16, 2);
        assertMetadata(second, 2, 16, 2);
        assertMetadata(third, 3, 16, 2);
    }

    @DisplayName("삭제된 공지는 목록과 총 개수에서 제외한다")
    @Test
    void excludeDeletedNotices() {
        // given

        // when
        JsonPath result = getNotices("");

        // then
        assertThat(result.getList("data.notices.id", Long.class)).doesNotContain(17L);
        assertThat(result.getLong("data.totalCount")).isEqualTo(16);
    }

    @DisplayName("없거나 삭제된 공지 상세는 해당 id를 포함한 NOT_FOUND를 반환한다")
    @Test
    void getMissingOrDeletedNotice() {
        // given

        // when & then
        for (Long id : List.of(999999L, 17L)) {
            JsonPath result = RestAssuredMockMvc.given().get("/notices/{id}", id)
                .then().statusCode(404).extract().jsonPath();
            assertAll(
                () -> assertThat(result.getString("code")).isEqualTo("NOT_FOUND"),
                () -> assertThat(result.<Object>get("data")).isNull(),
                () -> assertThat(result.getString("message")).contains(id.toString())
            );
        }
    }

    private JsonPath getNotices(String query) {
        return RestAssuredMockMvc.given().get("/notices" + query)
            .then().statusCode(200).extract().jsonPath();
    }

    private void assertMetadata(JsonPath result, int currentPage, long totalCount, int totalPages) {
        assertAll(
            () -> assertThat(result.getInt("data.currentPage")).isEqualTo(currentPage),
            () -> assertThat(result.getInt("data.pageSize")).isEqualTo(15),
            () -> assertThat(result.getLong("data.totalCount")).isEqualTo(totalCount),
            () -> assertThat(result.getInt("data.totalPages")).isEqualTo(totalPages)
        );
    }
}
