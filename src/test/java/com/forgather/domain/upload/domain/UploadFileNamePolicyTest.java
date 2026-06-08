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
        "abc.webp",
        "0.webp",
        "550e8400-e29b-41d4-a716-446655440000.webp",
        "a.webp",
        "my_photo (1).webp",
        "사진.webp",
        "archive.tar.webp",
        "a..webp"
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

    @DisplayName("경로 조작, 확장자 없음, 대문자 확장자 등 형식에 어긋난 파일명은 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {
        "../../../etc/passwd",
        "a/b.webp",
        "a\\b.webp",
        "noext",
        "abc.WEBP"
    })
    void invalidFileName(String fileName) {
        assertThatThrownBy(() -> UploadFileNamePolicy.validate(fileName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("올바르지 않은 업로드 파일명 형식");
    }

    @DisplayName("형식은 올바르지만 지원하지 않는 확장자는 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"photo.png", "photo.jpg", "photo.jpeg", "x.gif", "document.pdf", "icon.svg"})
    void unsupportedExtension(String fileName) {
        assertThatThrownBy(() -> UploadFileNamePolicy.validate(fileName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("지원하지 않는 이미지 확장자");
    }

    @DisplayName("경로 구분자(/, \\)가 포함된 파일명은 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"a/b.webp", "a\\b.webp", "../../../config/evil.webp", "spaces/1234/x.webp"})
    void rejectsPathSeparator(String fileName) {
        assertThatThrownBy(() -> UploadFileNamePolicy.validate(fileName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("올바르지 않은 업로드 파일명 형식");
    }

    @DisplayName("validateAll은 목록 중 하나라도 형식에 어긋나면 예외를 던진다")
    @Test
    void validateAllRejectsAnyInvalid() {
        List<String> fileNames = List.of("valid.webp", "../../../etc/passwd");

        assertThatThrownBy(() -> UploadFileNamePolicy.validateAll(fileNames))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("올바르지 않은 업로드 파일명 형식");
    }

    @DisplayName("validateAll은 모든 파일명이 유효하면 예외를 던지지 않는다")
    @Test
    void validateAllPassesWhenAllValid() {
        List<String> fileNames = List.of("abc.webp", "def.webp", "ghi.webp");

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
