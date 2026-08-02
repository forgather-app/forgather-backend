package com.forgather.domain.space.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.space.dto.CreateSpaceRequest;
import com.forgather.domain.space.dto.FeaturedSpaceResponse;
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.SpaceHostFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.ForbiddenException;
import com.forgather.global.exception.NotFoundException;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class SpaceServiceTest extends TestOnContainer {

    private final SpaceService spaceService;
    private final SpaceRepository spaceRepository;
    private final HostRepository hostRepository;
    private final SpaceHostRepository spaceHostRepository;

    @Autowired
    public SpaceServiceTest(SpaceService spaceService, SpaceRepository spaceRepository, HostRepository hostRepository,
        SpaceHostRepository spaceHostRepository
    ) {
        this.spaceService = spaceService;
        this.spaceRepository = spaceRepository;
        this.hostRepository = hostRepository;
        this.spaceHostRepository = spaceHostRepository;
    }

    @DisplayName("스페이스 생성 시, 검증에 실패하면 스페이스가 DB에 저장되지 않는다.")
    @Test
    void createSpaceWithInvalidName() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String invalidSpaceName = " "; // 스페이스 이름이 공백인 경우
        CreateSpaceRequest request = new CreateSpaceRequest(
            invalidSpaceName,
            "description",
            true,
            "forgather_official",
            "forgather@forgather.me",
            null,
            null
        );
        MultipartFile file = new MockMultipartFile("temp.png", new byte[] {});

        // when & then
        assertAll(
            () -> assertThatException().isThrownBy(() -> spaceService.create(request, file, host)),
            () -> assertThat(spaceRepository.findAllByDeletedAtIsNull()).isEmpty()
        );
    }

    @DisplayName("호스트의 스페이스 목록 조회 시 논리 삭제된 스페이스는 조회하지 않는다")
    @Test
    void doesNotReturnSoftDeletedSpaceWhenQueryHostSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCode("abcdefghij"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("1234567890"));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space1, host));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space2, host));
        spaceService.delete(space1.getCode(), host);

        // when
        HostSpaceResponse result = spaceService.getSpacesInformation(host);

        // then
        assertThat(result.spaces().getFirst().spaceCode()).isEqualTo(space2.getCode());
    }

    @DisplayName("논리 삭제된 스페이스를 조회하면 예외를 던진다")
    @Test
    void shouldThrowExceptionWhenQuerySoftDeletedSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode("abcdefghij"));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space, host));
        spaceService.delete(space.getCode(), host);

        // when & then
        assertThatThrownBy(() -> spaceService.getSpaceInformation(space.getCode()))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않는 스페이스입니다.");
    }

    @DisplayName("호스트의 스페이스를 축하받는 스페이스로 지정한다.")
    @Test
    void updateFeaturedSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");

        // when
        FeaturedSpaceResponse response = spaceService.updateFeaturedSpace(host, space.getCode());

        // then
        assertAll(
            () -> assertThat(response.spaceCode()).isEqualTo(space.getCode()),
            () -> assertThat(space.isFeatured()).isTrue()
        );
    }

    @DisplayName("다른 스페이스를 지정하면 이전에 지정된 스페이스는 해제된다.")
    @Test
    void updateFeaturedSpaceReplacesPreviousOne() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        spaceService.updateFeaturedSpace(host, first.getCode());

        // when
        spaceService.updateFeaturedSpace(host, second.getCode());

        // then
        assertAll(
            () -> assertThat(first.isFeatured()).isFalse(),
            () -> assertThat(second.isFeatured()).isTrue()
        );
    }

    /**
     * "호스트당 최대 1개"는 DB 제약이 아니라 서비스가 보장한다. 지정 시 호스트의 모든 스페이스를 해제한 뒤
     * 대상 하나만 켜므로, 어떤 이유로 불변식이 깨져도 다음 지정 요청에서 스스로 정상화되어야 한다.
     */
    @DisplayName("여러 스페이스가 지정된 상태에서 지정을 요청하면 지정된 스페이스가 1개로 정리된다.")
    @Test
    void updateFeaturedSpaceHealsMultipleFeaturedSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        first.feature();
        second.feature();

        // when
        spaceService.updateFeaturedSpace(host, second.getCode());

        // then
        assertAll(
            () -> assertThat(first.isFeatured()).isFalse(),
            () -> assertThat(second.isFeatured()).isTrue()
        );
    }

    @DisplayName("다른 호스트의 스페이스를 축하받는 스페이스로 지정하면 예외를 던진다.")
    @Test
    void updateFeaturedSpaceWithOtherHostSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Host otherHost = hostRepository.save(HostFixture.createHost());
        Space otherSpace = saveSpaceOf(otherHost, "9999999999");

        // when & then
        assertThatThrownBy(() -> spaceService.updateFeaturedSpace(host, otherSpace.getCode()))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("권한이 존재하지 않습니다.");
    }

    @DisplayName("존재하지 않는 스페이스를 축하받는 스페이스로 지정하면 예외를 던진다.")
    @Test
    void updateFeaturedSpaceWithNotExistingSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String notExistingSpaceCode = "0000000000";

        // when & then
        assertThatThrownBy(() -> spaceService.updateFeaturedSpace(host, notExistingSpaceCode))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않는 스페이스입니다.");
    }

    @DisplayName("지정된 스페이스를 삭제하면 지정이 해제된다.")
    @Test
    void deleteFeaturedSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        spaceService.updateFeaturedSpace(host, space.getCode());

        // when
        spaceService.delete(space.getCode(), host);

        // then
        assertThat(space.isFeatured()).isFalse();
    }

    private Space saveSpaceOf(Host owner, String spaceCode) {
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode(spaceCode));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space, owner));
        return space;
    }
}
