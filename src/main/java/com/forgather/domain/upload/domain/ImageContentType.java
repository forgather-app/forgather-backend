package com.forgather.domain.upload.domain;

import java.util.Arrays;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.forgather.global.exception.BaseException;

public enum ImageContentType {

    JPEG("image/jpeg", "jpg", "jpeg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    ;

    private final String mimeType;
    private final Set<String> extensions;

    ImageContentType(String mimeType, String... extensions) {
        this.mimeType = mimeType;
        this.extensions = Set.of(extensions);
    }

    public static String resolveByFileName(String fileName) {
        String extension = extractExtension(fileName);
        return Arrays.stream(values())
            .filter(type -> type.extensions.contains(extension))
            .findFirst()
            .map(type -> type.mimeType)
            .orElseThrow(() -> new BaseException("지원하지 않는 이미지 확장자입니다: " + fileName));
    }

    private static String extractExtension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension == null || extension.isBlank()) {
            throw new BaseException("파일 확장자를 확인할 수 없습니다: " + fileName);
        }
        return extension.toLowerCase();
    }
}
