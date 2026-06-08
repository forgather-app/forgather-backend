package com.forgather.domain.upload.domain;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.forgather.domain.model.Photo;

public interface ContentsStorage {

    String upload(String spaceCode, MultipartFile file) throws IOException;

    void deleteContents(List<String> contentPaths);

    // TODO: 추후 제거
    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    String issueSignedUrl(String path);

    /**
     * Content-Type과 Content-Length를 서명에 포함한 presigned URL을 발급합니다.
     * 클라이언트는 PUT 요청 시 동일한 Content-Type과 정확히 contentLength 바이트를 전송해야 합니다.
     * <p>
     *
     * @param path          S3 객체 경로
     * @param contentType   MIME 타입 (예: image/jpeg)
     * @param contentLength 파일 크기 (바이트)
     * @return presigned PUT URL
     */
    String issueSignedUrl(String path, String contentType, long contentLength);

    String getRootDirectory();

    void deletePhotos(List<? extends Photo> deletedPhotos);
}
