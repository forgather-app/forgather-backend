package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.notice.model.Notice;
import com.forgather.domain.notice.repository.NoticeRepository;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;

@AutoConfigureMockMvc
class NoticeAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("로그인 없이 공지 목록을 조회하면 공개 요약 필드와 최초 작성일을 반환한다")
    @Test
    void getNoticesWithoutLogin() {
        // given
        Notice notice = noticeRepository.save(new Notice("  공지 제목  ", "본문"));
        Notice persisted = noticeRepository.getByIdAndDeletedAtIsNullOrThrow(notice.getId());

        // when
        JsonPath result = getNotices("");

        // then
        assertAll(
            () -> assertThat(result.getMap("").keySet()).containsExactlyInAnyOrder("code", "message", "data"),
            () -> assertThat(result.getString("code")).isEqualTo("SUCCESS"),
            () -> assertThat(result.getString("message")).isNull(),
            () -> assertThat(result.getMap("data").keySet()).containsExactlyInAnyOrder(
                "notices", "currentPage", "pageSize", "totalCount", "totalPages"),
            () -> assertThat(result.getList("data.notices")).hasSize(1),
            () -> assertThat(result.getMap("data.notices[0]").keySet())
                .containsExactlyInAnyOrder("id", "title", "createdAt"),
            () -> assertThat(result.getLong("data.notices[0].id")).isEqualTo(notice.getId()),
            () -> assertThat(result.getString("data.notices[0].title")).isEqualTo(notice.getTitle()),
            () -> assertThat(LocalDateTime.parse(result.getString("data.notices[0].createdAt")))
                .isEqualTo(persisted.getCreatedAt())
        );
    }

    @DisplayName("로그인 없이 공지 상세를 조회하면 본문 원문과 최초 작성일을 반환한다")
    @Test
    void getNoticeWithoutLogin() {
        // given
        String body = "  첫 줄\n\n둘째 줄  \n<script>alert('공지')</script>";
        Notice notice = noticeRepository.save(new Notice("  새 공지  ", body));
        Notice persisted = noticeRepository.getByIdAndDeletedAtIsNullOrThrow(notice.getId());

        // when
        JsonPath result = RestAssuredMockMvc.given().get("/notices/{id}", notice.getId())
            .then().statusCode(200).extract().jsonPath();

        // then
        assertAll(
            () -> assertThat(result.getString("code")).isEqualTo("SUCCESS"),
            () -> assertThat(result.getString("message")).isNull(),
            () -> assertThat(result.getMap("data").keySet())
                .containsExactlyInAnyOrder("id", "title", "body", "createdAt"),
            () -> assertThat(result.getLong("data.id")).isEqualTo(notice.getId()),
            () -> assertThat(result.getString("data.title")).isEqualTo(notice.getTitle()),
            () -> assertThat(result.getString("data.body")).isEqualTo(body),
            () -> assertThat(LocalDateTime.parse(result.getString("data.createdAt")))
                .isEqualTo(persisted.getCreatedAt())
        );
    }

    @DisplayName("공지가 없으면 기본 첫 페이지와 빈 목록을 반환한다")
    @Test
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
        List<Long> ids = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        for (int index = 0; index < 16; index++) {
            Notice notice = noticeRepository.save(new Notice("공지 " + index, "본문"));
            ids.add(notice.getId());
            jdbcTemplate.update("UPDATE notice SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(index == 0 ? base.plusDays(20) : base.plusDays(index / 2)),
                notice.getId());
        }
        List<Long> expectedFirstPage = List.of(ids.get(0), ids.get(15), ids.get(14), ids.get(13),
            ids.get(12), ids.get(11), ids.get(10), ids.get(9), ids.get(8), ids.get(7), ids.get(6),
            ids.get(5), ids.get(4), ids.get(3), ids.get(2));

        // when
        JsonPath first = getNotices("?page=1&size=1&sort=title,asc");
        JsonPath second = getNotices("?page=2&size=50");
        JsonPath third = getNotices("?page=3");
        List<Long> firstIds = first.getList("data.notices.id", Long.class);
        List<Long> secondIds = second.getList("data.notices.id", Long.class);

        // then
        assertAll(
            () -> assertThat(firstIds).containsExactlyElementsOf(expectedFirstPage),
            () -> assertThat(secondIds).containsExactly(ids.get(1)),
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
        Notice active = noticeRepository.save(new Notice("게시 공지", "본문"));
        Notice deleted = noticeRepository.save(new Notice("삭제 공지", "본문"));
        deleted.delete();
        noticeRepository.save(deleted);

        // when
        JsonPath result = getNotices("");

        // then
        assertThat(result.getList("data.notices.id", Long.class)).containsExactly(active.getId());
        assertThat(result.getLong("data.totalCount")).isEqualTo(1);
    }

    @DisplayName("없거나 삭제된 공지 상세는 해당 id를 포함한 NOT_FOUND를 반환한다")
    @Test
    void getMissingOrDeletedNotice() {
        // given
        Notice deleted = noticeRepository.save(new Notice("삭제 공지", "본문"));
        deleted.delete();
        noticeRepository.save(deleted);

        // when & then
        for (Long id : List.of(999999L, deleted.getId())) {
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
