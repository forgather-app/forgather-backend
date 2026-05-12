package com.forgather.domain.exhibition.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.model.ExhibitionPhoto;
import com.forgather.domain.exhibition.model.ExhibitionTimes;
import com.forgather.global.auth.model.Host;

import io.swagger.v3.oas.annotations.media.Schema;

public record ExhibitionResponse(

    @Schema(description = "전시 ID")
    Long id,

    @Schema(description = "전시 제목")
    String title,

    @Schema(description = "전시 소개")
    String description,

    @Schema(description = "운영 공지")
    String operationNotice,

    @Schema(description = "대표 이미지 경로 (S3 key)")
    String representativeImagePath,

    @Schema(description = "시작일")
    LocalDate startDate,

    @Schema(description = "종료일")
    LocalDate endDate,

    @Schema(description = "진행 상태", example = "UPCOMING", allowableValues = "UPCOMING, IN_PROGRESS, ENDED")
    String progressStatus,

    @Schema(description = "운영 시간 (미설정 시 null, 운영 요일만 MONDAY~SUNDAY 순으로 포함)")
    OperatingHoursResponse operatingHours,

    @Schema(description = "장소 (미설정 시 null)")
    LocationResponse location,

    @Schema(description = "생성자 호스트 정보")
    CreatorInfo creator,

    @Schema(description = "생성 일시")
    LocalDateTime createdAt
) {

    public static ExhibitionResponse of(
        Exhibition exhibition,
        ExhibitionPhoto photo,
        ExhibitionTimes times,
        Host host
    ) {
        return new ExhibitionResponse(
            exhibition.getId(),
            exhibition.getTitle(),
            exhibition.getDescription(),
            exhibition.getOperationNotice(),
            photo.getPath(),
            exhibition.getStartDate(),
            exhibition.getEndDate(),
            exhibition.calculateProgressStatus(LocalDate.now()).name(),
            times.isEmpty() ? null : OperatingHoursResponse.from(times.getValues()),
            LocationResponse.from(exhibition.getLocation()),
            CreatorInfo.from(host),
            exhibition.getCreatedAt()
        );
    }

}
