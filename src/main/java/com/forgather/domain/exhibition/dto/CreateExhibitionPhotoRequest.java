package com.forgather.domain.exhibition.dto;

import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.model.ExhibitionPhoto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateExhibitionPhotoRequest(

    @Schema(description = "업로드 파일명 (UUID.확장자)", example = "UUID.webp")
    @NotBlank
    String uploadFileName,

    @Schema(description = "전시 이미지 파일 크기 (bytes)", example = "102400")
    @Positive
    @NotNull
    Long capacity
) {

    public ExhibitionPhoto toEntity(String path, Exhibition exhibition) {
        return new ExhibitionPhoto(path, capacity, exhibition);
    }
}
