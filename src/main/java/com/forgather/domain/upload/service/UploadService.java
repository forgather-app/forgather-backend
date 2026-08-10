package com.forgather.domain.upload.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.domain.SignedUrlIssuer;
import com.forgather.domain.upload.domain.UploadCategory;
import com.forgather.domain.upload.domain.UploadFileMetadata;
import com.forgather.domain.upload.dto.IssuePreSignedUrlRequest;
import com.forgather.domain.upload.dto.IssueSignedUrlRequest;
import com.forgather.domain.upload.dto.IssueSignedUrlResponse;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.ForbiddenException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final SpaceRepository spaceRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final SignedUrlIssuer signedUrlIssuer;

    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
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
        List<UploadFileMetadata> uploadFilesData = request.toUploadFilesData();
        spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);

        Map<String, String> signedUrls = signedUrlIssuer.issueForSpace(
            uploadFilesData,
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
            request.toUploadFilesData(),
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

    public IssueSignedUrlResponse issueSpacePhotoSignedUrls(Host host, IssuePreSignedUrlRequest request) {
        Map.Entry<String, String> signedUrl = signedUrlIssuer.issueForSpacePhoto(
            request.toUploadFilesData(),
            host.getId()
        );
        return new IssueSignedUrlResponse(signedUrl);
    }

    public IssueSignedUrlResponse issueExhibitionSignedUrls(IssuePreSignedUrlRequest request) {
        Map<String, String> signedUrls = signedUrlIssuer.issueForExhibition(request.toUploadFilesData());
        return new IssueSignedUrlResponse(signedUrls);
    }

    public IssueSignedUrlResponse issueHostProfileSignedUrls(Host host, IssuePreSignedUrlRequest request) {
        Map.Entry<String, String> signedUrl = signedUrlIssuer.issueForHostProfile(
            request.toUploadFilesData(),
            host.getId()
        );
        return new IssueSignedUrlResponse(signedUrl);
    }
}
