package com.forgather.domain.upload.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;

class ImageContentTypeTest {

    @DisplayName("지원하는 확장자의 파일명은 대응하는 Content-Type을 반환한다")
    @ParameterizedTest
    @CsvSource({
        "photo.webp, image/webp"
    })
    void resolveByFileName(String fileName, String expectedMimeType) {
        assertThat(ImageContentType.resolveByFileName(fileName)).isEqualTo(expectedMimeType);
    }

    @DisplayName("확장자 대소문자를 구분하지 않고 Content-Type을 반환한다")
    @ParameterizedTest
    @CsvSource({
        "photo.WEBP, image/webp",
        "photo.WebP, image/webp"
    })
    void resolveByFileNameIgnoringCase(String fileName, String expectedMimeType) {
        assertThat(ImageContentType.resolveByFileName(fileName)).isEqualTo(expectedMimeType);
    }

    @DisplayName("점이 여러 개인 파일명은 마지막 확장자를 기준으로 Content-Type을 반환한다")
    @ParameterizedTest
    @CsvSource({
        "archive.tar.webp, image/webp",
        "my.photo.webp, image/webp"
    })
    void resolveByFileNameWithMultipleDots(String fileName, String expectedMimeType) {
        assertThat(ImageContentType.resolveByFileName(fileName)).isEqualTo(expectedMimeType);
    }

    @DisplayName("지원하지 않는 확장자의 파일명은 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"photo.png", "photo.jpg", "photo.jpeg", "photo.gif", "icon.svg", "evil.webp.exe"})
    void resolveByFileNameWithUnsupportedExtension(String fileName) {
        assertThatThrownBy(() -> ImageContentType.resolveByFileName(fileName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("지원하지 않는 이미지 확장자");
    }

    @DisplayName("확장자가 없는 파일명은 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"noext", "filename."})
    void resolveByFileNameWithoutExtension(String fileName) {
        assertThatThrownBy(() -> ImageContentType.resolveByFileName(fileName))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("지원하지 않는 이미지 확장자");
    }

    @DisplayName("지원하는 확장자의 파일명이면 true를 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"photo.webp", "photo.WEBP", "archive.tar.webp"})
    void isSupportedFileName(String fileName) {
        assertThat(ImageContentType.isSupportedFileName(fileName)).isTrue();
    }

    @DisplayName("지원하지 않거나 확장자가 없는 파일명이면 false를 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"photo.png", "photo.jpg", "photo.gif", "icon.svg", "evil.webp.exe", "noext"})
    void isSupportedFileNameReturnsFalse(String fileName) {
        assertThat(ImageContentType.isSupportedFileName(fileName)).isFalse();
    }

    @DisplayName("파일명이 null이거나 비어있으면 지원하지 않는 것으로 판단한다")
    @ParameterizedTest
    @NullAndEmptySource
    void isSupportedFileNameWithNullOrEmpty(String fileName) {
        assertThat(ImageContentType.isSupportedFileName(fileName)).isFalse();
    }
}
