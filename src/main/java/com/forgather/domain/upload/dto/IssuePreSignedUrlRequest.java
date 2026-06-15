package com.forgather.domain.upload.dto;

import java.util.List;

import com.forgather.domain.upload.domain.UploadFileMetadata;
import com.forgather.domain.upload.domain.UploadFileNamePolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record IssuePreSignedUrlRequest(

    @NotEmpty
    @Valid
    @Schema(description = "업로드 파일 목록")
    List<UploadFileRequest> uploadFiles
) {

    public List<UploadFileMetadata> toUploadFilesData() {
        return uploadFiles.stream()
            .map(UploadFileRequest::toDomain)
            .toList();
    }

    public record UploadFileRequest(

        @NotBlank
        @Pattern(
            regexp = UploadFileNamePolicy.FILENAME_PATTERN,
            message = "올바르지 않은 업로드 파일명 형식입니다."
        )
        @Schema(description = "업로드 파일 이름 (UUID.확장자)", example = "UUID1.webp")
        String fileName,

        @NotNull
        @Positive
        @Max(value = UploadFileMetadata.MAX_FILE_SIZE_BYTES, message = "업로드 파일 크기는 최대 20MB 입니다.")
        @Schema(description = "업로드 파일 크기 (바이트)", example = "1048576")
        Long size
    ) {

        public UploadFileMetadata toDomain() {
            return new UploadFileMetadata(fileName, size);
        }
    }
}
