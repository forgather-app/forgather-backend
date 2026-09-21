package com.forgather.domain.upload.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.forgather.global.exception.BaseException;

class UploadFileMetadataTest {

    @DisplayName("파일 확장자로부터 Content-Type을 유도한다")
    @ParameterizedTest
    @CsvSource({
        "photo.webp, image/webp"
    })
    void contentType(String fileName, String expectedContentType) {
        UploadFileMetadata uploadFile = new UploadFileMetadata(fileName, 1024L);

        assertThat(uploadFile.getContentType()).isEqualTo(expectedContentType);
    }

    @DisplayName("파일 크기가 최대 크기(20MB) 이하이면 생성된다")
    @ParameterizedTest
    @ValueSource(longs = {1L, 1024L, UploadFileMetadata.MAX_FILE_SIZE_BYTES})
    void createWhenSizeWithinLimit(long size) {
        assertThatCode(() -> new UploadFileMetadata("photo.webp", size))
            .doesNotThrowAnyException();
    }

    @DisplayName("파일 크기가 최대 크기(20MB)를 초과하면 예외를 던진다")
    @Test
    void throwExceptionWhenSizeExceedsLimit() {
        long oversize = UploadFileMetadata.MAX_FILE_SIZE_BYTES + 1;

        assertThatThrownBy(() -> new UploadFileMetadata("photo.webp", oversize))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("최대");
    }

    @DisplayName("파일 크기가 0 이하이면 예외를 던진다")
    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void throwExceptionWhenSizeNotPositive(long size) {
        assertThatThrownBy(() -> new UploadFileMetadata("photo.webp", size))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("0보다 커야");
    }

    @DisplayName("파일명 형식이 올바르지 않으면 예외를 던진다")
    @ParameterizedTest
    @ValueSource(strings = {"../../../etc/passwd", "a/b.webp", "a\\b.webp", "noext", "x.gif"})
    void throwExceptionWhenInvalidFileName(String invalidFileName) {
        assertThatThrownBy(() -> new UploadFileMetadata(invalidFileName, 1024L))
            .isInstanceOf(BaseException.class);
    }
}
