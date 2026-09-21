package com.forgather.domain.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.container.TestOnContainer;
import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.model.ExhibitionHost;
import com.forgather.domain.exhibition.repository.ExhibitionHostRepository;
import com.forgather.domain.exhibition.repository.ExhibitionRepository;
import com.forgather.domain.exhibition.repository.jpa.ExhibitionHostJpaRepository;
import com.forgather.domain.exhibition.repository.jpa.ExhibitionJpaRepository;
import com.forgather.domain.host.model.AppleHost;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.repository.AppleHostRepository;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.domain.host.repository.jpa.HostProfilePhotoJpaRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.space.repository.jpa.SpaceJpaRepository;
import com.forgather.fixture.ExhibitionFixture;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.fixture.SpaceHostFixture;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxStatus;
import com.forgather.global.outbox.OutboxType;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class WithdrawServiceTest extends TestOnContainer {

    private final WithdrawService withdrawService;
    private final HostRepository hostRepository;
    private final HostProfilePhotoRepository photoRepository;
    private final HostProfilePhotoJpaRepository photoJpaRepository;
    private final KakaoHostRepository kakaoHostRepository;
    private final AppleHostRepository appleHostRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final SpaceJpaRepository spaceJpaRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionHostRepository exhibitionHostRepository;
    private final ExhibitionJpaRepository exhibitionJpaRepository;
    private final ExhibitionHostJpaRepository exhibitionHostJpaRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Autowired
    public WithdrawServiceTest(WithdrawService withdrawService, HostRepository hostRepository,
        HostProfilePhotoRepository photoRepository, HostProfilePhotoJpaRepository photoJpaRepository,
        KakaoHostRepository kakaoHostRepository, AppleHostRepository appleHostRepository,
        SpaceRepository spaceRepository, SpaceHostRepository spaceHostRepository,
        SpaceJpaRepository spaceJpaRepository, ExhibitionRepository exhibitionRepository,
        ExhibitionHostRepository exhibitionHostRepository, ExhibitionJpaRepository exhibitionJpaRepository,
        ExhibitionHostJpaRepository exhibitionHostJpaRepository, OutboxService outboxService,
        ObjectMapper objectMapper
    ) {
        this.withdrawService = withdrawService;
        this.hostRepository = hostRepository;
        this.photoRepository = photoRepository;
        this.photoJpaRepository = photoJpaRepository;
        this.kakaoHostRepository = kakaoHostRepository;
        this.appleHostRepository = appleHostRepository;
        this.spaceRepository = spaceRepository;
        this.spaceHostRepository = spaceHostRepository;
        this.spaceJpaRepository = spaceJpaRepository;
        this.exhibitionRepository = exhibitionRepository;
        this.exhibitionHostRepository = exhibitionHostRepository;
        this.exhibitionJpaRepository = exhibitionJpaRepository;
        this.exhibitionHostJpaRepository = exhibitionHostJpaRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    private SocialRevokePayload readPayload(Outbox outbox) throws JsonProcessingException {
        return objectMapper.readValue(outbox.getPayload(), SocialRevokePayload.class);
    }

    @DisplayName("탈퇴하면 프로필 사진도 삭제 처리된다.")
    @Test
    void deleteProfilePhoto() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        HostProfilePhoto photo = photoRepository.save(
            new HostProfilePhoto("photogather/v2/hosts/1/profile/a.webp", 1024L, host));

        // when
        withdrawService.withdraw(host);

        // then
        assertAll(
            () -> assertThat(photoRepository.findByHostAndDeletedAtIsNull(host)).isEmpty(),
            () -> assertThat(photoJpaRepository.findById(photo.getId()).orElseThrow().getDeletedAt()).isNotNull()
        );
    }

    @DisplayName("프로필 사진이 없어도 탈퇴할 수 있다.")
    @Test
    void deleteWithoutProfilePhoto() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        withdrawService.withdraw(host);

        // then
        assertThat(hostRepository.getByIdOrThrow(host.getId()).getDeletedAt()).isNotNull();
    }

    @DisplayName("스페이스를 소유한 채 탈퇴하면 스페이스와 호스트 매핑이 삭제 처리된다.")
    @Test
    void deleteOwnedSpaces() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spaceHostRepository.save(SpaceHostFixture.createSpaceHostWithSpaceAndHost(space, host));

        // when
        withdrawService.withdraw(host);

        // then
        assertAll(
            () -> assertThat(spaceJpaRepository.findById(space.getId()).orElseThrow().getDeletedAt()).isNotNull(),
            () -> assertThat(spaceHostRepository.findBySpaceAndHostAndDeletedAtIsNull(space, host)).isEmpty()
        );
    }

    @DisplayName("탈퇴하면 전시 호스트 매핑만 삭제되고 전시 자체는 유지된다.")
    @Test
    void deleteExhibitionHostOnly() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        Exhibition exhibition = exhibitionRepository.save(ExhibitionFixture.createOnlineExhibition());
        ExhibitionHost exhibitionHost =
            exhibitionHostRepository.save(new ExhibitionHost(exhibition, host, true));

        // when
        withdrawService.withdraw(host);

        // then
        assertAll(
            () -> assertThat(exhibitionHostJpaRepository.findById(exhibitionHost.getId())
                .orElseThrow().getDeletedAt()).isNotNull(),
            () -> assertThat(exhibitionJpaRepository.findById(exhibition.getId())
                .orElseThrow().getDeletedAt()).isNull()
        );
    }

    @DisplayName("탈퇴하면 소셜 매핑은 물리 삭제되어 같은 소셜 계정으로 다시 가입할 수 있다.")
    @Test
    void hardDeleteSocialAccount() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        kakaoHostRepository.save(new KakaoHost(host, "kakao-user-1"));

        // when
        withdrawService.withdraw(host);

        // then
        assertThat(kakaoHostRepository.findByUserId("kakao-user-1")).isEmpty();
    }

    @DisplayName("Kakao 계정으로 탈퇴하면 연결 해제 outbox가 PENDING으로 생성된다.")
    @Test
    void saveKakaoRevokeOutbox() throws JsonProcessingException {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        kakaoHostRepository.save(new KakaoHost(host, "kakao-user-1"));

        // when
        withdrawService.withdraw(host);

        // then
        List<Outbox> outboxes = outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE);
        assertThat(outboxes).hasSize(1);
        Outbox outbox = outboxes.getFirst();
        SocialRevokePayload payload = readPayload(outbox);
        assertAll(
            () -> assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING),
            () -> assertThat(outbox.getFailCount()).isZero(),
            () -> assertThat(payload.hostId()).isEqualTo(host.getId()),
            () -> assertThat(payload.provider()).isEqualTo(SocialProvider.KAKAO),
            () -> assertThat(payload.userId()).isEqualTo("kakao-user-1"),
            () -> assertThat(payload.refreshToken()).isNull()
        );
    }

    @DisplayName("Apple 계정으로 탈퇴하면 refresh token을 담은 연결 해제 outbox가 생성된다.")
    @Test
    void saveAppleRevokeOutboxWithRefreshToken() throws JsonProcessingException {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        appleHostRepository.save(new AppleHost(host, "apple-user-1", "apple-refresh-token"));

        // when
        withdrawService.withdraw(host);

        // then
        List<Outbox> outboxes = outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE);
        assertThat(outboxes).hasSize(1);
        SocialRevokePayload payload = readPayload(outboxes.getFirst());
        assertAll(
            () -> assertThat(payload.hostId()).isEqualTo(host.getId()),
            () -> assertThat(payload.provider()).isEqualTo(SocialProvider.APPLE),
            () -> assertThat(payload.userId()).isEqualTo("apple-user-1"),
            () -> assertThat(payload.refreshToken()).isEqualTo("apple-refresh-token")
        );
    }

    @DisplayName("소셜 계정이 없는 호스트가 탈퇴하면 연결 해제 outbox를 생성하지 않는다.")
    @Test
    void skipRevokeOutboxWithoutSocialAccount() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        withdrawService.withdraw(host);

        // then
        assertThat(outboxService.findPendingTasks(OutboxType.SOCIAL_REVOKE)).isEmpty();
    }
}
