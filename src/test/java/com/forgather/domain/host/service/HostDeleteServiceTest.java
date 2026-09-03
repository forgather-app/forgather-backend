package com.forgather.domain.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.host.repository.jpa.HostProfilePhotoJpaRepository;
import com.forgather.fixture.HostFixture;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class HostDeleteServiceTest {

    private final HostDeleteService hostDeleteService;
    private final HostRepository hostRepository;
    private final HostProfilePhotoRepository photoRepository;
    private final HostProfilePhotoJpaRepository photoJpaRepository;

    @Autowired
    public HostDeleteServiceTest(HostDeleteService hostDeleteService, HostRepository hostRepository,
        HostProfilePhotoRepository photoRepository, HostProfilePhotoJpaRepository photoJpaRepository
    ) {
        this.hostDeleteService = hostDeleteService;
        this.hostRepository = hostRepository;
        this.photoRepository = photoRepository;
        this.photoJpaRepository = photoJpaRepository;
    }

    @DisplayName("탈퇴하면 프로필 사진도 삭제 처리된다.")
    @Test
    void deleteProfilePhoto() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        HostProfilePhoto photo = photoRepository.save(
            new HostProfilePhoto("photogather/v2/hosts/1/profile/a.webp", 1024L, host));

        // when
        hostDeleteService.delete(host.getId());

        // then
        assertAll(
            () -> assertThat(photoRepository.findByHostAndDeletedAtIsNull(host)).isEmpty(),
            () -> assertThat(photoJpaRepository.findById(photo.getId()).orElseThrow().getDeletedAt()).isNotNull()
        );
    }

    @DisplayName("프로필 사진이 없어도 탈퇴할 수 있다.")
    @Test
    void deleteWithoutProfilePhoto() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        hostDeleteService.delete(host.getId());

        // then
        assertThat(hostRepository.getByIdOrThrow(host.getId()).getDeletedAt()).isNotNull();
    }
}
