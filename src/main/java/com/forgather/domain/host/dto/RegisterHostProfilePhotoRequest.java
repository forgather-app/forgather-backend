package com.forgather.domain.host.dto;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.upload.domain.UploadFileMetadata;
import com.forgather.domain.upload.domain.UploadFileNamePolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record RegisterHostProfilePhotoRequest(

    @NotBlank
    @Pattern(
        regexp = UploadFileNamePolicy.FILENAME_PATTERN,
        message = "올바르지 않은 업로드 파일명 형식입니다."
    )
    @Schema(description = "업로드 파일 이름 (UUID.확장자)", example = "UUID1.webp")
    String uploadFileName,

    @NotNull
    @Positive
    @Max(value = UploadFileMetadata.MAX_FILE_SIZE_BYTES, message = "업로드 파일 크기는 최대 20MB 입니다.")
    @Schema(description = "업로드 파일 크기 (바이트)", example = "1048576")
    Long capacity
) {

    public HostProfilePhoto toEntity(String path, Host host) {
        return new HostProfilePhoto(path, capacity, host);
    }
}
