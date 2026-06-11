package com.forgather.domain.upload.domain;

import com.forgather.global.exception.BaseException;

import lombok.Getter;

/**
 * presigned URL을 발급할 업로드 파일 한 건의 도메인 값 객체.
 * 파일명 형식과 크기 상한(최대 {@link #MAX_FILE_SIZE_BYTES})을 스스로 보장하고,
 * 파일명 확장자로부터 고정할 Content-Type을 제공한다.
 */
@Getter
public class UploadFile {

    public static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024; // 20MB

    private final String fileName;
    private final long size;

    public UploadFile(String fileName, long size) {
        UploadFileNamePolicy.validate(fileName);
        validateSize(fileName, size);
        this.fileName = fileName;
        this.size = size;
    }

    private static void validateSize(String fileName, long size) {
        if (size <= 0) {
            throw new BaseException("업로드 파일 크기는 0보다 커야 합니다: " + fileName);
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw new BaseException(
                "업로드 파일 크기는 최대 %dMB 입니다: %s".formatted(MAX_FILE_SIZE_BYTES / 1024 / 1024, fileName)
            );
        }
    }

    public String getContentType() {
        return ImageContentType.resolveByFileName(fileName);
    }
}
