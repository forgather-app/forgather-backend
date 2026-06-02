package com.forgather.domain.upload.dto;

import java.util.List;

import com.forgather.domain.upload.domain.UploadFileNamePolicy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record IssuePreSignedUrlRequest(

    @NotEmpty
    @Schema(description = "업로드 파일 이름 목록", example = "[\"UUID1.png\", \"UUID2.jpeg\", \"UUID3.png\"]")
    List<
        @NotBlank
        @Pattern(
            regexp = UploadFileNamePolicy.FILENAME_PATTERN,
            message = "올바르지 않은 업로드 파일명 형식입니다."
        ) String>
    uploadFileNames
) {
}
