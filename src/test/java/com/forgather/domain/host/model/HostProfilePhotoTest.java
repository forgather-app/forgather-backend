package com.forgather.domain.host.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.fixture.HostFixture;
import com.forgather.global.exception.BaseNullPointerException;

class HostProfilePhotoTest {

    private static final String PATH = "forgather/v2/hosts/1/profile/photo.webp";
    private static final Long CAPACITY = 1024L;

    @DisplayName("호스트가 null이면 생성할 수 없다.")
    @Test
    void createWithNullHost() {
        // when & then
        assertThatThrownBy(() -> new HostProfilePhoto(PATH, CAPACITY, null))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("호스트");
    }

    @DisplayName("프로필 사진은 원본 파일명 없이 생성된다.")
    @Test
    void createWithEmptyOriginalName() {
        // given
        Host host = HostFixture.createHostWithId(1L);

        // when
        HostProfilePhoto photo = new HostProfilePhoto(PATH, CAPACITY, host);

        // then
        assertThat(photo.getOriginalName()).isEmpty();
    }

    @DisplayName("생성 시점에는 삭제되지 않은 상태다.")
    @Test
    void createNotDeleted() {
        // given
        Host host = HostFixture.createHostWithId(1L);

        // when
        HostProfilePhoto photo = new HostProfilePhoto(PATH, CAPACITY, host);

        // then
        assertThat(photo.getDeletedAt()).isNull();
    }
}
