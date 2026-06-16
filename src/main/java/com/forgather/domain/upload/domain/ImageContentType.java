package com.forgather.domain.upload.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.forgather.global.exception.BaseException;

public enum ImageContentType {

    WEBP("image/webp", "webp"),
    ;

    private final String mimeType;
    private final Set<String> extensions;

    ImageContentType(String mimeType, String... extensions) {
        this.mimeType = mimeType;
        this.extensions = Set.of(extensions);
    }

    /**
     * 파일명의 확장자가 지원하는 이미지 형식인지 여부를 반환한다.
     * 확장자가 없거나 지원 목록에 없으면 false이며, 확장자 대소문자는 구분하지 않는다.
     */
    public static boolean isSupportedFileName(String fileName) {
        return findByFileName(fileName).isPresent();
    }

    public static String resolveByFileName(String fileName) {
        return findByFileName(fileName)
            .map(type -> type.mimeType)
            .orElseThrow(() -> new BaseException("지원하지 않는 이미지 확장자입니다: " + fileName));
    }

    private static Optional<ImageContentType> findByFileName(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension == null || extension.isBlank()) {
            return Optional.empty();
        }
        String normalized = extension.toLowerCase();
        return Arrays.stream(values())
            .filter(type -> type.extensions.contains(normalized))
            .findFirst();
    }
}
