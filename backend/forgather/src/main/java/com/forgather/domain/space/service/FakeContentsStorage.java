package com.forgather.domain.space.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.forgather.global.exception.FileDownloadException;
import com.forgather.global.util.RandomCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class FakeContentsStorage implements ContentsStorage {

    private static final String CONTENTS_INNER_PATH = "contents";
    private static final String ROOT_DIRECTORY = "test";
    private static final long ISSUE_SIGNED_URL_DELAY_MS = 5L;
    private static final long DELETE_DELAY_MS = 120L;

    private final RandomCodeGenerator randomCodeGenerator;

    @Override
    public String upload(String spaceCode, MultipartFile file) throws IOException {
        return generateFilePath(spaceCode, file);
    }

    private String generateFilePath(String spaceCode, MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String uploadFileName = UUID.randomUUID().toString();
        return String.format("%s/%s/%s/%s.%s", ROOT_DIRECTORY, CONTENTS_INNER_PATH, spaceCode,
            uploadFileName, extension);
    }

    @Override
    public InputStream download(String photoPath) {
        log.atDebug()
            .addKeyValue("photoPath", photoPath)
            .log("Fake S3 다운로드 완료");

        return new ByteArrayInputStream(new byte[0]);
    }

    @Override
    public File downloadSelected(String tempPath, String spaceCode, List<String> photoPaths) {
        File localDownloadDirectory = new File(tempPath,
            "images-" + spaceCode + "-" + randomCodeGenerator.generate(10));
        if (!localDownloadDirectory.exists()) {
            boolean created = localDownloadDirectory.mkdirs();
            if (!created) {
                throw new FileDownloadException("다운로드 디렉토리 생성 실패: " + localDownloadDirectory.getAbsolutePath());
            }
        }

        log.atDebug()
            .addKeyValue("spaceCode", spaceCode)
            .addKeyValue("photoCount", photoPaths.size())
            .log("Fake S3 다운로드 선택 완료");

        return localDownloadDirectory;
    }

    @Deprecated(since = "2025-09-19", forRemoval = true)
    @Override
    public URL issueDownloadUrl(String photoPath) {
        try {
            return new URL("https://fake-s3.com/" + photoPath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL", e);
        }
    }

    @Override
    public void deleteContent(String contentPath) {
        simulateDeleteDelay();
    }

    @Override
    public void deleteContents(List<String> contentPaths) {
        simulateDeleteDelay();
    }

    private void simulateDeleteDelay() {
        try {
            Thread.sleep(DELETE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Delete operation interrupted", e);
        }
    }

    @Override
    public String issueSignedUrl(String path) {
        try {
            Thread.sleep(ISSUE_SIGNED_URL_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Issue signed URL operation interrupted", e);
        }
        return "https://fake-s3.com/signed-url/" + path;
    }

    @Override
    public String getRootDirectory() {
        return ROOT_DIRECTORY;
    }
}
