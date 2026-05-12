package com.forgather.domain.exhibition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.container.TestOnContainer;
import com.forgather.domain.exhibition.dto.CreateExhibitionRequest;
import com.forgather.domain.exhibition.dto.ExhibitionResponse;
import com.forgather.domain.exhibition.dto.LocationRequest;
import com.forgather.domain.exhibition.dto.OperatingHourRequest;
import com.forgather.domain.exhibition.model.LocationType;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.exception.BaseException;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class ExhibitionServiceTest extends TestOnContainer {

    private final ExhibitionService exhibitionService;
    private final HostRepository hostRepository;

    @Autowired
    public ExhibitionServiceTest(ExhibitionService exhibitionService, HostRepository hostRepository) {
        this.exhibitionService = exhibitionService;
        this.hostRepository = hostRepository;
    }

    @DisplayName("온라인 전시를 운영 시간과 함께 생성할 수 있다.")
    @Test
    void createOnlineExhibitionWithOperatingHours() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/abc.webp",
            1024L,
            "봄 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명입니다.",
            "운영 공지사항입니다.",
            List.of(
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.SATURDAY, LocalTime.of(11, 0), LocalTime.of(20, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when
        ExhibitionResponse response = exhibitionService.create(host, request);

        // then
        assertAll(
            () -> assertThat(response.id()).isNotNull(),
            () -> assertThat(response.title()).isEqualTo("봄 전시"),
            () -> assertThat(response.representativeImagePath()).isEqualTo("exhibitions/abc.webp"),
            () -> assertThat(response.location().locationType()).isEqualTo(LocationType.ONLINE),
            () -> assertThat(response.location().url()).isEqualTo("https://forgather.app"),
            () -> assertThat(response.operatingHours()).hasSize(2),
            () -> assertThat(response.creator().id()).isEqualTo(host.getId())
        );
    }

    @DisplayName("오프라인 전시를 운영 시간 없이 생성할 수 있다.")
    @Test
    void createOfflineExhibitionWithoutOperatingHours() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/offline.webp",
            2048L,
            "여름 전시",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31),
            "오프라인 전시입니다.",
            null,
            null,
            new LocationRequest(LocationType.OFFLINE, null, "서울특별시 송파구", "루터회관")
        );

        // when
        ExhibitionResponse response = exhibitionService.create(host, request);

        // then
        assertAll(
            () -> assertThat(response.id()).isNotNull(),
            () -> assertThat(response.location().locationType()).isEqualTo(LocationType.OFFLINE),
            () -> assertThat(response.location().baseAddress()).isEqualTo("서울특별시 송파구"),
            () -> assertThat(response.location().detailAddress()).isEqualTo("루터회관"),
            () -> assertThat(response.operatingHours()).isNull()
        );
    }

    @DisplayName("같은 요일이 중복 입력되면 예외가 발생한다.")
    @Test
    void rejectsDuplicateDayOfWeek() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/dup.webp",
            1024L,
            "중복 요일 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            "운영 공지",
            List.of(
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(20, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        assertThatThrownBy(() -> exhibitionService.create(host, request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("같은 요일");
    }

    @DisplayName("일부 요일만 운영해도 정상 생성된다.")
    @Test
    void createWithPartialDays() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/partial.webp",
            1024L,
            "월수금 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            "운영 공지",
            List.of(
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when
        ExhibitionResponse response = exhibitionService.create(host, request);

        // then
        assertThat(response.operatingHours()).hasSize(3);
    }

    @DisplayName("운영 시간 응답은 MONDAY..SUNDAY 순으로 정렬된다.")
    @Test
    void responseSortedByDayOfWeek() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/sort.webp",
            1024L,
            "정렬 검증 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            "운영 공지",
            List.of(
                new OperatingHourRequest(DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when
        ExhibitionResponse response = exhibitionService.create(host, request);

        // then
        assertThat(response.operatingHours())
            .extracting("dayOfWeek")
            .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    }

    @DisplayName("같은 요일에 여러 시간대를 입력하면 예외가 발생한다")
    @Test
    void rejectsMultipleTimeRangesForSameDay() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/multi-range.webp",
            1024L,
            "다중 구간 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            "운영 공지",
            List.of(
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0)),
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        assertThatThrownBy(() -> exhibitionService.create(host, request))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("같은 요일");
    }

    @DisplayName("운영 시간 빈 리스트로 생성하면 응답의 운영 시간은 null이다.")
    @Test
    void createWithEmptyOperatingHours() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/empty.webp",
            1024L,
            "빈 운영시간 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            "운영 공지",
            List.of(),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when
        ExhibitionResponse response = exhibitionService.create(host, request);

        // then
        assertThat(response.operatingHours()).isNull();
    }
}
