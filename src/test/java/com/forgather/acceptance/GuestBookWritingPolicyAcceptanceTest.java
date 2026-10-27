package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.guestbook.repository.jpa.GuestBookCardPhotoJpaRepository;
import com.forgather.domain.guestbook.repository.jpa.GuestBookCardJpaRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.util.JwtTokenProvider;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;

@AutoConfigureMockMvc
class GuestBookWritingPolicyAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private GuestBookCardJpaRepository cardRepository;

    @Autowired
    private GuestBookCardPhotoJpaRepository photoRepository;

    @MockitoBean
    private ContentsStorage contentsStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockedStatic<LocalDate> currentDate;

    private String token;

    @BeforeEach
    void setUpPolicy() {
        LocalDate today = LocalDate.of(2040, 10, 22);
        currentDate = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
        currentDate.when(() -> LocalDate.now(ZoneId.of("Asia/Seoul"))).thenReturn(today);
        Host host = hostRepository.save(HostFixture.createHost());
        token = jwtTokenProvider.generateAccessToken(host.getId());
        when(contentsStorage.getRootDirectory()).thenReturn("images/test/spaces");
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",\"guestBookWritingPolicy\":null", ",\"guestBookWritingPolicy\":\"ALWAYS\""})
    @DisplayName("정책을 생략하거나 null 또는 ALWAYS로 생성하면 항상 방명록 작성이 가능하다")
    void defaultsToAlways(String policy) {
        // given
        String code = createSpace("{\"name\":\"전시\"" + policy + "}");

        // when
        writeCard(code).statusCode(201);

        // then
        detail(code).body("data.guestBookWritingPolicy", equalTo("ALWAYS"))
            .body("data.isGuestBookWritable", equalTo(true));
    }

    @ParameterizedTest
    @CsvSource({"2040-10-20,2040-10-24,201", "2040-10-20,2040-10-21,400"})
    @DisplayName("전시 기간 안에서는 작성을 허용하고 기간 밖에서는 저장 없이 400을 반환한다")
    void restrictsWritingByKoreanDate(String startDate, String endDate, int status) {
        // given
        String code = createSpace("""
            {"name":"전시","guestBookWritingPolicy":"EXHIBITION_PERIOD",
             "exhibitionStartDate":"%s","exhibitionEndDate":"%s"}
            """.formatted(startDate, endDate));
        long cards = cardRepository.count();
        long photos = photoRepository.count();

        // when
        writeCard(code).statusCode(status);

        // then
        detail(code).body("data.guestBookWritingPolicy", equalTo("EXHIBITION_PERIOD"))
            .body("data.isGuestBookWritable", equalTo(status == 201));
        assertThat(cardRepository.count()).isEqualTo(cards + (status == 201 ? 1 : 0));
        assertThat(photoRepository.count()).isEqualTo(photos + (status == 201 ? 1 : 0));
    }

    @Test
    @DisplayName("전시 기간 없이 기간 제한 정책으로 생성하거나 변경하면 400으로 거부한다")
    void rejectsPeriodPolicyWithoutDates() {
        // given
        String code = createSpace("{\"name\":\"전시\"}");

        // when
        createRequest("{\"name\":\"전시\",\"guestBookWritingPolicy\":\"EXHIBITION_PERIOD\"}")
            .statusCode(400);
        update(code, "{\"guestBookWritingPolicy\":\"EXHIBITION_PERIOD\"}").statusCode(400);

        // then
        detail(code).body("data.guestBookWritingPolicy", equalTo("ALWAYS"))
            .body("data.isGuestBookWritable", equalTo(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"guestBookWritingPolicy\":null}"})
    @DisplayName("수정 요청에서 정책을 생략하거나 null이면 기존 기간 제한 정책을 유지한다")
    void preservesOmittedPolicy(String body) {
        // given
        String code = createRestrictedSpace();

        // when
        update(code, body).statusCode(200)
            .body("data.guestBookWritingPolicy", equalTo("EXHIBITION_PERIOD"));

        // then
        detail(code).body("data.guestBookWritingPolicy", equalTo("EXHIBITION_PERIOD"));
    }

    @Test
    @DisplayName("기간 제한 중 기간만 삭제하면 전체 수정을 거부하고 기존 값을 보존한다")
    void rejectsDeletingRestrictedPeriod() {
        // given
        String code = createRestrictedSpace();

        // when
        update(code, "{\"name\":\"변경\",\"isDeleteExhibitionPeriod\":true}").statusCode(400);

        // then
        detail(code).body("data.name", equalTo("전시"))
            .body("data.exhibitionStartDate", equalTo("2040-10-20"))
            .body("data.guestBookWritingPolicy", equalTo("EXHIBITION_PERIOD"));
    }

    @Test
    @DisplayName("기간 삭제와 항상 허용 정책을 함께 요청하면 두 값을 모두 변경한다")
    void deletesPeriodTogetherWithAlwaysPolicy() {
        // given
        String code = createRestrictedSpace();

        // when
        update(code, "{\"isDeleteExhibitionPeriod\":true,\"guestBookWritingPolicy\":\"ALWAYS\"}")
            .statusCode(200);

        // then
        detail(code).body("data.exhibitionStartDate", nullValue())
            .body("data.exhibitionEndDate", nullValue())
            .body("data.guestBookWritingPolicy", equalTo("ALWAYS"));
        writeCard(code).statusCode(201);
    }

    @Test
    @DisplayName("기간과 제한 정책을 함께 설정할 수 있고 날짜 쌍은 삭제 플래그보다 우선한다")
    void updatesPeriodAndPolicyTogether() {
        // given
        String code = createSpace("{\"name\":\"전시\"}");

        // when
        update(code, """
            {"guestBookWritingPolicy":"EXHIBITION_PERIOD","isDeleteExhibitionPeriod":true,
             "exhibitionStartDate":"2040-10-22","exhibitionEndDate":"2040-10-22"}
            """).statusCode(200);

        // then
        detail(code).body("data.exhibitionStartDate", equalTo("2040-10-22"))
            .body("data.guestBookWritingPolicy", equalTo("EXHIBITION_PERIOD"));
        writeCard(code).statusCode(201);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISABLED", "UNKNOWN"})
    @DisplayName("지원하지 않는 정책은 생성과 수정에서 400으로 거부한다")
    void rejectsUnsupportedPolicy(String policy) {
        // given
        String code = createSpace("{\"name\":\"전시\"}");

        // when
        createRequest("{\"name\":\"전시\",\"guestBookWritingPolicy\":\"" + policy + "\"}").statusCode(400);
        update(code, "{\"guestBookWritingPolicy\":\"" + policy + "\"}").statusCode(400);

        // then
        detail(code).body("data.guestBookWritingPolicy", equalTo("ALWAYS"))
            .body("data.isGuestBookWritable", equalTo(true));
    }

    @Test
    @DisplayName("정책 컬럼을 생략한 기존 방식의 데이터도 기본 정책으로 방명록을 작성할 수 있다")
    void appliesDatabaseDefaultToLegacyInsert() {
        // given
        jdbcTemplate.update("""
            INSERT INTO space (code, name, description, is_public, link_url, link_name)
            VALUES ('legacy0001', '전시', '', true, '', '')
            """);

        // when
        writeCard("legacy0001").statusCode(201);

        // then
        assertThat(jdbcTemplate.queryForObject(
            "SELECT guest_book_writing_policy FROM space WHERE code = 'legacy0001'", String.class))
            .isEqualTo("ALWAYS");
    }

    @Test
    @DisplayName("조회 후 종료일이 지나면 작성 가능 여부를 다시 계산하고 작성을 거절한다")
    void rechecksWritingAfterExhibitionEnds() {
        // given
        String code = createRestrictedSpace();
        detail(code).body("data.isGuestBookWritable", equalTo(true));

        // when
        LocalDate nextDay = LocalDate.of(2040, 10, 23);
        currentDate.when(() -> LocalDate.now(ZoneId.of("Asia/Seoul"))).thenReturn(nextDay);

        // then
        detail(code).body("data.isGuestBookWritable", equalTo(false));
        writeCard(code).statusCode(400);
    }

    private String createRestrictedSpace() {
        return createSpace("""
            {"name":"전시","guestBookWritingPolicy":"EXHIBITION_PERIOD",
             "exhibitionStartDate":"2040-10-20","exhibitionEndDate":"2040-10-22"}
            """);
    }

    private String createSpace(String body) {
        return createRequest(body).statusCode(201).extract().path("data.spaceCode");
    }

    private ValidatableMockMvcResponse createRequest(String body) {
        return RestAssuredMockMvc.given().postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON).body(body).when().post("/spaces").then();
    }

    private ValidatableMockMvcResponse update(String code, String body) {
        return RestAssuredMockMvc.given().postProcessors(withAccessToken(token))
            .contentType(ContentType.JSON).body(body).when().patch("/spaces/{code}", code).then();
    }

    private ValidatableMockMvcResponse detail(String code) {
        return RestAssuredMockMvc.given().when().get("/spaces/{code}", code).then().statusCode(200);
    }

    private ValidatableMockMvcResponse writeCard(String code) {
        return RestAssuredMockMvc.given().contentType(ContentType.JSON)
            .body("""
                {"nickname":"방문자","message":"축하합니다","photos":[
                 {"uploadFileName":"photo.jpg","originalName":"original.jpg","capacity":1024}]}
                """)
            .when().post("/spaces/{code}/guestbook", code).then();
    }

    @AfterEach
    void tearDownCurrentDate() {
        if (currentDate != null) {
            currentDate.close();
        }
    }
}
