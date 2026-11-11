package com.forgather.domain.export.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;

class SpacePdfExportTest {

    private static final String JOB_ID = "de5011e4-d6bf-11f1-8249-0a1abd5e6063";
    private static final String SNAPSHOT = """
        {"spaceName":"전시","nickname":"주최자","guestbookInfos":[]}
        """;
    private static final String EMAIL = "reader@example.com";

    @DisplayName("유효한 작업 ID, 스냅샷, 이메일로 PDF 추출 작업을 생성한다")
    @Test
    void createsExport() {
        // given & when
        SpacePdfExport export = new SpacePdfExport(JOB_ID, SNAPSHOT, EMAIL);

        // then
        assertAll(
            () -> assertThat(export.getJobId()).isEqualTo(JOB_ID),
            () -> assertThat(export.getSnapshot()).isEqualTo(SNAPSHOT),
            () -> assertThat(export.getEmail()).isEqualTo(EMAIL)
        );
    }

    @DisplayName("필수값이 null이면 PDF 추출 작업을 생성할 수 없다")
    @Test
    void rejectsNullFields() {
        // when & then
        assertAll(
            () -> assertThatThrownBy(() -> new SpacePdfExport(null, SNAPSHOT, EMAIL))
                .isExactlyInstanceOf(BaseNullPointerException.class),
            () -> assertThatThrownBy(() -> new SpacePdfExport(JOB_ID, null, EMAIL))
                .isExactlyInstanceOf(BaseNullPointerException.class),
            () -> assertThatThrownBy(() -> new SpacePdfExport(JOB_ID, SNAPSHOT, null))
                .isExactlyInstanceOf(BaseNullPointerException.class)
        );
    }

    @DisplayName("필수값이 빈 문자열이거나 공백뿐이면 PDF 추출 작업을 생성할 수 없다")
    @ParameterizedTest
    @ValueSource(strings = {"", " \t"})
    void rejectsBlankFields(String blank) {
        // when & then
        assertAll(
            () -> assertThatThrownBy(() -> new SpacePdfExport(blank, SNAPSHOT, EMAIL))
                .isExactlyInstanceOf(BaseException.class),
            () -> assertThatThrownBy(() -> new SpacePdfExport(JOB_ID, blank, EMAIL))
                .isExactlyInstanceOf(BaseException.class),
            () -> assertThatThrownBy(() -> new SpacePdfExport(JOB_ID, SNAPSHOT, blank))
                .isExactlyInstanceOf(BaseException.class)
        );
    }
}
