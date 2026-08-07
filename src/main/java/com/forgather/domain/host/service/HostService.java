package com.forgather.domain.host.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.dto.HostProfileResponse;
import com.forgather.domain.host.dto.PublicHostProfileResponse;
import com.forgather.domain.host.dto.UpdateHostProfileRequest;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.model.Host;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostService {

    private final HostRepository hostRepository;

    public HostProfileResponse getProfile(Host loginHost) {
        return HostProfileResponse.from(loginHost);
    }

    @Transactional(readOnly = true)
    public PublicHostProfileResponse getPublicProfile(String hostCode) {
        Host host = hostRepository.getActiveByCodeOrThrow(hostCode);
        return PublicHostProfileResponse.from(host);
    }

    @Transactional
    public HostProfileResponse updateProfile(Host loginHost, UpdateHostProfileRequest request) {
        Host host = hostRepository.getByIdOrThrow(loginHost.getId());
        host.updateProfile(request.nickname(), request.introduction(), request.linkUrl(), request.pictureUrl());
        return HostProfileResponse.from(host);
    }
}
