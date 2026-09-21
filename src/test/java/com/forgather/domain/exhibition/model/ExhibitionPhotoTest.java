package com.forgather.domain.exhibition.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.fixture.ExhibitionFixture;
import com.forgather.global.exception.BaseNullPointerException;

class ExhibitionPhotoTest {

    private static final String PATH = "exhibition/photo.png";
    private static final Long CAPACITY = 1024L;

    @DisplayName("전시가 null이면 생성할 수 없다.")
    @Test
    void createWithNullExhibition() {
        // when & then
        assertThatThrownBy(() -> new ExhibitionPhoto(PATH, CAPACITY, null))
            .isInstanceOf(BaseNullPointerException.class)
            .hasMessageContaining("전시");
    }

    @DisplayName("전시 사진은 원본 파일명 없이 생성된다.")
    @Test
    void createWithEmptyOriginalName() {
        // given
        Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();

        // when
        ExhibitionPhoto photo = new ExhibitionPhoto(PATH, CAPACITY, exhibition);

        // then
        assertThat(photo.getOriginalName()).isEmpty();
    }
}
