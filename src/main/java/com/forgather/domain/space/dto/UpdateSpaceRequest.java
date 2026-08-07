package com.forgather.domain.space.dto;

import org.hibernate.validator.constraints.URL;

import com.forgather.global.validation.TextSize;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;

public record UpdateSpaceRequest(

    @Schema(description = "새로운 스페이스 이름", example = "나의 졸업전시", maxLength = 30, nullable = true)
    @TextSize(max = 30)
    String name,

    @Schema(description = "새로운 스페이스 설명", example = "졸업전시 스페이스입니다.", maxLength = 200, nullable = true)
    @TextSize(max = 200)
    String description,

    @Schema(description = "새로운 스페이스 공개여부", example = "true", nullable = true)
    Boolean isPublic,

    @Schema(description = "새로운 인스타그램 아이디", example = "forgather_official_new", maxLength = 30, nullable = true)
    @TextSize(max = 30)
    String instagramUsername,

    @Schema(description = "새로운 이메일", example = "forgather_new@forgather.me", maxLength = 50, nullable = true)
    @Email
    @TextSize(max = 50)
    String email,

    @Schema(
        description = "새로운 소개 링크 URL (표시 이름과 함께 입력, 빈 쌍으로 삭제)",
        example = "https://forgather.app",
        maxLength = 2048,
        nullable = true
    )
    @URL
    String linkUrl,

    @Schema(
        description = "새로운 소개 링크 표시 이름 (URL과 함께 입력)",
        example = "포트폴리오",
        maxLength = 30,
        nullable = true
    )
    String linkName,

    @Schema(
        description = "스페이스 사진 삭제 여부. 사진을 지울 때만 사용한다. "
            + "photo를 함께 보내면 무시되며 교체로 동작한다. 삭제할 사진이 없어도 성공한다. "
            + "생략하거나 null이면 삭제하지 않는다.",
        example = "true",
        defaultValue = "false",
        nullable = true
    )
    Boolean isDeletePhoto,

    @Schema(
        description = "새로운 스페이스 사진 (선택). 발급받은 presigned URL로 업로드를 마친 뒤 전달한다. "
            + "기존 사진이 있으면 교체되고, 없으면 새로 등록된다.",
        nullable = true
    )
    @Valid
    SpacePhotoRequest photo
) {

    public boolean isDeletingPhoto() {
        return isDeletePhoto != null && isDeletePhoto;
    }
}
