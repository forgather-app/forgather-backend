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
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
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
    private HostRepository hostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    @DisplayName("전체 호스트 목록을 조회한다.")
    @Test
    void getAllHosts() {
        // given
        Host host1 = hostRepository.save(HostFixture.createHost());
        Host host2 = hostRepository.save(HostFixture.createHost());
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
        Host host = hostRepository.save(createHostWithNickname("카카오원본이름", "서비스닉네임"));
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName("1111111111", "첫번째 스페이스"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName("2222222222", "두번째 스페이스"));
        spaceHostRepository.save(new SpaceHost(space1, host));
        spaceHostRepository.save(new SpaceHost(space2, host));

        // when
        HostSpacesResponse result = adminHostService.getHostSpaces(host.getId());

        // then
        assertAll(
            () -> assertThat(result.hostId()).isEqualTo(host.getId()),
            () -> assertThat(result.hostName()).isEqualTo(host.getNickname()),
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
        Host host = hostRepository.save(createHostWithNickname("카카오원본이름", "서비스닉네임"));

        // when
        HostSpacesResponse result = adminHostService.getHostSpaces(host.getId());

        // then
        assertAll(
            () -> assertThat(result.hostId()).isEqualTo(host.getId()),
            () -> assertThat(result.hostName()).isEqualTo(host.getNickname()),
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
        hostRepository.save(createHostWithNickname("카카오원본이름", name));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName(name, pageable);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(1),
            () -> assertThat(result.hosts().get(0).name()).isEqualTo(name)
        );
    }

    @DisplayName("서비스 닉네임이 포함되면 이름 검색으로 조회된다.")
    @Test
    void searchHostsByNameContaining() {
        // given
        String name = "포스";
        hostRepository.save(createHostWithNickname("원본포스", "포스티"));
        hostRepository.save(createHostWithNickname("포스원본", "1포스"));
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
        hostRepository.save(HostFixture.createHostWithName("포스티"));
        hostRepository.save(HostFixture.createHostWithName("레오"));
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
        hostRepository.save(HostFixture.createHostWithName("포스티"));
        hostRepository.save(HostFixture.createHostWithName("레오"));
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
        hostRepository.save(HostFixture.createHostWithName("포스"));
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
        hostRepository.save(HostFixture.createHostWithName("포스티%"));
        hostRepository.save(HostFixture.createHostWithName("포스티"));
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
        hostRepository.save(HostFixture.createHostWithName("포스_티"));
        hostRepository.save(HostFixture.createHostWithName("포스티"));
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
        hostRepository.save(HostFixture.createHostWithName("포스\\티"));
        hostRepository.save(HostFixture.createHostWithName("포스티"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminHostResponse result = adminHostService.searchHostsByName("\\", pageable);

        // then
        assertAll(
            () -> assertThat(result.hosts()).hasSize(1),
            () -> assertThat(result.hosts().get(0).name()).isEqualTo("포스\\티")
        );
    }

    private Host createHostWithNickname(String name, String nickname) {
        Host host = new Host(HostFixture.randomCode(), name, "posty@forgather.app");
        host.updateNickname(nickname);
        return host;
    }
}
