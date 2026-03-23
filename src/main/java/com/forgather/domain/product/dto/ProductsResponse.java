package com.forgather.domain.product.dto;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductsResponse(
    @ArraySchema(
        schema = @Schema(implementation = SimpleProductResponse.class),
        arraySchema = @Schema(description = "작품 목록")
    )
    List<SimpleProductResponse> products
) {

    public ProductsResponse(List<SimpleProductResponse> products) {
        this.products = new ArrayList<>(products);
    }
}
