package com.forgather.domain.upload.domain;

import static com.forgather.domain.upload.domain.FilePathGenerator.generateContentsFilePath;
import static com.forgather.domain.upload.domain.FilePathGenerator.generateExhibitionContentsFilePath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.forgather.global.exception.BaseException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class SignedUrlIssuer {

    private static final int MAX_COUNT_PER_ISSUE = 100;

    private final ContentsStorage contentsStorage;

    public Map<String, String> issueForSpace(
        List<UploadFile> uploadFiles,
        String spaceCode,
        UploadCategory category
    ) {
        validateNotEmpty(uploadFiles);
        validateCount(uploadFiles);

        if (spaceCode == null || spaceCode.isEmpty()) {
            throw new BaseException("스페이스 코드는 null이거나 비어있을 수 없습니다.");
        }
        if (category == null) {
            throw new BaseException("업로드 카테고리는 필수입니다.");
        }
        Map<String, String> signedUrls = new HashMap<>();
        for (UploadFile uploadFile : uploadFiles) {
            String filePath = generateContentsFilePath(
                contentsStorage.getRootDirectory(),
                spaceCode,
                category,
                uploadFile.getFileName()
            );
            signedUrls.put(uploadFile.getFileName(), issueUploadUrl(filePath, uploadFile));
        }
        return signedUrls;
    }

    /**
     * 전시 사진 presigned URL을 발급한다.
     */
    public Map<String, String> issueForExhibition(List<UploadFile> uploadFiles, Long hostId) {
        validateNotEmpty(uploadFiles);
        validateCount(uploadFiles);

        Map<String, String> signedUrls = new HashMap<>();
        for (UploadFile uploadFile : uploadFiles) {
            String filePath = generateExhibitionContentsFilePath(
                contentsStorage.getRootDirectory(),
                hostId,
                uploadFile.getFileName()
            );
            signedUrls.put(uploadFile.getFileName(), issueUploadUrl(filePath, uploadFile));
        }
        return signedUrls;
    }

    private String issueUploadUrl(String filePath, UploadFile uploadFile) {
        return contentsStorage.issueSignedUrl(
            filePath,
            uploadFile.getContentType(),
            uploadFile.getSize()
        );
    }

    public Map<String, String> issueSignedUrls(
        List<String> uploadFileNames,
        String spaceCode,
        UploadCategory category
    ) {
        validateNotEmpty(uploadFileNames);
        if (spaceCode == null || spaceCode.isEmpty()) {
            throw new BaseException("스페이스 코드는 null이거나 비어있을 수 없습니다.");
        }
        if (category == null) {
            throw new BaseException("업로드 카테고리는 필수입니다.");
        }
        validateCount(uploadFileNames);
        Map<String, String> signedUrls = new HashMap<>();
        for (String uploadFileName : uploadFileNames) {
            if (uploadFileName == null || uploadFileName.isBlank()) {
                throw new BaseException("업로드 파일명은 null이거나 비어있을 수 없습니다.");
            }
            String filePath = generateContentsFilePath(
                contentsStorage.getRootDirectory(),
                spaceCode,
                category,
                uploadFileName
            );
            signedUrls.put(uploadFileName, contentsStorage.issueSignedUrl(filePath));
        }
        return signedUrls;
    }

    private void validateNotEmpty(List<?> uploadFiles) {
        if (uploadFiles == null || uploadFiles.isEmpty()) {
            throw new BaseException("업로드 파일명 목록은 null이거나 비어있을 수 없습니다.");
        }
    }

    private void validateCount(List<?> uploadFiles) {
        if (uploadFiles.size() > MAX_COUNT_PER_ISSUE) {
            throw new BaseException("한번에 발급 가능한 업로드 url 개수는 %d개 입니다.".formatted(MAX_COUNT_PER_ISSUE));
        }
    }
}
