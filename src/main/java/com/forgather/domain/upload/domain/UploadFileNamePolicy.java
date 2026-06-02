package com.forgather.domain.upload.domain;

import java.util.List;
import java.util.regex.Pattern;

import com.forgather.global.exception.BaseException;

public class UploadFileNamePolicy {

    public static final String FILENAME_PATTERN = "^[^/\\\\]+\\.(jpg|jpeg|png|webp)$";

    private static final Pattern PATTERN = Pattern.compile(FILENAME_PATTERN);

    private UploadFileNamePolicy() {
    }

    public static void validateAll(List<String> fileNames) {
        if (fileNames == null) {
            throw new BaseException("업로드 파일명 목록은 null일 수 없습니다.");
        }
        fileNames.forEach(UploadFileNamePolicy::validate);
    }

    public static void validate(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BaseException("업로드 파일명은 null이거나 비어있을 수 없습니다.");
        }
        if (!PATTERN.matcher(fileName).matches()) {
            throw new BaseException("올바르지 않은 업로드 파일명 형식입니다: " + fileName);
        }
    }
}
