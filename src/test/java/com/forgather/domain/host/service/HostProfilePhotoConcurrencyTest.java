package com.forgather.domain.host.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.dto.RegisterHostProfilePhotoRequest;
import com.forgather.domain.host.dto.UpdateHostProfileRequest;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.jpa.HostProfilePhotoJpaRepository;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.fake.FakeContentStorage;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;

/**
 * 트랜잭션을 실제로 커밋해야 경합이 재현되므로 @Transactional을 붙이지 않고, cleanup.sql로 정리한다.
 */
@ActiveProfiles("test")
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HostProfilePhotoConcurrencyTest extends TestOnContainer {

    private static final int CONCURRENCY = 4;

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
    private final HostProfilePhotoJpaRepository photoJpaRepository;

    @Autowired
    HostProfilePhotoConcurrencyTest(HostService hostService, HostRepository hostRepository,
        HostProfilePhotoJpaRepository photoJpaRepository
    ) {
        this.hostService = hostService;
        this.hostRepository = hostRepository;
        this.photoJpaRepository = photoJpaRepository;
    }

    private UpdateHostProfileRequest photoRequest(String uploadFileName) {
        return new UpdateHostProfileRequest(null, null, null,
            new RegisterHostProfilePhotoRequest(uploadFileName, 1024L), null);
    }

    @DisplayName("동시에 프로필 사진을 교체해도 활성 사진은 한 장만 남는다.")
    @Test
    void keepSingleActivePhotoUnderConcurrentReplace() throws InterruptedException {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        hostService.updateProfile(host, photoRequest("old.webp"));

        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);

        // when
        for (int i = 0; i < CONCURRENCY; i++) {
            String uploadFileName = "new%d.webp".formatted(i);
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    hostService.updateProfile(host, photoRequest(uploadFileName));
                } catch (Exception ignored) {
                    // 경합으로 일부 요청이 실패하는 것은 허용한다. 활성 사진이 하나로 남는지만 본다.
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        // then
        List<HostProfilePhoto> activePhotos = photoJpaRepository.findAll().stream()
            .filter(photo -> photo.getDeletedAt() == null)
            .toList();
        assertThat(activePhotos).hasSize(1);
    }
}
