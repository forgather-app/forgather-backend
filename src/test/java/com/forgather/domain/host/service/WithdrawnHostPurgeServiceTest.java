package com.forgather.domain.host.service;

import static com.forgather.fixture.HostFixture.createHostWithName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.model.Photo;
import com.forgather.fake.FakeContentStorage;
import com.forgather.global.auth.model.Host;

@Import({WithdrawnHostPurgeService.class, HostPhotoFinder.class, HostAnonymizeService.class,
    WithdrawnHostPurgeServiceTest.StubContentsStorageConfig.class})
@DataJpaTest
class WithdrawnHostPurgeServiceTest {

    @Autowired
    private WithdrawnHostPurgeService withdrawnHostPurgeService;

    @Autowired
    private StubContentsStorage contentsStorage;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void resetStub() {
        contentsStorage.reset();
    }

    private Host saveHostDeletedDaysAgo(String name, int days) {
        Host host = createHostWithName(name);
        ReflectionTestUtils.setField(host, "deletedAt", LocalDateTime.now().minusDays(days));
        return entityManager.persist(host);
    }

    private HostProfilePhoto saveDeletedProfilePhoto(Host host, String path) {
        HostProfilePhoto photo = new HostProfilePhoto(path, 1024L, host);
        photo.delete();
        return entityManager.persist(photo);
    }

    @DisplayName("탈퇴 후 30일이 지난 회원의 사진을 저장소에서 삭제하고 익명화한다")
    @Test
    void purgeExpiredHost() {
        // given
        Host host = saveHostDeletedDaysAgo("만료회원", 31);
        saveDeletedProfilePhoto(host, "profile-path");

        // when
        int purgedCount = withdrawnHostPurgeService.purgeExpiredHosts();

        // then
        assertAll(
            () -> assertThat(purgedCount).isEqualTo(1),
            () -> assertThat(contentsStorage.getDeletedPaths()).containsExactly("profile-path"),
            () -> assertThat(host.getName()).isEqualTo("탈퇴한 회원"),
            () -> assertThat(host.getAnonymizedAt()).isNotNull()
        );
    }

    @DisplayName("탈퇴 후 30일이 지나지 않은 회원은 처리하지 않는다")
    @Test
    void skipNotExpiredHost() {
        // given
        Host host = saveHostDeletedDaysAgo("보존회원", 29);
        saveDeletedProfilePhoto(host, "retained-path");

        // when
        int purgedCount = withdrawnHostPurgeService.purgeExpiredHosts();

        // then
        assertAll(
            () -> assertThat(purgedCount).isZero(),
            () -> assertThat(contentsStorage.getDeletedPaths()).isEmpty(),
            () -> assertThat(host.getAnonymizedAt()).isNull()
        );
    }

    @DisplayName("저장소 사진 삭제에 실패하면 익명화하지 않아 다음 스케줄에서 재시도된다")
    @Test
    void doNotAnonymizeWhenStorageDeletionFails() {
        // given
        Host host = saveHostDeletedDaysAgo("만료회원", 31);
        saveDeletedProfilePhoto(host, "fail-path");
        contentsStorage.failOn("fail-path");

        // when
        int purgedCount = withdrawnHostPurgeService.purgeExpiredHosts();

        // then
        assertAll(
            () -> assertThat(purgedCount).isZero(),
            () -> assertThat(host.getName()).isEqualTo("만료회원"),
            () -> assertThat(host.getAnonymizedAt()).isNull()
        );
    }

    @DisplayName("한 회원의 처리 실패가 다른 회원의 처리를 막지 않는다")
    @Test
    void isolateFailurePerHost() {
        // given
        Host failedHost = saveHostDeletedDaysAgo("실패회원", 31);
        saveDeletedProfilePhoto(failedHost, "fail-path");
        contentsStorage.failOn("fail-path");

        Host purgedHost = saveHostDeletedDaysAgo("성공회원", 31);
        saveDeletedProfilePhoto(purgedHost, "success-path");

        // when
        int purgedCount = withdrawnHostPurgeService.purgeExpiredHosts();

        // then
        assertAll(
            () -> assertThat(purgedCount).isEqualTo(1),
            () -> assertThat(contentsStorage.getDeletedPaths()).containsExactly("success-path"),
            () -> assertThat(failedHost.getAnonymizedAt()).isNull(),
            () -> assertThat(purgedHost.getAnonymizedAt()).isNotNull()
        );
    }

    @TestConfiguration
    static class StubContentsStorageConfig {

        @Bean
        StubContentsStorage contentsStorage() {
            return new StubContentsStorage();
        }
    }

    /**
     * 삭제된 사진 경로를 기록하고, 지정한 경로가 포함되면 실패를 흉내 낸다.
     */
    static class StubContentsStorage extends FakeContentStorage {

        private final List<String> deletedPaths = new ArrayList<>();
        private final Set<String> failPaths = new HashSet<>();

        @Override
        public void deletePhotos(List<? extends Photo> deletedPhotos) {
            List<String> paths = deletedPhotos.stream()
                .map(Photo::getPath)
                .toList();
            if (paths.stream().anyMatch(failPaths::contains)) {
                throw new IllegalStateException("저장소 사진 삭제 실패");
            }
            deletedPaths.addAll(paths);
        }

        public void failOn(String path) {
            failPaths.add(path);
        }

        public void reset() {
            deletedPaths.clear();
            failPaths.clear();
        }

        public List<String> getDeletedPaths() {
            return deletedPaths;
        }
    }
}
