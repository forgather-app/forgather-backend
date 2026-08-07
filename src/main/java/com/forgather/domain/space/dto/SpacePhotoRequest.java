package com.forgather.domain.space.dto;

import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;
import com.forgather.domain.upload.domain.UploadFileMetadata;
import com.forgather.domain.upload.domain.UploadFileNamePolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record SpacePhotoRequest(

    @Schema(description = "업로드 파일명 (UUID.확장자)", example = "UUID.webp")
    @NotBlank
    @Pattern(
        regexp = UploadFileNamePolicy.FILENAME_PATTERN,
        message = "올바르지 않은 업로드 파일명 형식입니다."
    )
    String uploadFileName,

    @Schema(description = "스페이스 사진 파일 크기 (bytes)", example = "102400")
    @NotNull
    @Positive
    @Max(value = UploadFileMetadata.MAX_FILE_SIZE_BYTES, message = "업로드 파일 크기는 최대 20MB 입니다.")
    Long capacity
) {

    public SpacePhoto toEntity(Space space, String path) {
        return new SpacePhoto(space, path, capacity);
    }
}
