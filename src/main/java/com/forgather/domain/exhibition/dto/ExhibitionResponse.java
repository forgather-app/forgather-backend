package com.forgather.domain.exhibition.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import com.forgather.domain.exhibition.model.Exhibition;
import com.forgather.domain.exhibition.model.ExhibitionPhoto;
import com.forgather.domain.exhibition.model.ExhibitionTimes;
import com.forgather.global.auth.model.Host;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public record ExhibitionResponse(

    @Schema(description = "전시 ID", example = "1")
    Long id,

    @Schema(description = "전시 제목", example = "봄 전시")
    String title,

    @Schema(description = "전시 소개 (미설정 시 null)", example = "졸업 전시입니다.")
    String description,

    @Schema(description = "운영 공지 (미설정 시 null)", example = "첫번째 주 일요일은 휴관합니다.")
    String operationNotice,

    @Schema(description = "대표 이미지 경로", example = "exhibitions/abc.webp")
    String representativeImagePath,

    @Schema(description = "시작일", example = "2026-06-01")
    LocalDate startDate,

    @Schema(description = "종료일", example = "2026-06-30")
    LocalDate endDate,

    @Schema(
        description = "진행 상태 (KST 기준 오늘 날짜로 계산)",
        example = "UPCOMING",
        allowableValues = {"UPCOMING", "IN_PROGRESS", "ENDED"}
    )
    String progressStatus,

    @ArraySchema(
        schema = @Schema(implementation = TimeRangeResponse.class),
        arraySchema = @Schema(
            description = "운영 시간 (미설정 시 null, 운영 요일만 MONDAY~SUNDAY 순으로 포함)",
            example = """
                [
                  {"dayOfWeek": "MONDAY", "startTime": "10:00", "endTime": "18:00"},
                  {"dayOfWeek": "TUESDAY", "startTime": "10:00", "endTime": "18:00"}
                ]"""
        )
    )
    List<TimeRangeResponse> operatingHours,

    @Schema(description = "장소 (미설정 시 null)")
    LocationResponse location,

    @Schema(description = "생성자 호스트 정보")
    CreatorInfo creator,

    @Schema(description = "생성 일시", example = "2026-05-12T15:30:00")
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
            exhibition.calculateProgressStatus(LocalDate.now(ZoneId.of("Asia/Seoul"))).name(),
            times.isEmpty() ? null : times.getValues()
                .stream()
                .map(TimeRangeResponse::from)
                .toList(),
            LocationResponse.from(exhibition.getLocation()),
            CreatorInfo.from(host),
            exhibition.getCreatedAt()
        );
    }
}
