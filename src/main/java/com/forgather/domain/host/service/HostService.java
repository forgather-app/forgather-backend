package com.forgather.domain.host.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.dto.HostProfileResponse;
import com.forgather.domain.host.dto.PublicHostProfileResponse;
import com.forgather.domain.host.dto.RegisterHostProfilePhotoRequest;
import com.forgather.domain.host.dto.UpdateHostProfileRequest;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.domain.upload.domain.FilePathGenerator;

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

    @Transactional(readOnly = true)
    public PublicHostProfileResponse getPublicProfile(String hostCode) {
        Host host = hostRepository.getActiveByCodeOrThrow(hostCode);
        return PublicHostProfileResponse.of(host, findActivePhoto(host).orElse(null));
    }

    @Transactional
    public HostProfileResponse updateProfile(Host loginHost, UpdateHostProfileRequest request) {
        Host host = hostRepository.getByIdWithLockOrThrow(loginHost.getId());
        host.updateProfile(request.nickname(), request.introduction(), request.linkUrl());
        return HostProfileResponse.of(host, applyPhotoChange(host, request));
    }

    /**
     * 새 사진이 전달되면 기존 사진을 삭제하고 교체한다.
     * 삭제 요청만 있으면 제거한다.
     * 그 외에는 기존 사진을 유지한다.
     */
    private HostProfilePhoto applyPhotoChange(Host host, UpdateHostProfileRequest request) {
        Optional<HostProfilePhoto> existingPhoto = findActivePhoto(host);
        RegisterHostProfilePhotoRequest newPhoto = request.photo();

        if (newPhoto != null) {
            existingPhoto.ifPresent(HostProfilePhoto::delete);
            return hostProfilePhotoRepository.save(newPhoto.toEntity(buildPhotoPath(host, newPhoto), host));
        }
        if (request.isDeletingPhoto()) {
            existingPhoto.ifPresent(HostProfilePhoto::delete);
            return null;
        }
        return existingPhoto.orElse(null);
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
