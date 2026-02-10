package com.forgather.back_office.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminHostResponse;
import com.forgather.back_office.dto.HostDetailResponse;
import com.forgather.back_office.dto.HostSpacesResponse;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHostMap;
import com.forgather.global.auth.repository.SpaceHostMapRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHostService {

    private final HostRepository hostRepository;
    private final SpaceHostMapRepository spaceHostMapRepository;

    public AdminHostResponse getAllHosts(Pageable pageable) {
        Page<Host> hosts = hostRepository.findAll(pageable);
        return AdminHostResponse.from(hosts.map(host -> {
            List<Long> spaceIds = spaceHostMapRepository.findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(
                    host)
                .stream()
                .map(spaceHostMap -> spaceHostMap.getSpace().getId())
                .toList();
            return HostDetailResponse.of(host, spaceIds);
        }));
    }

    public HostSpacesResponse getHostSpaces(Long hostId) {
        Host host = hostRepository.getByIdOrThrow(hostId);
        List<Space> spaces = spaceHostMapRepository
            .findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(host)
            .stream()
            .map(SpaceHostMap::getSpace)
            .toList();

        return HostSpacesResponse.of(host, spaces);
    }
}
