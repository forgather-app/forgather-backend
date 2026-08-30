package com.forgather.domain.host.service;

import static com.forgather.fixture.GuestBookCardFixture.createGuestBookCardWithSpace;
import static com.forgather.fixture.HostFixture.createHostWithName;
import static com.forgather.fixture.ProductFixture.createProductWithSpace;
import static com.forgather.fixture.SpaceFixture.createSpace;
import static com.forgather.fixture.SpaceHostFixture.createSpaceHostWithSpaceAndHost;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookCardPhoto;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.model.Photo;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;
import com.forgather.global.auth.model.Host;

@Import(HostPhotoFinder.class)
@DataJpaTest
class HostPhotoFinderTest {

    @Autowired
    private HostPhotoFinder hostPhotoFinder;

    @Autowired
    private TestEntityManager entityManager;

    private Host saveHostWithSpace(String name, Space space) {
        Host host = entityManager.persist(createHostWithName(name));
        entityManager.persist(space);
        entityManager.persist(createSpaceHostWithSpaceAndHost(space, host));
        return host;
    }

    @DisplayName("soft delete된 행을 포함해 호스트의 모든 저장소 사진을 수집한다")
    @Test
    void findAllIncludingSoftDeleted() {
        // given
        Space space = createSpace();
        Host host = saveHostWithSpace("탈퇴회원", space);

        HostProfilePhoto profilePhoto = new HostProfilePhoto("profile-path", 1024L, host);
        profilePhoto.delete();
        entityManager.persist(profilePhoto);

        SpacePhoto spacePhoto = new SpacePhoto(space, "space-path", 1024L);
        spacePhoto.delete();
        entityManager.persist(spacePhoto);

        Product product = entityManager.persist(createProductWithSpace(space));
        ProductPhoto productPhoto = new ProductPhoto(product, "originalName", "product-path", 1024L, 1);
        productPhoto.delete();
        entityManager.persist(productPhoto);

        GuestBookCard card = entityManager.persist(createGuestBookCardWithSpace(space));
        GuestBookCardPhoto cardPhoto = new GuestBookCardPhoto("originalName", "card-path", 1024L, card);
        cardPhoto.delete();
        entityManager.persist(cardPhoto);

        // when
        List<Photo> photos = hostPhotoFinder.findAllIncludingDeleted(host);

        // then
        assertThat(photos).extracting(Photo::getPath)
            .containsExactlyInAnyOrder("profile-path", "space-path", "product-path", "card-path");
    }

    @DisplayName("탈퇴 전에 삭제한 스페이스의 사진도 수집한다")
    @Test
    void findPhotosOfDeletedSpace() {
        // given
        Space space = createSpace();
        space.delete();
        Host host = saveHostWithSpace("탈퇴회원", space);

        SpacePhoto spacePhoto = new SpacePhoto(space, "deleted-space-path", 1024L);
        spacePhoto.delete();
        entityManager.persist(spacePhoto);

        // when
        List<Photo> photos = hostPhotoFinder.findAllIncludingDeleted(host);

        // then
        assertThat(photos).extracting(Photo::getPath).containsExactly("deleted-space-path");
    }

    @DisplayName("다른 호스트의 사진은 수집하지 않는다")
    @Test
    void excludeOtherHostPhotos() {
        // given
        Host host = entityManager.persist(createHostWithName("탈퇴회원"));

        Space otherSpace = createSpace();
        Host otherHost = saveHostWithSpace("다른회원", otherSpace);
        entityManager.persist(new HostProfilePhoto("other-profile-path", 1024L, otherHost));
        entityManager.persist(new SpacePhoto(otherSpace, "other-space-path", 1024L));

        // when
        List<Photo> photos = hostPhotoFinder.findAllIncludingDeleted(host);

        // then
        assertThat(photos).isEmpty();
    }

    @DisplayName("path가 빈 사진 행도 수집한다")
    @Test
    void includeEmptyPathPhotos() {
        // given
        Space emptyPhotoSpace = createSpace();
        Host host = saveHostWithSpace("탈퇴회원", emptyPhotoSpace);
        entityManager.persist(SpacePhoto.empty(emptyPhotoSpace));

        Space space = createSpace();
        entityManager.persist(space);
        entityManager.persist(createSpaceHostWithSpaceAndHost(space, host));
        entityManager.persist(new SpacePhoto(space, "space-path", 1024L));

        // when
        List<Photo> photos = hostPhotoFinder.findAllIncludingDeleted(host);

        // then
        assertThat(photos).extracting(Photo::getPath).containsExactlyInAnyOrder("", "space-path");
    }
}
