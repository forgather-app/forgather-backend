package com.forgather.acceptance;

import static com.forgather.back_office.auth.session.SessionConstants.SESSION_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.forgather.back_office.auth.session.SessionManager;
import com.forgather.back_office.model.AdminUser;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.back_office.repository.jpa.AdminPdfExportFailureJpaRepository;
import com.forgather.domain.export.model.PdfExportFailureProcessingStatus;
import com.forgather.domain.export.model.PdfExportFailureStage;
import com.forgather.domain.export.model.SpacePdfExportFailure;
import com.forgather.fixture.AdminUserFixture;
import com.forgather.fixture.SpacePdfExportFailureFixture;

@AutoConfigureMockMvc
class AdminPdfExportFailureAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminPdfExportFailureJpaRepository failureRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private SessionManager sessionManager;

    private String sessionId;

    @BeforeEach
    void prepareAdmin() {
        AdminUser admin = adminUserRepository.save(AdminUserFixture.createAdminUser("PDF 실패 관리자"));
        sessionId = sessionManager.createSession(admin.getId(), admin.getUsername()).getSessionId().getValue();
    }

    @DisplayName("관리자 세션 없이 실패 목록을 조회할 수 없다")
    @Test
    void rejectsListWithoutAdminSession() throws Exception {
        mockMvc.perform(get("/admin/pdf/failures")).andExpect(status().isUnauthorized());
    }

    @DisplayName("관리자 세션 없이 실패 상세를 조회할 수 없다")
    @Test
    void rejectsDetailWithoutAdminSession() throws Exception {
        mockMvc.perform(get("/admin/pdf/failures/1")).andExpect(status().isUnauthorized());
    }

    @DisplayName("관리자 세션 없이 실패 조회 화면에 접근하면 로그인 화면으로 이동한다")
    @Test
    void redirectsViewWithoutAdminSession() throws Exception {
        mockMvc.perform(get("/view/admin/pdf-export-failures")).andExpect(status().is3xxRedirection());
    }

    @DisplayName("실패 목록은 요약 정보와 실패 시각 및 ID 내림차순의 페이지 정보를 반환한다")
    @Test
    void listsNewestFailuresWithStablePagination() throws Exception {
        // given
        SpacePdfExportFailure oldest = saveFailure("request-1", "2026-11-08T00:00:00");
        SpacePdfExportFailure previous = saveFailure("request-2", "2026-11-09T00:00:00");
        SpacePdfExportFailure latest = saveFailure("request-3", "2026-11-09T00:00:00");

        // when & then
        mockMvc.perform(get("/admin/pdf/failures").param("page", "1").param("size", "2")
                .with(withCookie(SESSION_COOKIE_NAME, sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failures", hasSize(2)))
            .andExpect(jsonPath("$.failures[0].id").value(latest.getId()))
            .andExpect(jsonPath("$.failures[0].requestId").value("request-3"))
            .andExpect(jsonPath("$.failures[0].jobId").value(nullValue()))
            .andExpect(jsonPath("$.failures[0].stage").value("UNKNOWN"))
            .andExpect(jsonPath("$.failures[0].errorType").value(nullValue()))
            .andExpect(jsonPath("$.failures[0].processingStatus").value("PENDING"))
            .andExpect(jsonPath("$.failures[0].failedAt").value("2026-11-09T00:00:00"))
            .andExpect(jsonPath("$.failures[1].id").value(previous.getId()))
            .andExpect(jsonPath("$.failures[0].rawEvent").doesNotExist())
            .andExpect(jsonPath("$.totalCount").value(3))
            .andExpect(jsonPath("$.currentPage").value(1))
            .andExpect(jsonPath("$.pageSize").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/admin/pdf/failures").param("page", "2").param("size", "2")
                .with(withCookie(SESSION_COOKIE_NAME, sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failures", hasSize(1)))
            .andExpect(jsonPath("$.failures[0].id").value(oldest.getId()));
    }

    @DisplayName("실패 상세는 식별 정보와 실패 단계, 처리 상태, 오류 원문 및 시각을 반환한다")
    @Test
    void returnsFailureDetail() throws Exception {
        // given
        SpacePdfExportFailure failure = saveFailure("request-1", "2026-11-09T00:00:00.123456");

        // when & then
        MvcResult result = mockMvc.perform(get("/admin/pdf/failures/{id}", failure.getId())
                .with(withCookie(SESSION_COOKIE_NAME, sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(failure.getId()))
            .andExpect(jsonPath("$.jobId").value(nullValue()))
            .andExpect(jsonPath("$.stage").value("UNKNOWN"))
            .andExpect(jsonPath("$.functionArn").value("arn:aws:lambda:test"))
            .andExpect(jsonPath("$.requestId").value("request-1"))
            .andExpect(jsonPath("$.failureCondition").value("RetriesExhausted"))
            .andExpect(jsonPath("$.errorType").value(nullValue()))
            .andExpect(jsonPath("$.processingNote").value(nullValue()))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.approximateInvokeCount").value(nullValue()))
            .andExpect(jsonPath("$.processingStatus").value("PENDING"))
            .andExpect(jsonPath("$.errorMessage").value("<script>alert('failure')</script>"))
            .andExpect(jsonPath("$.rawEvent").isString())
            .andExpect(jsonPath("$.failedAt").value("2026-11-09T00:00:00.123456"))
            .andReturn();
        String rawEvent = objectMapper.readTree(result.getResponse().getContentAsString()).get("rawEvent").asText();
        assertThat(objectMapper.readTree(rawEvent)).isEqualTo(objectMapper.readTree("{\"detail\":\"failed\"}"));
    }

    @DisplayName("실패 기록이 없으면 빈 목록을 반환한다")
    @Test
    void returnsEmptyList() throws Exception {
        mockMvc.perform(get("/admin/pdf/failures").with(withCookie(SESSION_COOKIE_NAME, sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failures", hasSize(0)))
            .andExpect(jsonPath("$.totalCount").value(0));
    }

    @DisplayName("존재하지 않는 실패 ID의 상세 조회는 404를 반환한다")
    @Test
    void returnsNotFoundForMissingFailure() throws Exception {
        mockMvc.perform(get("/admin/pdf/failures/{id}", Long.MAX_VALUE)
                .with(withCookie(SESSION_COOKIE_NAME, sessionId)))
            .andExpect(status().isNotFound());
    }

    @DisplayName("관리자는 실패 조회 화면에 접근할 수 있다")
    @Test
    void opensViewAsAdmin() throws Exception {
        mockMvc.perform(get("/view/admin/pdf-export-failures").with(withCookie(SESSION_COOKIE_NAME, sessionId)))
            .andExpect(status().isOk());
    }

    @DisplayName("논리 삭제된 실패 기록은 목록에서 제외한다")
    @Test
    void excludesSoftDeletedFailuresFromList() throws Exception {
        // given
        SpacePdfExportFailure active = saveFailure("request-1", "2026-11-09T00:00:00");
        SpacePdfExportFailure deleted = saveFailure("request-2", "2026-11-09T01:00:00");
        deleted.delete();
        failureRepository.saveAndFlush(deleted);

        // when & then
        mockMvc.perform(get("/admin/pdf/failures").with(withCookie(SESSION_COOKIE_NAME, sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.failures", hasSize(1)))
            .andExpect(jsonPath("$.failures[0].id").value(active.getId()))
            .andExpect(jsonPath("$.totalCount").value(1));
    }

    @DisplayName("논리 삭제된 실패 기록의 상세 조회는 404를 반환한다")
    @Test
    void excludesSoftDeletedFailureFromDetail() throws Exception {
        // given
        SpacePdfExportFailure deleted = saveFailure("request-1", "2026-11-09T00:00:00");
        deleted.delete();
        failureRepository.saveAndFlush(deleted);

        // when & then
        mockMvc.perform(get("/admin/pdf/failures/{id}", deleted.getId())
                .with(withCookie(SESSION_COOKIE_NAME, sessionId)))
            .andExpect(status().isNotFound());
    }

    private SpacePdfExportFailure saveFailure(String requestId, String failedAt) {
        return failureRepository.saveAndFlush(SpacePdfExportFailureFixture.createFailure(
            requestId, LocalDateTime.parse(failedAt), PdfExportFailureStage.UNKNOWN,
            PdfExportFailureProcessingStatus.PENDING));
    }
}
