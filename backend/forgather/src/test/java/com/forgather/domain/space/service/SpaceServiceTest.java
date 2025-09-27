package com.forgather.domain.space.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.forgather.domain.space.dto.CreateSpaceRequest;
import com.forgather.domain.space.dto.SpaceCapacityResponse;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpaceType;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.global.auth.model.Host;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "spring.aop.auto=false")
class SpaceServiceTest {

    private final SpaceService spaceService;
    private final SpaceRepository spaceRepository;
    private final HostRepository hostRepository;

    @Autowired
    public SpaceServiceTest(SpaceService spaceService, SpaceRepository spaceRepository, HostRepository hostRepository) {
        this.spaceService = spaceService;
        this.spaceRepository = spaceRepository;
        this.hostRepository = hostRepository;
    }

    @AfterEach
    void tearDown() {
        this.hostRepository.deleteAll();
        this.spaceRepository.deleteAll();
    }

    @DisplayName("스페이스 생성 시, 검증에 실패하면 스페이스가 DB에 저장되지 않는다.")
    @Test
    void createSpaceWithInvalidName() {
        // given
        String invalidSpaceName = " "; // 스페이스 이름이 공백인 경우
        CreateSpaceRequest request = new CreateSpaceRequest(
            invalidSpaceName,
            48,
            LocalDateTime.now().plusDays(1),
            SpaceType.PRIVATE
        );
        Host host = hostRepository.save(new Host("testHost", "testPictureUrl"));

        assertThatException().isThrownBy(
            () -> spaceService.create(request, host)
        );

        assertThat(spaceRepository.findAll()).isEmpty();
    }

    @DisplayName("공개 스페이스는 모두 용량을 조회할 수 있다.")
    @Test
    void getPublicSpaceCapacity() {
        // given
        String spaceCode = "1234567890";
        Host host = new Host("testHost", "testPictureUrl");
        hostRepository.save(host);
        Space space = new Space(host, spaceCode, "testName", 100, LocalDateTime.now(),
            1000L, SpaceType.PUBLIC);
        spaceRepository.save(space);

        // when
        SpaceCapacityResponse result = spaceService.getSpaceCapacity("1234567890", null);

        // then
        assertThat(result.maxCapacity()).isEqualTo(1000L);
        assertThat(result.usedCapacity()).isEqualTo(0L);
    }

    @DisplayName("공개 스페이스는 다른 호스트도 용량을 조회할 수 있다")
    @Test
    void getPublicSpaceCapacityWithOtherHost() {
        // given
        String spaceCode = "1234567890";
        Host host = new Host("testHost", "testPictureUrl");
        Host anotherHost = new Host("anotherHost", "anotherPictureUrl");
        hostRepository.save(host);
        hostRepository.save(anotherHost);
        Space space = new Space(host, spaceCode, "testName", 100, LocalDateTime.now(),
            1000L, SpaceType.PUBLIC);
        spaceRepository.save(space);

        // when
        SpaceCapacityResponse result = spaceService.getSpaceCapacity("1234567890", anotherHost);

        // then
        assertThat(result.maxCapacity()).isEqualTo(1000L);
        assertThat(result.usedCapacity()).isEqualTo(0L);
    }
}
