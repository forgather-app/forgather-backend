package com.forgather.domain.product.dto;

import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SimpleProductResponse(
    @Schema(description = "작품 id", example = "1")
    Long id,

    @Schema(description = "작품명", example = "고귀한 의자")
    String title,

    @Schema(description = "임베드 영상 링크", example = "https://youtu.be/lkuAxAVgAX0")
    String videoUrl,

    @Schema(description = "첫 번째 사진 정보 (없으면 null)")
    ProductPhotoResponse firstPhoto
) {

    public SimpleProductResponse(Product product, ProductPhoto firstPhoto) {
        this(product.getId(),
            product.getTitle(),
            product.getVideoUrl(),
            (firstPhoto != null) ? new ProductPhotoResponse(firstPhoto) : null
        );
    }
}
