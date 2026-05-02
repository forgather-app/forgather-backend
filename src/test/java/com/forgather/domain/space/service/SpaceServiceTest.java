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

import com.forgather.domain.space.dto.CreateSpaceRequest;
import com.forgather.domain.space.dto.UserSpaceResponse;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.AppUserRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.AppUserFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.SpaceHostFixture;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.NotFoundException;
import com.forgather.container.TestOnContainer;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class SpaceServiceTest extends TestOnContainer {

    private final SpaceService spaceService;
    private final SpaceRepository spaceRepository;
    private final AppUserRepository userRepository;
    private final SpaceHostRepository spaceHostRepository;

    @Autowired
    public SpaceServiceTest(SpaceService spaceService, SpaceRepository spaceRepository, AppUserRepository userRepository,
        SpaceHostRepository spaceHostRepository
    ) {
        this.spaceService = spaceService;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.spaceHostRepository = spaceHostRepository;
    }

    @DisplayName("스페이스 생성 시, 검증에 실패하면 스페이스가 DB에 저장되지 않는다.")
    @Test
    void createSpaceWithInvalidName() {
        // given
        AppUser user = userRepository.save(AppUserFixture.createAppUser());
        String invalidSpaceName = " "; // 스페이스 이름이 공백인 경우
        CreateSpaceRequest request = new CreateSpaceRequest(
            invalidSpaceName,
            "description",
            true,
            "forgather_official",
            "forgather@forgather.me"
        );
        MultipartFile file = new MockMultipartFile("temp.png", new byte[] {});

        // when & then
        assertAll(
            () -> assertThatException().isThrownBy(() -> spaceService.create(request, file, user)),
            () -> assertThat(spaceRepository.findAllByDeletedAtIsNull()).isEmpty()
        );
    }

    @DisplayName("호스트의 스페이스 목록 조회 시 논리 삭제된 스페이스는 조회하지 않는다")
    @Test
    void doesNotReturnSoftDeletedSpaceWhenQueryUserSpaces() {
        // given
        AppUser user = userRepository.save(AppUserFixture.createAppUser());
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCode("abcdefghij"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("1234567890"));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndAppUser(space1, user));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndAppUser(space2, user));
        spaceService.delete(space1.getCode(), user);

        // when
        UserSpaceResponse result = spaceService.getSpacesInformation(user);

        // then
        assertThat(result.spaces().getFirst().spaceCode()).isEqualTo(space2.getCode());
    }

    @DisplayName("논리 삭제된 스페이스를 조회하면 예외를 던진다")
    @Test
    void shouldThrowExceptionWhenQuerySoftDeletedSpace() {
        // given
        AppUser user = userRepository.save(AppUserFixture.createAppUser());
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode("abcdefghij"));
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndAppUser(space, user));
        spaceService.delete(space.getCode(), user);

        // when & then
        assertThatThrownBy(() -> spaceService.getSpaceInformation(space.getCode()))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않는 스페이스입니다.");
    }
}
