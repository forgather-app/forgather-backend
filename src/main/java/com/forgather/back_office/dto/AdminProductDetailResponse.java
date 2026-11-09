package com.forgather.back_office.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;

public record AdminProductDetailResponse(
    Long id,
    String title,
    String authorName,
    String description,
    String videoUrl,
    Boolean isVideoAfterPhoto,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    SimpleSpaceResponse space,
    AdminProductHostResponse host,
    List<AdminProductPhotoResponse> photos
) {

    /**
     * @param host 스페이스의 호스트. 호스트가 탈퇴했거나 연결되지 않았으면 null
     */
    public static AdminProductDetailResponse of(Product product, Host host, List<ProductPhoto> photos) {
        return new AdminProductDetailResponse(
            product.getId(),
            product.getTitle(),
            product.getAuthorName(),
            product.getDescription(),
            product.getVideoUrl(),
            product.isVideoAfterPhoto(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            SimpleSpaceResponse.from(product.getSpace()),
            (host != null) ? AdminProductHostResponse.from(host) : null,
            photos.stream().map(AdminProductPhotoResponse::from).toList()
        );
    }
}
