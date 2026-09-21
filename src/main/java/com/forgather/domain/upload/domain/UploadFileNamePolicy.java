package com.forgather.domain.upload.domain;

import java.util.regex.Pattern;

import com.forgather.global.exception.BaseException;

/**
 * 업로드 파일명 검증 정책. 현재 이미지 전용이며, 형식 검증(종류 무관)과 확장자 검증(종류별)을 분리해 둔다.
 * 동영상 등 업로드 종류가 늘어나면 이 클래스를 인터페이스로 승격하고
 * {@link #validateSupportedExtension}을 종류별 구현으로 분리한다.
 */
public class UploadFileNamePolicy {

    public static final String FILENAME_PATTERN = "^[^/\\\\]+\\.[a-z0-9]+$";
    private static final Pattern PATTERN = Pattern.compile(FILENAME_PATTERN);

    private UploadFileNamePolicy() {
    }

    public static void validate(String fileName) {
        validateNameFormat(fileName);
        validateSupportedExtension(fileName);
    }

    private static void validateNameFormat(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BaseException("업로드 파일명은 null이거나 비어있을 수 없습니다.");
        }
        if (!PATTERN.matcher(fileName).matches()) {
            throw new BaseException("올바르지 않은 업로드 파일명 형식입니다: " + fileName);
        }
    }

    private static void validateSupportedExtension(String fileName) {
        if (!ImageContentType.isSupportedFileName(fileName)) {
            throw new BaseException("지원하지 않는 이미지 확장자입니다: " + fileName);
        }
    }
}
