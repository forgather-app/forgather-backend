package com.forgather.domain.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.dto.HostProfileResponse;
import com.forgather.domain.host.dto.RegisterHostProfilePhotoRequest;
import com.forgather.domain.host.dto.UpdateHostProfileRequest;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.host.repository.jpa.HostProfilePhotoJpaRepository;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.fake.FakeContentStorage;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class HostServiceTest extends TestOnContainer {

    private static final String UPLOAD_FILE_NAME = "abc.webp";
    private static final Long CAPACITY = 1024L;

    @TestConfiguration
    static class FakeStorageConfiguration {

        @Bean
        @Primary
        ContentsStorage fakeContentsStorage() {
            return new FakeContentStorage();
        }
    }

    private final HostService hostService;
    private final HostRepository hostRepository;
    private final HostProfilePhotoRepository photoRepository;
    private final HostProfilePhotoJpaRepository photoJpaRepository;

    @Autowired
    public HostServiceTest(HostService hostService, HostRepository hostRepository,
        HostProfilePhotoRepository photoRepository, HostProfilePhotoJpaRepository photoJpaRepository
    ) {
        this.hostService = hostService;
        this.hostRepository = hostRepository;
        this.photoRepository = photoRepository;
        this.photoJpaRepository = photoJpaRepository;
    }

    private UpdateHostProfileRequest photoRequest(String uploadFileName) {
        return new UpdateHostProfileRequest(null, null, null,
            new RegisterHostProfilePhotoRequest(uploadFileName, CAPACITY), null);
    }

    @DisplayName("사진 메타데이터를 전달하면 프로필 사진이 저장된다.")
    @Test
    void registerPhoto() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        HostProfileResponse response = hostService.updateProfile(host, photoRequest(UPLOAD_FILE_NAME));

        // then
        String expectedPath = "photogather/v2/hosts/%d/profile/%s".formatted(host.getId(), UPLOAD_FILE_NAME);
        HostProfilePhoto saved = photoRepository.findByHostAndDeletedAtIsNull(host).orElseThrow();

        assertAll(
            () -> assertThat(response.photoPath()).isEqualTo(expectedPath),
            () -> assertThat(saved.getPath()).isEqualTo(expectedPath),
            () -> assertThat(saved.getCapacity()).isEqualTo(CAPACITY)
        );
    }

    @DisplayName("사진을 교체하면 기존 사진은 삭제 처리되고 새 사진이 저장된다.")
    @Test
    void replacePhoto() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        hostService.updateProfile(host, photoRequest("old.webp"));

        // when
        HostProfileResponse response = hostService.updateProfile(host, photoRequest("new.webp"));

        // then
        String expectedPath = "photogather/v2/hosts/%d/profile/new.webp".formatted(host.getId());
        List<HostProfilePhoto> allPhotos = photoJpaRepository.findAll();
        HostProfilePhoto active = photoRepository.findByHostAndDeletedAtIsNull(host).orElseThrow();

        assertAll(
            () -> assertThat(response.photoPath()).isEqualTo(expectedPath),
            () -> assertThat(active.getPath()).isEqualTo(expectedPath),
            () -> assertThat(allPhotos).hasSize(2),
            () -> assertThat(allPhotos)
                .filteredOn(photo -> photo.getDeletedAt() != null)
                .extracting(HostProfilePhoto::getPath)
                .containsExactly("photogather/v2/hosts/%d/profile/old.webp".formatted(host.getId()))
        );
    }

    @DisplayName("삭제 요청을 보내면 프로필 사진이 제거된다.")
    @Test
    void deletePhoto() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        hostService.updateProfile(host, photoRequest(UPLOAD_FILE_NAME));

        // when
        HostProfileResponse response = hostService.updateProfile(host,
            new UpdateHostProfileRequest(null, null, null, null, true));

        // then
        assertAll(
            () -> assertThat(response.photoPath()).isNull(),
            () -> assertThat(photoRepository.findByHostAndDeletedAtIsNull(host)).isEmpty()
        );
    }

    @DisplayName("사진 필드를 전달하지 않으면 기존 사진이 유지된다.")
    @Test
    void keepPhotoWhenNotRequested() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        hostService.updateProfile(host, photoRequest(UPLOAD_FILE_NAME));

        // when
        HostProfileResponse response = hostService.updateProfile(host,
            new UpdateHostProfileRequest("새닉네임", null, null, null, null));

        // then
        String expectedPath = "photogather/v2/hosts/%d/profile/%s".formatted(host.getId(), UPLOAD_FILE_NAME);

        assertAll(
            () -> assertThat(response.nickname()).isEqualTo("새닉네임"),
            () -> assertThat(response.photoPath()).isEqualTo(expectedPath),
            () -> assertThat(photoJpaRepository.findAll()).hasSize(1)
        );
    }
}
