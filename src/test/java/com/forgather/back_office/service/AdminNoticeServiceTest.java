package com.forgather.back_office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminNoticeResponse;
import com.forgather.back_office.dto.NoticeDetailResponse;
import com.forgather.back_office.dto.NoticeSummaryResponse;
import com.forgather.back_office.dto.RegisterNoticeRequest;
import com.forgather.container.TestOnContainer;
import com.forgather.domain.notice.model.Notice;
import com.forgather.domain.notice.repository.NoticeRepository;
import com.forgather.global.exception.NotFoundException;

import jakarta.persistence.EntityManager;

@Transactional
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AdminNoticeServiceTest extends TestOnContainer {

    @Autowired
    private AdminNoticeService adminNoticeService;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("공지를 저장하고 재조회하면 최대 본문 원문과 자동 생성 시각이 보존된다")
    @Test
    void saveAndReloadPreservesBodyWhitespaceAndSetsCreatedAt() {
        // given
        String title = "  공지 제목  ";
        String body = "  첫 줄\n\n둘째 줄  \n" + "👨‍👩‍👧‍👦".repeat(9986);
        LocalDateTime before = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        // when
        NoticeDetailResponse response = adminNoticeService.create(new RegisterNoticeRequest(title, body));
        entityManager.flush();
        entityManager.clear();
        Notice reloaded = noticeRepository.getByIdAndDeletedAtIsNullOrThrow(response.id());
        LocalDateTime after = LocalDateTime.now().plusSeconds(1).truncatedTo(ChronoUnit.SECONDS);

        // then
        assertAll(
            () -> assertThat(reloaded.getId()).isNotNull(),
            () -> assertThat(reloaded.getTitle()).isEqualTo(title),
            () -> assertThat(reloaded.getBody()).isEqualTo(body),
            () -> assertThat(reloaded.getCreatedAt()).isBetween(before, after),
            () -> assertThat(reloaded.getCreatedAt()).isEqualTo(response.createdAt().plusNanos(500_000_000).truncatedTo(ChronoUnit.SECONDS))
        );
    }

    @DisplayName("공지 목록이 비어 있으면 첫 페이지와 고정 크기의 빈 결과를 반환한다")
    @Test
    void getEmptyNotices() {
        // given
        // when
        AdminNoticeResponse result = adminNoticeService.getAllNotices(PageRequest.of(0, 1));

        // then
        assertAll(
            () -> assertThat(result.notices()).isEmpty(),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(15),
            () -> assertThat(result.totalCount()).isZero(),
            () -> assertThat(result.totalPages()).isZero()
        );
    }

    @DisplayName("목록은 요청 크기와 정렬을 무시하고 생성일과 id 역순으로 15건씩 조회한다")
    @Test
    void listUsesFixedSizeAndStableLatestOrder() {
        // given
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            ids.add(noticeRepository.save(new Notice("공지 " + i, "본문")).getId());
        }
        entityManager.flush();
        for (int i = 0; i < 16; i++) {
            jdbcTemplate.update("UPDATE notice SET created_at = ? WHERE id = ?",
                LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(i / 2), ids.get(i));
        }
        entityManager.clear();
        List<Long> expected = ids.reversed();

        // when
        AdminNoticeResponse first = adminNoticeService.getAllNotices(PageRequest.of(0, 1, Sort.by("title")));
        AdminNoticeResponse second = adminNoticeService.getAllNotices(PageRequest.of(1, 50));

        // then
        assertAll(
            () -> assertThat(first.notices()).extracting(NoticeSummaryResponse::id).containsExactlyElementsOf(expected.subList(0, 15)),
            () -> assertThat(second.notices()).extracting(NoticeSummaryResponse::id).containsExactly(expected.get(15)),
            () -> assertThat(first.currentPage()).isEqualTo(1),
            () -> assertThat(second.currentPage()).isEqualTo(2),
            () -> assertThat(first.pageSize()).isEqualTo(15),
            () -> assertThat(second.pageSize()).isEqualTo(15),
            () -> assertThat(first.totalCount()).isEqualTo(16),
            () -> assertThat(second.totalCount()).isEqualTo(16),
            () -> assertThat(first.totalPages()).isEqualTo(2),
            () -> assertThat(second.totalPages()).isEqualTo(2)
        );
    }

    @DisplayName("상세를 조회하고 논리 삭제된 공지와 없는 공지는 조회에서 제외한다")
    @Test
    void getNoticeAndExcludeDeletedNotices() {
        // given
        Notice active = noticeRepository.save(new Notice("제목", "본문"));
        Notice deleted = noticeRepository.save(new Notice("삭제 제목", "삭제 본문"));
        deleted.delete();
        entityManager.flush();
        entityManager.clear();

        // when
        NoticeDetailResponse result = adminNoticeService.getNotice(active.getId());
        AdminNoticeResponse list = adminNoticeService.getAllNotices(PageRequest.of(0, 15));

        // then
        assertAll(
            () -> assertThat(result.id()).isEqualTo(active.getId()),
            () -> assertThat(result.title()).isEqualTo("제목"),
            () -> assertThat(result.body()).isEqualTo("본문"),
            () -> assertThat(result.createdAt()).isNotNull(),
            () -> assertThat(list.notices()).extracting(NoticeSummaryResponse::id).containsExactly(active.getId()),
            () -> assertThatThrownBy(() -> adminNoticeService.getNotice(deleted.getId())).isInstanceOf(NotFoundException.class),
            () -> assertThatThrownBy(() -> adminNoticeService.getNotice(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class)
        );
    }
}
