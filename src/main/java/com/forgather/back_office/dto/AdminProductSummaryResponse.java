package com.forgather.back_office.dto;

import java.time.LocalDateTime;

/**
 * 어드민 작품 목록의 행 단위 응답.
 * AdminProductRepository의 JPQL 생성자 표현식이 직접 생성하므로 생성자 파라미터 순서와 타입을 쿼리와 함께 바꿔야 한다.
 */
public record AdminProductSummaryResponse(
    Long id,
    String title,
    String authorName,
    String spaceCode,
    String spaceName,
    String videoUrl,
    long photoCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
