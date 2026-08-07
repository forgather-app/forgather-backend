package com.forgather.domain.space.dto;

import java.util.List;

import com.forgather.domain.space.model.Space;

import io.swagger.v3.oas.annotations.media.Schema;

public record FeaturedSpacesResponse(

    @Schema(description = "처리 후 '지금 축하받고 있는 스페이스'로 지정되어 있는 호스트의 전체 스페이스 코드 목록. "
        + "이번 요청으로 지정한 것뿐 아니라 이전에 지정되어 있던 것도 함께 담깁니다.",
        example = "[\"1234567890\", \"0987654321\"]")
    List<String> featuredSpaceCodes
) {

    /**
     * 요청 값이 아니라 처리 후 엔티티 상태에서 뽑는다. 클라이언트가 재조회 없이 최종 상태를 신뢰할 수 있어야 하기 때문이다.
     */
    public static FeaturedSpacesResponse from(List<Space> spaces) {
        return new FeaturedSpacesResponse(
            spaces.stream()
                .filter(Space::isFeatured)
                .map(Space::getCode)
                .toList()
        );
    }
}
