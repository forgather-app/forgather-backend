package com.forgather.global.auth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.exhibition.model.ExhibitionHost;
import com.forgather.domain.exhibition.repository.ExhibitionHostRepository;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.service.SpaceService;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.AppleHostRepository;
import com.forgather.global.auth.repository.KakaoHostRepository;
import com.forgather.global.auth.repository.SpaceHostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostDeleteService {

    private final HostRepository hostRepository;
    private final KakaoHostRepository kakaoHostRepository;
    private final AppleHostRepository appleHostRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final ExhibitionHostRepository exhibitionHostRepository;
    private final SpaceService spaceService;

    @Transactional
    public void delete(Long hostId) {
        Host host = hostRepository.getByIdOrThrow(hostId);
        deleteSocialAccounts(host);
        deleteSpaces(host);
        deleteExhibitionHosts(host);
        host.delete();
    }

    /**
     * 소셜 매핑은 hard delete한다.
     * user_id unique 제약을 해제해 같은 소셜 계정으로 즉시 신규 가입할 수 있게 한다.
     */
    private void deleteSocialAccounts(Host host) {
        kakaoHostRepository.findByHost(host).ifPresent(kakaoHostRepository::delete);
        appleHostRepository.findByHost(host).ifPresent(appleHostRepository::delete);
    }

    private void deleteSpaces(Host host) {
        List<SpaceHost> spaceHosts =
            spaceHostRepository.findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(host);
        for (SpaceHost spaceHost : spaceHosts) {
            spaceService.delete(spaceHost.getSpace().getCode(), host);
        }
    }

    private void deleteExhibitionHosts(Host host) {
        exhibitionHostRepository.findAllByHostAndDeletedAtIsNull(host)
            .forEach(ExhibitionHost::delete);
    }
}
