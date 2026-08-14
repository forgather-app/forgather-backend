package com.forgather.domain.host.service;

import static com.forgather.fixture.HostFixture.createHostWithName;
import static com.forgather.fixture.ProductFixture.createProductWithSpace;
import static com.forgather.fixture.SpaceFixture.createSpace;
import static com.forgather.fixture.SpaceHostFixture.createSpaceHostWithSpaceAndHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.model.Host;

@Import({HostAnonymizeService.class, HostPhotoFinder.class})
@DataJpaTest
class HostAnonymizeServiceTest {

    @Autowired
    private HostAnonymizeService hostAnonymizeService;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Host saveHostDeletedDaysAgo(String name, int days) {
        Host host = createHostWithName(name);
        ReflectionTestUtils.setField(host, "deletedAt", LocalDateTime.now().minusDays(days));
        return hostRepository.save(host);
    }

    @DisplayName("탈퇴 회원을 재조회해 익명화한다")
    @Test
    void anonymizeWithdrawnHost() {
        // given
        Host host = saveHostDeletedDaysAgo("만료회원", 31);

        // when
        hostAnonymizeService.anonymize(host.getId(), LocalDateTime.now());

        // then
        assertAll(
            () -> assertThat(host.getName()).isEqualTo("탈퇴한 회원"),
            () -> assertThat(host.getEmail()).isNull(),
            () -> assertThat(host.getAnonymizedAt()).isNotNull()
        );
    }

    @DisplayName("탈퇴 회원이 남긴 사진의 원본 파일명을 함께 마스킹한다")
    @Test
    void anonymizePhotoOriginalName() {
        // given
        Host host = saveHostDeletedDaysAgo("만료회원", 31);
        Space space = entityManager.persist(createSpace());
        entityManager.persist(createSpaceHostWithSpaceAndHost(space, host));
        Product product = entityManager.persist(createProductWithSpace(space));
        ProductPhoto photo = new ProductPhoto(product, "개인정보가_담긴_파일명.jpg", "product-path", 1024L, 1);
        photo.delete();
        entityManager.persist(photo);

        // when
        hostAnonymizeService.anonymize(host.getId(), LocalDateTime.now());

        // then
        assertThat(photo.getOriginalName()).isEqualTo("익명화된 파일명");
    }

    @DisplayName("이미 익명화된 회원은 다시 처리하지 않는다")
    @Test
    void skipAlreadyAnonymizedHost() {
        // given
        Host host = createHostWithName("만료회원");
        ReflectionTestUtils.setField(host, "deletedAt", LocalDateTime.now().minusDays(31));
        LocalDateTime firstAnonymizedAt = LocalDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(host, "anonymizedAt", firstAnonymizedAt);
        hostRepository.save(host);

        // when
        hostAnonymizeService.anonymize(host.getId(), LocalDateTime.now());

        // then
        assertAll(
            () -> assertThat(host.getName()).isEqualTo("만료회원"),
            () -> assertThat(host.getAnonymizedAt()).isEqualTo(firstAnonymizedAt)
        );
    }
}
