package com.forgather.domain.upload.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;

class UploadFileNamePolicyTest {

    @DisplayName("허용되는 파일명 형식은 예외를 던지지 않는다")
    @ParameterizedTest
    @ValueSource(strings = {
        "abc.jpg",
        "0.jpg",
        "550e8400-e29b-41d4-a716-446655440000.png",
        "a.jpeg",
        "a.webp",
        "my_photo (1).jpg",
        "사진.png",
        "archive.tar.png",
        "a..png"
    })
    void validFileName(String fileName) {
        assertThatCode(() -> UploadFileNamePolicy.validate(fileName))
            .doesNotThrowAnyException();
    }

    @DisplayName("파일명이 null이거나 비어있으면 예외를 던진다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankFileName(String fileName) {
        assertThatThrownBy(() -> UploadFileNamePolicy.validate(fileName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("null이거나 비어있을 수 없습니다");
    }

    @DisplayName("경로 조작·허용되지 않은 형식의 파일명은 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {
        "../../../etc/passwd",
        "a/b.png",
        "a\\b.png",
        "noext",
        "x.gif",
        "abc.PNG"
    })
    void invalidFileName(String fileName) {
        assertThatThrownBy(() -> UploadFileNamePolicy.validate(fileName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("올바르지 않은 업로드 파일명 형식");
    }

    @DisplayName("경로 구분자(/, \\)가 포함된 파일명은 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"a/b.png", "a\\b.png", "../../../config/evil.png", "spaces/1234/x.png"})
    void rejectsPathSeparator(String fileName) {
        assertThatThrownBy(() -> UploadFileNamePolicy.validate(fileName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("올바르지 않은 업로드 파일명 형식");
    }

    @DisplayName("validateAll은 목록 중 하나라도 형식에 어긋나면 예외를 던진다")
    @Test
    void validateAllRejectsAnyInvalid() {
        List<String> fileNames = List.of("valid.png", "../../../etc/passwd");

        assertThatThrownBy(() -> UploadFileNamePolicy.validateAll(fileNames))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("올바르지 않은 업로드 파일명 형식");
    }

    @DisplayName("validateAll은 모든 파일명이 유효하면 예외를 던지지 않는다")
    @Test
    void validateAllPassesWhenAllValid() {
        List<String> fileNames = List.of("abc.jpg", "def.jpeg", "ghi.webp");

        assertThatCode(() -> UploadFileNamePolicy.validateAll(fileNames))
            .doesNotThrowAnyException();
    }

    @DisplayName("validateAll은 목록이 null이면 예외를 던진다")
    @Test
    void validateAllRejectsNullList() {
        assertThatThrownBy(() -> UploadFileNamePolicy.validateAll(null))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("목록은 null일 수 없습니다");
    }
}
