package com.forgather.acceptance;

import static com.forgather.fixture.HostFixture.createHost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.exhibition.dto.CreateExhibitionPhotoRequest;
import com.forgather.domain.exhibition.dto.CreateExhibitionRequest;
import com.forgather.domain.exhibition.dto.ExhibitionResponse;
import com.forgather.domain.exhibition.dto.LocationRequest;
import com.forgather.domain.exhibition.dto.OperatingHourRequest;
import com.forgather.domain.exhibition.model.LocationType;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.fixture.HostFixture;
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

        Host newHost = new Host(HostFixture.randomCode(), "카카오원본이름", "posty@forgather.app");
        newHost.updateNickname("서비스닉네임");
        host = hostRepository.save(newHost);
        token = jwtTokenProvider.generateAccessToken(host.getId());
    }

    @DisplayName("운영 시간과 함께 온라인 전시를 생성한다.")
    @Test
    void createOnlineExhibition() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("spring.webp", 1024L),
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
            .postProcessors(withAccessToken(token))
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
            () -> assertThat(response.data().photoPath())
                .isEqualTo("ROOT_DIRECTORY_PLACEHOLDER/exhibitions/spring.webp"),
            () -> assertThat(response.data().location().locationType()).isEqualTo(LocationType.ONLINE),
            () -> assertThat(response.data().location().url()).isEqualTo("https://forgather.app"),
            () -> assertThat(response.data().operatingHours()).hasSize(2),
            () -> assertThat(response.data().creator().id()).isEqualTo(host.getId()),
            () -> assertThat(response.data().creator().name()).isEqualTo(host.getNickname())
        );
    }

    @DisplayName("운영 시간 없이 오프라인 전시를 생성한다.")
    @Test
    void createOfflineExhibitionWithoutOperatingHours() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("summer.webp", 2048L),
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
            .postProcessors(withAccessToken(token))
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

    @DisplayName("전시 사진 없이 전시를 생성하면 대표 이미지 경로는 null이다.")
    @Test
    void createExhibitionWithoutPhoto() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            null,
            "사진 없는 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "사진 없이 생성한 전시입니다.",
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when
        ApiResponse<ExhibitionResponse> response = RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
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
            () -> assertThat(response.data().id()).isNotNull(),
            () -> assertThat(response.data().title()).isEqualTo("사진 없는 전시"),
            () -> assertThat(response.data().photoPath()).isNull()
        );
    }

    @DisplayName("운영 시간 응답은 MONDAY..SUNDAY 순으로 정렬된다.")
    @Test
    void operatingHoursSortedByDayOfWeek() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("sort.webp", 1024L),
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
            .postProcessors(withAccessToken(token))
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
        assertThat(response.data().operatingHours())
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
            new CreateExhibitionPhotoRequest("empty-title.webp", 1024L),
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
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("업로드 파일명이 비어 있으면 전시를 생성할 수 없다.")
    @Test
    void createExhibitionWithoutUploadFileName() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("", 1024L),
            "빈 업로드 파일명 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
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
            new CreateExhibitionPhotoRequest("dup.webp", 1024L),
            "운영시간 요일 중복 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            null,
            List.of(
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(21, 0))
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
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
            new CreateExhibitionPhotoRequest("emoji.webp", 1024L),
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
            .postProcessors(withAccessToken(token))
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
            new CreateExhibitionPhotoRequest("emoji.webp", 1024L),
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
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("운영 시간 항목이 7개를 초과하면 전시를 생성할 수 없다.")
    @Test
    void createExhibitionWithTooManyOperatingHours() {
        // given - 고유 요일 7개 + MONDAY 1개 추가로 8개 입력 (Bean Validation의 @Size 위반)
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("too-many.webp", 1024L),
            "운영시간 초과 전시",
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
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"))
            .body("message", containsString("운영시간은 최대 7개"));
    }

    @DisplayName("운영 시간 리스트에 null 원소가 포함되면 전시를 생성할 수 없다.")
    @Test
    void createExhibitionWithNullOperatingHourElement() {
        // given
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("null-element.webp", 1024L),
            "null 원소 포함 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            null,
            Arrays.asList(
                new OperatingHourRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)),
                null
            ),
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"))
            .body("message", containsString("운영시간 항목은 null일 수 없습니다"));
    }

    @DisplayName("전시 제목이 100자를 초과하면 전시를 생성할 수 없다.")
    @Test
    void rejectsTitleExceedingMaxLength() {
        // given
        String title = "가".repeat(101);
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("long-title.webp", 1024L),
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
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("전시 소개가 200자를 초과하면 전시를 생성할 수 없다.")
    @Test
    void rejectsDescriptionExceedingMaxLength() {
        // given
        String description = "가".repeat(201);
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("long-desc.webp", 1024L),
            "긴 소개 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            description,
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("운영 공지가 200자를 초과하면 전시를 생성할 수 없다.")
    @Test
    void rejectsOperationNoticeExceedingMaxLength() {
        // given
        String operationNotice = "가".repeat(201);
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("long-notice.webp", 1024L),
            "긴 운영 공지 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            operationNotice,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("전시 소개가 정확히 200자이면 전시가 정상 생성된다.")
    @Test
    void createExhibitionWithDescriptionAtMaxLength() {
        // given
        String description = "가".repeat(200);
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("max-desc.webp", 1024L),
            "최대 길이 소개 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            description,
            null,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    @DisplayName("운영 공지가 정확히 200자이면 전시가 정상 생성된다.")
    @Test
    void createExhibitionWithOperationNoticeAtMaxLength() {
        // given
        String operationNotice = "가".repeat(200);
        CreateExhibitionRequest request = new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("max-notice.webp", 1024L),
            "최대 길이 운영 공지 전시",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            "전시 설명",
            operationNotice,
            null,
            new LocationRequest(LocationType.ONLINE, "https://forgather.app", null, null)
        );

        // when & then
        RestAssuredMockMvc.given()
            .postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions")
            .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    private CreateExhibitionRequest createValidRequest() {
        return new CreateExhibitionRequest(
            new CreateExhibitionPhotoRequest("default.webp", 1024L),
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
