package com.forgather.domain.host.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.dto.HostProfileResponse;
import com.forgather.domain.host.dto.RegisterHostProfilePhotoRequest;
import com.forgather.domain.host.dto.UpdateHostProfileRequest;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.domain.upload.domain.FilePathGenerator;
import com.forgather.global.auth.model.Host;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostService {

    private final HostRepository hostRepository;
    private final HostProfilePhotoRepository hostProfilePhotoRepository;
    private final ContentsStorage contentsStorage;

    @Transactional(readOnly = true)
    public HostProfileResponse getProfile(Host loginHost) {
        return HostProfileResponse.of(loginHost, findActivePhoto(loginHost).orElse(null));
    }

    @Transactional
    public HostProfileResponse updateProfile(Host loginHost, UpdateHostProfileRequest request) {
        Host host = hostRepository.getByIdOrThrow(loginHost.getId());
        host.updateProfile(request.nickname(), request.introduction(), request.linkUrl());
        return HostProfileResponse.of(host, applyPhotoChange(host, request));
    }

    /**
     * 사진 필드가 모두 비어 있으면 기존 사진을 유지한다.
     * 새 사진이 전달되면 기존 사진을 삭제하고 교체하며, 삭제 요청만 있으면 제거한다.
     */
    private HostProfilePhoto applyPhotoChange(Host host, UpdateHostProfileRequest request) {
        Optional<HostProfilePhoto> existingPhoto = findActivePhoto(host);
        if (request.photo() == null && !request.isDeletingPhoto()) {
            return existingPhoto.orElse(null);
        }
        existingPhoto.ifPresent(HostProfilePhoto::delete);
        if (request.photo() == null) {
            return null;
        }
        return hostProfilePhotoRepository.save(request.photo().toEntity(buildPhotoPath(host, request.photo()), host));
    }

    private Optional<HostProfilePhoto> findActivePhoto(Host host) {
        return hostProfilePhotoRepository.findByHostAndDeletedAtIsNull(host);
    }

    private String buildPhotoPath(Host host, RegisterHostProfilePhotoRequest photo) {
        return FilePathGenerator.generateHostProfileFilePath(
            contentsStorage.getRootDirectory(),
            host.getId(),
            photo.uploadFileName()
        );
    }
}
