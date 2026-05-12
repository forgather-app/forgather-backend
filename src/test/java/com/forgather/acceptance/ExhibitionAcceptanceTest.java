package com.forgather.acceptance;

import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.exhibition.dto.CreateExhibitionRequest;
import com.forgather.domain.exhibition.dto.ExhibitionResponse;
import com.forgather.domain.exhibition.dto.LocationRequest;
import com.forgather.domain.exhibition.dto.OperatingHourRequest;
import com.forgather.domain.exhibition.model.LocationType;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@DisplayName("인수 테스트: Exhibition")
@AutoConfigureMockMvc
class ExhibitionAcceptanceTest extends AcceptanceTest {

    private static final String API_VERSION_HEADER = "X-API-Version";
    private static final String API_VERSION_V1 = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Host host;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        host = hostRepository.save(createHost());
        token = jwtTokenProvider.generateAccessToken(host.getId());
    }

    @DisplayName("운영 시간과 함께 온라인 전시를 생성한다.")
    @Test
    void createOnlineExhibition() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/spring.webp",
            1024L,
            "봄 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "봄 전시 설명입니다.",
            "운영 공지사항입니다.",
            List.of(
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.SATURDAY, LocalTime.of(11, 0), LocalTime.of(20, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when
        ApiResponse<ExhibitionResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(response.message()).isNull(),
            () -> assertThat(response.data().id()).isNotNull(),
            () -> assertThat(response.data().title()).isEqualTo("봄 전시"),
            () -> assertThat(response.data().representativeImagePath()).isEqualTo("exhibitions/spring.webp"),
            () -> assertThat(response.data().location().locationType()).isEqualTo(LocationType.ONLINE),
            () -> assertThat(response.data().location().url()).isEqualTo("https://forgather.app"),
            () -> assertThat(response.data().operatingHours().timeRanges()).hasSize(2),
            () -> assertThat(response.data().creator().id()).isEqualTo(host.getId()),
            () -> assertThat(response.data().creator().name()).isEqualTo(host.getName())
        );
    }

    @DisplayName("운영 시간 없이 오프라인 전시를 생성한다.")
    @Test
    void createOfflineExhibitionWithoutOperatingHours() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/summer.webp",
            2048L,
            "여름 전시",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31),
            "오프라인 여름 전시입니다.",
            null,
            null,
            new LocationRequest(LocationType.OFFLINE, null, "서울특별시 송파구", "루터회관")
        );

        // when
        ApiResponse<ExhibitionResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertAll(
            () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(response.data().location().locationType()).isEqualTo(LocationType.OFFLINE),
            () -> assertThat(response.data().location().baseAddress()).isEqualTo("서울특별시 송파구"),
            () -> assertThat(response.data().location().detailAddress()).isEqualTo("루터회관"),
            () -> assertThat(response.data().operatingHours()).isNull()
        );
    }

    @DisplayName("운영 시간 응답은 MONDAY..SUNDAY 순으로 정렬된다.")
    @Test
    void operatingHoursSortedByDayOfWeek() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/sort.webp",
            1024L,
            "정렬 검증 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "정렬 검증을 위한 전시입니다.",
            "운영 공지",
            List.of(
                new OperatingHourRequest(DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when
        ApiResponse<ExhibitionResponse> response = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(response.data().operatingHours().timeRanges())
            .extracting("dayOfWeek")
            .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
    }

    @DisplayName("로그인 없이 전시를 생성할 수 없다.")
    @Test
    void createExhibitionWithoutLogin() {
        // given
        CreateExhibitionRequest request = createValidRequest();

        // when & then
        RestAssuredMockMvc.given()
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .body("code", equalTo("UNAUTHORIZED"));
    }

    @DisplayName("전시 제목이 비어 있으면 전시를 생성할 수 없다.")
    @Test
    void createExhibitionWithoutTitle() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/empty-title.webp",
            1024L,
            "",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("이미지 경로가 비어 있으면 전시를 생성할 수 없다.")
    @Test
    void createExhibitionWithoutImagePath() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "",
            1024L,
            "빈 이미지 경로 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("운영 시간 요일이 중복되면 전시를 생성할 수 없다.")
    @Test
    void createExhibitionWithDuplicateOperatingDay() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/too-many.webp",
            1024L,
            "운영시간 요일 중복 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            null,
            List.of(
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("이모지로 구성된 100자 제목은 grapheme 기준으로 100자이므로 정상 생성된다.")
    @Test
    void createExhibitionWithEmojiTitleWithinGraphemeLimit() {
        String emoji = "🙂";
        String title = emoji.repeat(100);
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/emoji.webp",
            1024L,
            title,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    @DisplayName("이모지로 구성된 101자 제목은 grapheme 기준 100자를 초과하므로 거부된다.")
    @Test
    void rejectsEmojiTitleExceedingGraphemeLimit() {
        String emoji = "🙂";
        String title = emoji.repeat(101);
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            "exhibitions/emoji.webp",
            1024L,
            title,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .header(API_VERSION_HEADER, API_VERSION_V1)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    private CreateExhibitionRequest createValidRequest() {
        return new CreateExhibitionRequest(
            "exhibitions/default.webp",
            1024L,
            "기본 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "기본 전시 설명입니다.",
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );
    }
}
