package com.forgather.domain.upload.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.domain.upload.domain.SignedUrlIssuer;
import com.forgather.domain.upload.domain.UploadCategory;
import com.forgather.domain.upload.domain.UploadFile;
import com.forgather.domain.upload.dto.IssuePreSignedUrlRequest;
import com.forgather.domain.upload.dto.IssueSignedUrlRequest;
import com.forgather.domain.upload.dto.IssueSignedUrlResponse;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.FileUploadException;
import com.forgather.global.exception.ForbiddenException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadService {

    private final SpaceRepository spaceRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final ContentsStorage contentsStorage;
    private final SignedUrlIssuer signedUrlIssuer;

    public String upload(String spaceCode, MultipartFile file) {
        try {
            long startMillis = System.currentTimeMillis();
            log.info("파일 업로드 시작 spaceCode: {}, originalName: {}, getSize: {}",
                spaceCode, file.getOriginalFilename(), file.getSize());

            String path = contentsStorage.upload(spaceCode, file);

            long durationMillis = System.currentTimeMillis() - startMillis;
            log.info("파일 업로드 완료 spaceCode: {}, originalName: {}, getSize: {}, path: {}, duration: {}",
                spaceCode, file.getOriginalFilename(), file.getSize(), path, durationMillis + "ms");

            return path;
        } catch (IOException e) {
            throw new FileUploadException("파일 업로드 실패 spaceCode: %s, originalName: %s, getSize: %d".formatted(
                spaceCode, file.getOriginalFilename(), file.getSize()
            ), e);
        }
    }

    // TODO: 추후 제거
    @Transactional(readOnly = true)
    public IssueSignedUrlResponse issueSignedUrls(String spaceCode, IssueSignedUrlRequest request) {
        spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        Map<String, String> signedUrls = signedUrlIssuer.issueSignedUrls(
            request.uploadFileNames(),
            spaceCode,
            request.category()
        );
        return new IssueSignedUrlResponse(signedUrls);
    }

    @Transactional(readOnly = true)
    public IssueSignedUrlResponse issueGuestbookSignedUrls(String spaceCode, IssuePreSignedUrlRequest request) {
        List<UploadFile> uploadFiles = request.toUploadFiles();
        spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);

        Map<String, String> signedUrls = signedUrlIssuer.issueForSpace(
            uploadFiles,
            spaceCode,
            UploadCategory.GUESTBOOK
        );
        return new IssueSignedUrlResponse(signedUrls);
    }

    @Transactional(readOnly = true)
    public IssueSignedUrlResponse issueProductSignedUrls(
        String spaceCode,
        Host host,
        IssuePreSignedUrlRequest request
    ) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(space, host);

        Map<String, String> signedUrls = signedUrlIssuer.issueForSpace(
            request.toUploadFiles(),
            spaceCode,
            UploadCategory.PRODUCT
        );
        return new IssueSignedUrlResponse(signedUrls);
    }

    private void validateSpaceHost(Space space, Host host) {
        if (spaceHostRepository.findBySpaceAndHostAndDeletedAtIsNull(space, host).isPresent()) {
            return;
        }
        throw new ForbiddenException("권한이 존재하지 않습니다.");
    }

    public IssueSignedUrlResponse issueExhibitionSignedUrls(Host host, IssuePreSignedUrlRequest request) {
        Map<String, String> signedUrls = signedUrlIssuer.issueForExhibition(
            request.toUploadFiles(),
            host.getId()
        );
        return new IssueSignedUrlResponse(signedUrls);
    }
}
