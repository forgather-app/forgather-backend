package com.forgather.domain.exhibition.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.forgather.domain.host.model.Host;
import com.forgather.fixture.ExhibitionFixture;
import com.forgather.fixture.HostFixture;
import com.forgather.global.exception.BaseNullPointerException;

class ExhibitionHostTest {

    @Nested
    @DisplayName("필수 필드 검증")
    class RequiredFields {

        @DisplayName("전시가 null이면 생성할 수 없다.")
        @Test
        void createWithNullExhibition() {
            // given
            Host host = HostFixture.createHost();

            // when & then
            assertThatThrownBy(() -> new ExhibitionHost(null, host, true))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("전시");
        }

        @DisplayName("전시 호스트가 null이면 생성할 수 없다.")
        @Test
        void createWithNullHost() {
            // given
            Exhibition exhibition = ExhibitionFixture.createOnlineExhibition();

            // when & then
            assertThatThrownBy(() -> new ExhibitionHost(exhibition, null, true))
                .isInstanceOf(BaseNullPointerException.class)
                .hasMessageContaining("전시 호스트");
        }
    }
}
