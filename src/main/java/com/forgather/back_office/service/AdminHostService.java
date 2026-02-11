package com.forgather.back_office.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminHostResponse;
import com.forgather.back_office.dto.HostSpacesResponse;
import com.forgather.back_office.repository.AdminSpaceHostMapRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHostMap;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHostService {

    private final HostRepository hostRepository;
    private final AdminSpaceHostMapRepository adminSpaceHostMapRepository;

    public AdminHostResponse getAllHosts(Pageable pageable) {
        return AdminHostResponse.from(adminSpaceHostMapRepository.findAllHostsWithSpaceCount(pageable));
    }

    public HostSpacesResponse getHostSpaces(Long hostId) {
        Host host = hostRepository.getByIdOrThrow(hostId);
        List<Space> spaces = adminSpaceHostMapRepository
            .findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(host)
            .stream()
            .map(SpaceHostMap::getSpace)
            .toList();

        return HostSpacesResponse.of(host, spaces);
    }
}
