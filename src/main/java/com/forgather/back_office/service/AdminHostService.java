package com.forgather.back_office.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminHostResponse;
import com.forgather.back_office.dto.HostDetailResponse;
import com.forgather.back_office.dto.HostSpacesResponse;
import com.forgather.back_office.repository.AdminHostRepository;
import com.forgather.back_office.repository.AdminSpaceHostRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHostService {

    private final AdminHostRepository adminHostRepository;
    private final AdminSpaceHostRepository adminSpaceHostRepository;

    public AdminHostResponse getAllHosts(Pageable pageable) {
        return AdminHostResponse.from(adminHostRepository.findAllHostsWithSpaceCount(pageable));
    }

    public HostSpacesResponse getHostSpaces(Long hostId) {
        Host host = adminHostRepository.getByIdOrThrow(hostId);
        List<Space> spaces = adminSpaceHostRepository
            .findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(host)
            .stream()
            .map(SpaceHost::getSpace)
            .toList();

        return HostSpacesResponse.of(host, spaces);
    }

    public AdminHostResponse searchHostsByName(String name, Pageable pageable) {
        if (name == null) {
            return AdminHostResponse.from(adminHostRepository.findAllHostsWithSpaceCount(pageable));
        }
        String strippedName = name.strip();
        if (strippedName.isEmpty()) {
            return AdminHostResponse.from(adminHostRepository.findAllHostsWithSpaceCount(pageable));
        }
        String escapedName = escapeLikeWildcards(strippedName);
        Page<HostDetailResponse> hosts = adminHostRepository.findByNameContaining(escapedName, pageable);
        return AdminHostResponse.from(hosts);
    }

    private String escapeLikeWildcards(String input) {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
