package com.forgather.domain.space.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.space.dto.CreateSpaceRequest;
import com.forgather.domain.space.dto.CreateSpaceResponse;
import com.forgather.domain.space.dto.FeatureSpacesRequest;
import com.forgather.domain.space.dto.FeaturedSpacesResponse;
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.dto.SpaceResponse;
import com.forgather.domain.space.dto.UpdateSpaceRequest;
import com.forgather.domain.space.dto.UnfeatureSpacesRequest;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpacePhotoRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.SpaceHostFixture;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.NotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class SpaceServiceTest extends TestOnContainer {

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final SpaceService spaceService;
    private final SpaceRepository spaceRepository;
    private final SpacePhotoRepository spacePhotoRepository;
    private final HostRepository hostRepository;
    private final SpaceHostRepository spaceHostRepository;

    @Autowired
    public SpaceServiceTest(SpaceService spaceService, SpaceRepository spaceRepository,
        SpacePhotoRepository spacePhotoRepository, HostRepository hostRepository,
        SpaceHostRepository spaceHostRepository, EntityManager entityManager, ObjectMapper objectMapper
    ) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.spaceService = spaceService;
        this.spaceRepository = spaceRepository;
        this.spacePhotoRepository = spacePhotoRepository;
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
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        // when & then
        assertAll(
            () -> assertThatException().isThrownBy(() -> spaceService.create(request, host)),
            () -> assertThat(spaceRepository.findAllByDeletedAtIsNull()).isEmpty()
        );
    }

    @DisplayName("스페이스를 생성해도 스페이스 사진은 저장하지 않는다.")
    @Test
    void createSpaceDoesNotSavePhoto() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateSpaceRequest request = new CreateSpaceRequest(
            "test-space",
            "description",
            true,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        // when
        CreateSpaceResponse response = spaceService.create(request, host);

        // then
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(response.spaceCode());
        assertThat(spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)).isEmpty();
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
        assertThatThrownBy(() -> spaceService.getSpaceInformation(space.getCode(), null))
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
            () -> assertThat(response.featuredSpaceCodes()).containsExactlyInAnyOrder(first.getCode(), second.getCode()),
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
            () -> assertThat(response.featuredSpaceCodes()).containsExactly(space.getCode()),
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

    @DisplayName("일부를 해제하면 해제 대상만 미지정 상태가 된다.")
    @Test
    void unfeatureSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(first.getCode(), second.getCode())));

        // when
        spaceService.unfeatureSpaces(host, new UnfeatureSpacesRequest(List.of(first.getCode())));

        // then
        assertAll(
            () -> assertThat(first.isFeatured()).isFalse(),
            () -> assertThat(second.isFeatured()).isTrue()
        );
    }

    @DisplayName("해제 요청에 포함되지 않은 스페이스의 지정 상태는 그대로 유지된다.")
    @Test
    void unfeatureSpacesKeepsOmittedSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space first = saveSpaceOf(host, "1111111111");
        Space second = saveSpaceOf(host, "2222222222");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(first.getCode(), second.getCode())));

        // when
        spaceService.unfeatureSpaces(host, new UnfeatureSpacesRequest(List.of(first.getCode())));

        // then
        assertThat(second.isFeatured()).isTrue();
    }

    /**
     * DELETE는 멱등해야 한다. 이미 미지정인 스페이스를 해제해도 실패하지 않아야
     * 클라이언트가 현재 지정 상태를 몰라도 안전하게 재시도할 수 있다.
     */
    @DisplayName("지정되지 않은 스페이스를 해제해도 예외 없이 미지정 상태가 유지된다.")
    @Test
    void unfeatureSpacesIsIdempotent() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");

        // when
        spaceService.unfeatureSpaces(host, new UnfeatureSpacesRequest(List.of(space.getCode())));

        // then
        assertThat(space.isFeatured()).isFalse();
    }

    @DisplayName("빈 목록으로 해제를 요청하면 예외를 던진다.")
    @Test
    void unfeatureSpacesWithEmptyList() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when & then
        assertThatThrownBy(() -> spaceService.unfeatureSpaces(host, new UnfeatureSpacesRequest(List.of())))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("스페이스 코드 목록이 존재하지 않습니다.");
    }

    /**
     * 해제는 "호스트가 가진 스페이스"에만 적용되어야 한다. 남의 스페이스를 목록에 넣어도 꺼지지 않고,
     * 부분 반영 없이 요청 전체가 실패해 내 기존 지정 상태도 그대로 남아야 한다.
     */
    @DisplayName("해제 목록에 다른 호스트의 스페이스가 섞이면 예외를 던지고 양쪽의 지정 상태가 모두 유지된다.")
    @Test
    void unfeatureSpacesWithOtherHostSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Host otherHost = hostRepository.save(HostFixture.createHost());
        Space featured = saveSpaceOf(host, "1111111111");
        Space otherSpace = saveSpaceOf(otherHost, "9999999999");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(featured.getCode())));
        spaceService.featureSpaces(otherHost, new FeatureSpacesRequest(List.of(otherSpace.getCode())));

        // when & then
        assertAll(
            () -> assertThatThrownBy(() -> spaceService.unfeatureSpaces(
                host, new UnfeatureSpacesRequest(List.of(featured.getCode(), otherSpace.getCode())))
            )
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("유효하지 않은 스페이스 코드입니다."),

            () -> assertThat(featured.isFeatured()).isTrue(),
            () -> assertThat(otherSpace.isFeatured()).isTrue()
        );
    }

    @DisplayName("해제 목록에 존재하지 않는 스페이스가 섞이면 예외를 던지고 기존 지정 상태가 유지된다.")
    @Test
    void unfeatureSpacesWithNotExistingSpace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space featured = saveSpaceOf(host, "1111111111");
        spaceService.featureSpaces(host, new FeatureSpacesRequest(List.of(featured.getCode())));
        String notExistingSpaceCode = "0000000000";

        // when & then
        assertAll(
            () -> assertThatThrownBy(() -> spaceService.unfeatureSpaces(
                host, new UnfeatureSpacesRequest(List.of(featured.getCode(), notExistingSpaceCode))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("유효하지 않은 스페이스 코드입니다."),
            () -> assertThat(featured.isFeatured()).isTrue()
        );
    }

    @DisplayName("장소 전체 정보 또는 장소명만 입력하면 생성한 스페이스에 저장된다")
    @ParameterizedTest
    @CsvSource({"전시관, 00123, https://place.map.kakao.com/00123",
        "전시관, 00123, http://place.map.kakao.com/00123", "전시관,'',''"})
    void createSpacePersistsPlace(String placeName, String placeId, String placeUrl) {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateSpaceRequest request = new CreateSpaceRequest(
            "전시", null, true, null, null, null, null, placeName, placeId, placeUrl);

        // when
        CreateSpaceResponse response = spaceService.create(request, host);

        // then
        assertPersistedPlace(response.spaceCode(), placeName, placeId, placeUrl);
    }

    @DisplayName("장소 생성 요청의 null과 공백은 DB와 장소 객체에 빈 문자열로 저장된다")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void createSpaceWithoutPlace(String value) {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateSpaceRequest request = new CreateSpaceRequest(
            "전시", null, true, null, null, null, null, value, value, value);

        // when
        CreateSpaceResponse response = spaceService.create(request, host);

        // then
        assertPersistedPlace(response.spaceCode(), "", "", "");
        assertResponsePlace(spaceService.getSpaceInformation(response.spaceCode(), null), "", "", "");
    }

    @DisplayName("장소명이 없는 스페이스도 상세 응답의 장소 필드 세 개를 빈 문자열로 제공한다")
    @Test
    void getSpaceWithoutPlaceReturnsEmptyStrings() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        assertPersistedPlace(space.getCode(), "", "", "");

        // when
        SpaceResponse response = spaceService.getSpaceInformation(space.getCode(), null);

        // then
        assertResponsePlace(response, "", "", "");
    }

    @DisplayName("장소가 없는 스페이스에 전체 장소 또는 장소명만 수정해 저장한다")
    @ParameterizedTest
    @CsvSource({"전시관, 00123, https://place.map.kakao.com/00123",
        "전시관, 00123, http://place.map.kakao.com/00123", "전시관,'',''"})
    void updateSpaceAddsPlace(String placeName, String placeId, String placeUrl) {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        UpdateSpaceRequest request = new UpdateSpaceRequest(
            null, null, null, null, null, null, null, null, placeName, placeId, placeUrl);

        // when
        SpaceResponse response = spaceService.update(space.getCode(), request, host);

        // then
        assertPersistedPlace(space.getCode(), placeName, placeId, placeUrl);
        assertResponsePlace(response, placeName, placeId, placeUrl);
    }

    @DisplayName("장소명만 수정하면 기존 카카오 정보는 유지하고 원문 공백도 보존한다")
    @Test
    void updatePlaceNameKeepsKakaoPlace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        space.updatePlace("전시관", "00123", "https://place.map.kakao.com/00123");
        UpdateSpaceRequest request = new UpdateSpaceRequest(
            null, null, null, null, null, null, null, null, " 새 전시관 ", null, null);

        // when
        spaceService.update(space.getCode(), request, host);

        // then
        assertPersistedPlace(space.getCode(), " 새 전시관 ", "00123", "https://place.map.kakao.com/00123");
    }

    @DisplayName("장소 수정 필드가 모두 null이면 기존 장소 전체를 유지한다")
    @Test
    void updateNullPlaceKeepsCurrentPlace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        space.updatePlace("전시관", "00123", "https://place.map.kakao.com/00123");
        UpdateSpaceRequest request = new UpdateSpaceRequest("새 이름", null, null, null, null, null, null, null, null, null, null);

        // when
        spaceService.update(space.getCode(), request, host);

        // then
        assertPersistedPlace(space.getCode(), "전시관", "00123", "https://place.map.kakao.com/00123");
    }

    @DisplayName("카카오 두 필드만 공백으로 수정하면 장소명을 유지하며 카카오 정보를 삭제한다")
    @Test
    void updateBlankKakaoPlaceKeepsPlaceName() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        space.updatePlace("전시관", "00123", "https://place.map.kakao.com/00123");
        UpdateSpaceRequest request = new UpdateSpaceRequest(null, null, null, null, null, null, null, null, null, "", " ");

        // when
        spaceService.update(space.getCode(), request, host);

        // then
        assertPersistedPlace(space.getCode(), "전시관", "", "");
    }

    @DisplayName("장소 세 필드를 공백으로 수정하면 장소 객체와 DB 세 컬럼을 모두 비운다")
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void updateBlankPlaceDeletesPlace(String blank) {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        space.updatePlace("전시관", "00123", "https://place.map.kakao.com/00123");
        UpdateSpaceRequest request = new UpdateSpaceRequest(null, null, null, null, null, null, null, null, blank, blank, blank);

        // when
        SpaceResponse response = spaceService.update(space.getCode(), request, host);

        // then
        assertPersistedPlace(space.getCode(), "", "", "");
        assertResponsePlace(response, "", "", "");
    }

    @DisplayName("카카오 정보 한쪽만 삭제하면 최종 장소 검증에 실패하고 기존 장소는 유지한다")
    @Test
    void updateOneKakaoFieldRejectsMergedPlace() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        space.updatePlace("전시관", "00123", "https://place.map.kakao.com/00123");
        UpdateSpaceRequest request = new UpdateSpaceRequest(null, null, null, null, null, null, null, null, null, "", null);

        // when & then
        assertThatThrownBy(() -> spaceService.update(space.getCode(), request, host))
            .isInstanceOf(BaseException.class);
        assertPersistedPlace(space.getCode(), "전시관", "00123", "https://place.map.kakao.com/00123");
    }

    @DisplayName("생성 시 카카오 검증 실패를 전달한다")
    @Test
    void rejectsCreateWhenKakaoValidationFails() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateSpaceRequest request = new CreateSpaceRequest(
            "전시", null, true, null, null, null, null, "전시관", "123", "https://example.com/123");

        // when & then
        assertThatThrownBy(() -> spaceService.create(request, host)).isInstanceOf(BaseException.class);
    }

    @DisplayName("카카오 ID만 수정하면 기존 URL과 병합한 결과를 검증하고 거부 시 기존 장소를 유지한다")
    @Test
    void validatesMergedKakaoPlaceOnUpdate() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = saveSpaceOf(host, "1111111111");
        space.updatePlace("전시관", "00123", "https://place.map.kakao.com/00123");
        UpdateSpaceRequest request = new UpdateSpaceRequest(
            null, null, null, null, null, null, null, null, null, "456", null);

        // when & then
        assertThatThrownBy(() -> spaceService.update(space.getCode(), request, host))
            .isInstanceOf(BaseException.class);
        assertPersistedPlace(space.getCode(), "전시관", "00123", "https://place.map.kakao.com/00123");
    }

    private void assertPersistedPlace(String spaceCode, String placeName, String placeId,
        String placeUrl) {
        entityManager.flush();
        entityManager.clear();
        Space reloaded = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        assertThat(reloaded.getPlace()).isNotNull();
        assertAll(
            () -> assertThat(reloaded.getPlace().getPlaceName()).isEqualTo(placeName),
            () -> assertThat(reloaded.getPlace().getPlaceId()).isEqualTo(placeId),
            () -> assertThat(reloaded.getPlace().getPlaceUrl()).isEqualTo(placeUrl)
        );
        Object[] columns = (Object[]) entityManager.createNativeQuery(
                "SELECT place_name, place_id, place_url FROM space WHERE code = :code")
            .setParameter("code", spaceCode)
            .getSingleResult();
        assertThat(columns).containsExactly(placeName, placeId, placeUrl);
    }

    private void assertResponsePlace(SpaceResponse response, String placeName, String placeId,
        String placeUrl) {
        JsonNode json = objectMapper.valueToTree(response);
        assertThat(json.has("placeName")).isTrue();
        assertThat(json.has("placeId")).isTrue();
        assertThat(json.has("placeUrl")).isTrue();
        assertAll(
            () -> assertThat(json.get("placeName")).isEqualTo(objectMapper.valueToTree(placeName)),
            () -> assertThat(json.get("placeId")).isEqualTo(objectMapper.valueToTree(placeId)),
            () -> assertThat(json.get("placeUrl")).isEqualTo(objectMapper.valueToTree(placeUrl))
        );
    }

    private Space saveSpaceOf(Host owner, String spaceCode) {
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode(spaceCode));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space, owner));
        return space;
    }
}
