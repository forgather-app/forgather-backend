package com.forgather.back_office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminHostResponse;
import com.forgather.back_office.dto.HostSpacesResponse;
import com.forgather.back_office.dto.SimpleSpaceResponse;
import com.forgather.back_office.repository.AdminUserRepository;
import com.forgather.container.TestOnContainer;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.AppUserRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.AppUserFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.NotFoundException;

@Transactional
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AdminHostServiceTest extends TestOnContainer {

    @Autowired
    private AdminHostService adminHostService;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private AppUserRepository hostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    @DisplayName("전체 호스트 목록을 조회한다.")
    @Test
    void getAllHosts() {
        // given
        AppUser host1 = hostRepository.save(AppUserFixture.createAppUser());
        AppUser host2 = hostRepository.save(AppUserFixture.createAppUser());
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCode("1111111111"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("2222222222"));
        spaceHostRepository.save(new SpaceHost(space1, host1));
        spaceHostRepository.save(new SpaceHost(space2, host1));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.getAllHosts(pageable);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(2),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(10),
            () -> assertThat(result.totalCount()).isEqualTo(2),
            () -> assertThat(result.totalPages()).isEqualTo(1),
            () -> assertThat(result.hosts().get(0).spaceCount()).isEqualTo(2L),
            () -> assertThat(result.hosts().get(1).spaceCount()).isEqualTo(0L)
        );
    }

    @DisplayName("호스트가 소유한 스페이스 목록을 조회한다.")
    @Test
    void getHostSpaces() {
        // given
        AppUser host = hostRepository.save(AppUserFixture.createAppUser());
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName("1111111111", "첫번째 스페이스"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName("2222222222", "두번째 스페이스"));
        spaceHostRepository.save(new SpaceHost(space1, host));
        spaceHostRepository.save(new SpaceHost(space2, host));

        // when
        HostSpacesResponse result = adminHostService.getHostSpaces(host.getId());

        // then
        assertAll(
            () -> assertThat(result.hostId()).isEqualTo(host.getId()),
            () -> assertThat(result.hostName()).isEqualTo(host.getName()),
            () -> assertThat(result.spaces()).hasSize(2),
            () -> assertThat(result.spaces()).extracting(SimpleSpaceResponse::code)
                .containsExactlyInAnyOrder("1111111111", "2222222222"),
            () -> assertThat(result.spaces()).extracting(SimpleSpaceResponse::name)
                .containsExactlyInAnyOrder("첫번째 스페이스", "두번째 스페이스")
        );
    }

    @DisplayName("스페이스가 없는 호스트의 스페이스 목록을 조회하면 빈 목록을 반환한다.")
    @Test
    void getHostSpacesWithNoSpaces() {
        // given
        AppUser host = hostRepository.save(AppUserFixture.createAppUser());

        // when
        HostSpacesResponse result = adminHostService.getHostSpaces(host.getId());

        // then
        assertAll(
            () -> assertThat(result.hostId()).isEqualTo(host.getId()),
            () -> assertThat(result.hostName()).isEqualTo(host.getName()),
            () -> assertThat(result.spaces()).isEmpty()
        );
    }

    @DisplayName("존재하지 않는 호스트의 스페이스 목록을 조회하면 예외가 발생한다.")
    @Test
    void getHostSpacesWithNonExistentHost() {
        // given
        Long nonExistentHostId = 9999L;

        // when & then
        assertThatThrownBy(() -> adminHostService.getHostSpaces(nonExistentHostId))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("이름이 정확히 일치하면 이름 검색으로 조회된다.")
    @Test
    void searchHostsByName() {
        // given
        String name = "포스티";
        hostRepository.save(AppUserFixture.createAppUserWithName(name));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName(name, pageable);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(1),
            () -> assertThat(result.hosts().get(0).name()).isEqualTo(name)
        );
    }

    @DisplayName("이름이 포함되면 이름 검색으로 조회된다.")
    @Test
    void searchHostsByNameContaining() {
        // given
        String name = "포스";
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("1포스"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName(name, pageable);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(2),
            () -> assertThat(result.hosts()).extracting("name")
                .containsExactlyInAnyOrder("포스티", "1포스")
        );
    }

    @DisplayName("이름이 null일 경우 전체 호스트 목록을 조회한다.")
    @Test
    void searchHostsByNameNull() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("레오"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName(null, pageable);

        // then
        assertThat(result.hosts()).hasSize(2);
    }

    @DisplayName("이름에 공백 문자가 들어올 경우 전체 호스트 목록을 조회한다.")
    @Test
    void searchHostsByNameWhitespaces() {
        // given
        String name = "   ";
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("레오"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName(name, pageable);

        // then
        assertThat(result.hosts()).hasSize(2);
    }

    @DisplayName("이름이 포함되어 있지 않다면 이름 검색으로 조회되지 않는다.")
    @Test
    void searchHostsByNameNonContaining() {
        // given
        String name = "포스티";
        hostRepository.save(AppUserFixture.createAppUserWithName("포스"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName(name, pageable);

        // then
        assertThat(result.hosts()).isEmpty();
    }

    @DisplayName("검색어에 % 와일드카드 문자가 포함되면 리터럴로 처리된다.")
    @Test
    void searchHostsByNameWithPercentWildcard() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티%"));
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName("%", pageable);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(1),
            () -> assertThat(result.hosts().get(0).name()).isEqualTo("포스티%")
        );
    }

    @DisplayName("검색어에 _ 와일드카드 문자가 포함되면 리터럴로 처리된다.")
    @Test
    void searchHostsByNameWithUnderscoreWildcard() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스_티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName("_", pageable);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(1),
            () -> assertThat(result.hosts().get(0).name()).isEqualTo("포스_티")
        );
    }

    @DisplayName("검색어에 백슬래시가 포함되면 리터럴로 처리된다.")
    @Test
    void searchHostsByNameWithBackslash() {
        // given
        hostRepository.save(AppUserFixture.createAppUserWithName("포스\\티"));
        hostRepository.save(AppUserFixture.createAppUserWithName("포스티"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName("\\", pageable);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(1),
            () -> assertThat(result.hosts().get(0).name()).isEqualTo("포스\\티")
        );
    }
}
