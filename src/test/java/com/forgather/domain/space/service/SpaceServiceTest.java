package com.forgather.domain.space.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

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
import com.forgather.domain.space.dto.FeatureSpacesRequest;
import com.forgather.domain.space.dto.FeaturedSpacesResponse;
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.SpaceHostFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.BaseException;
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

    @DisplayName("여러 스페이스를 축하받는 스페이스로 한 번에 지정하고 지정된 코드 목록을 반환한다.")
    @Test
    void featureSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");

        // when
        FeaturedSpacesResponse response =
            spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(first.getCode(), second.getCode())));

        // then
        assertAll(
            () -> assertThat(response.spaceCodes()).containsExactlyInAnyOrder(first.getCode(), second.getCode()),
            () -> assertThat(first.isFeatured()).isTrue(),
            () -> assertThat(second.isFeatured()).isTrue()
        );
    }

    /**
     * 지정 API는 요청한 스페이스만 켠다. 해제는 별도 해제 API의 책임이므로, 요청에서 빠졌다는 이유로
     * 기존 지정이 풀려서는 안 된다. 이 API가 교체 의미를 갖지 않는다는 것을 고정하는 테스트다.
     */
    @DisplayName("요청에 포함되지 않은 스페이스의 지정 상태는 그대로 유지된다.")
    @Test
    void featureSpacesKeepsOmittedSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(first.getCode(), second.getCode())));

        // when
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(second.getCode())));

        // then
        assertAll(
            () -> assertThat(first.isFeatured()).isTrue(),
            () -> assertThat(second.isFeatured()).isTrue()
        );
    }

    /**
     * 지정 개수에 상한이 없다. 호스트가 가진 스페이스를 전부 지정할 수 있어야 한다.
     */
    @DisplayName("호스트가 가진 모든 스페이스를 축하받는 스페이스로 지정할 수 있다.")
    @Test
    void featureSpacesWithAllSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        Space third = saveSpaceOf(host, "3333333333");

        // when
        spaceService.featureSpaces(host,
            new FeatureSpacesRequest(List.of(first.getCode(), second.getCode(), third.getCode())));

        // then
        assertAll(
            () -> assertThat(first.isFeatured()).isTrue(),
            () -> assertThat(second.isFeatured()).isTrue(),
            () -> assertThat(third.isFeatured()).isTrue()
        );
    }

    @DisplayName("빈 목록을 요청하면 예외를 던진다.")
    @Test
    void featureSpacesWithEmptyList() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when & then
        assertThatThrownBy(() -> spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of())))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("스페이스 코드 목록이 존재하지 않습니다.");
    }

    @DisplayName("같은 목록으로 다시 지정해도 지정 상태가 유지된다.")
    @Test
    void featureSpacesIsIdempotent() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(space.getCode())));

        // when
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(space.getCode())));

        // then
        assertThat(space.isFeatured()).isTrue();
    }

    @DisplayName("같은 스페이스 코드가 중복으로 들어와도 집합으로 취급해 한 번만 지정된다.")
    @Test
    void featureSpacesWithDuplicatedCodes() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");

        // when
        FeaturedSpacesResponse response =
            spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(space.getCode(), space.getCode())));

        // then
        assertAll(
            () -> assertThat(response.spaceCodes()).containsExactly(space.getCode()),
            () -> assertThat(space.isFeatured()).isTrue()
        );
    }

    /**
     * 지정은 "호스트가 가진 스페이스"에만 적용되어야 한다. 다른 호스트의 지정 상태를 건드리면 안 된다.
     */
    @DisplayName("스페이스를 지정해도 다른 호스트의 지정 상태에는 영향을 주지 않는다.")
    @Test
    void featureSpacesDoesNotAffectOtherHostSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Host otherHost = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        Space otherSpace = saveSpaceOf(otherHost, "9999999999");
        spaceService.featureSpaces(otherHost, new FeatureSpacesRequest(List.of(otherSpace.getCode())));

        // when
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(space.getCode())));

        // then
        assertAll(
            () -> assertThat(space.isFeatured()).isTrue(),
            () -> assertThat(otherSpace.isFeatured()).isTrue()
        );
    }

    /**
     * 부분 반영을 허용하면 클라이언트가 최종 상태를 알 수 없다. 목록에 남의 스페이스가 섞이면
     * 요청 전체가 실패하고 기존 지정 상태가 그대로 남아야 한다.
     */
    @DisplayName("목록에 다른 호스트의 스페이스가 섞이면 예외를 던지고 기존 지정 상태가 유지된다.")
    @Test
    void featureSpacesWithOtherHostSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Host otherHost = hostRepository.save(HostFixture.createHost());
        Space featured = saveSpaceOf(host, "1111111111");
        Space notFeatured = saveSpaceOf(host, "2222222222");
        Space otherSpace = saveSpaceOf(otherHost, "9999999999");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(featured.getCode())));

        // when & then
        assertAll(
            () -> assertThatThrownBy(() -> spaceService.featureSpaces(
                host, new FeatureSpacesRequest(List.of(notFeatured.getCode(), otherSpace.getCode()))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("유효하지 않은 스페이스 코드입니다."),
            () -> assertThat(featured.isFeatured()).isTrue(),
            () -> assertThat(notFeatured.isFeatured()).isFalse()
        );
    }

    @DisplayName("목록에 존재하지 않는 스페이스가 섞이면 예외를 던지고 기존 지정 상태가 유지된다.")
    @Test
    void featureSpacesWithNotExistingSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space featured = saveSpaceOf(host, "1111111111");
        Space notFeatured = saveSpaceOf(host, "2222222222");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(featured.getCode())));
        String notExistingSpaceCode = "0000000000";

        // when & then
        assertAll(
            () -> assertThatThrownBy(() -> spaceService.featureSpaces(
                host, new FeatureSpacesRequest(List.of(notFeatured.getCode(), notExistingSpaceCode))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("유효하지 않은 스페이스 코드입니다."),
            () -> assertThat(featured.isFeatured()).isTrue(),
            () -> assertThat(notFeatured.isFeatured()).isFalse()
        );
    }

    @DisplayName("지정된 스페이스를 삭제하면 지정이 해제된다.")
    @Test
    void deleteFeaturedSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(space.getCode())));

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
