package com.forgather.acceptance;

import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.SpaceFixture.createSpace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.guestbook.dto.CreateGuestBookReportRequest;
import com.forgather.domain.guestbook.dto.CreateGuestBookReportResponse;
import com.forgather.domain.guestbook.dto.ReportDetailResponse;
import com.forgather.domain.guestbook.dto.ReportHistoryResponse;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.domain.guestbook.model.VisibilityStatus;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpaceHost;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
@DisplayName("인수 테스트: 방명록 신고")
class GuestbookReportAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Space space;
    private Host host;
    private GuestBookCard card;
    private final GuestBookReportReason reason = GuestBookReportReason.ADVERTISEMENT_SPAM;
    private String accessToken;
    private String anotherAccessToken;

    @BeforeEach
    void setUp() {
        space = spaceRepository.save(createSpace());

        host = hostRepository.save(createHost());
        Host anotherHost = hostRepository.save(createHost());
        accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        anotherAccessToken = jwtTokenProvider.generateAccessToken(anotherHost.getId());

        spaceHostRepository.save(new SpaceHost(space, host));

        card = guestBookCardRepository.save(new GuestBookCard(space, "닉네임", "방명록 메시지"));

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("방명록 신고")
    @Nested
    class reportGuestBook {

        @DisplayName("호스트가 자신의 방명록을 신고하면 201을 반환한다")
        @Test
        void report() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason, null);

            // when
            ApiResponse<CreateGuestBookReportResponse> result = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().id()).isNotNull(),
                () -> assertThat(result.data().guestBookCardId()).isEqualTo(card.getId())
            );
        }

        @DisplayName("신고 후 방명록의 상태가 숨김으로 변경된다")
        @Test
        void cardIsHiddenAfterReport() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason, null);

            // when
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value());

            // then
            GuestBookCard updatedCard = guestBookCardRepository.getByIdAndDeletedAtIsNullOrThrow(card.getId());
            assertThat(updatedCard.getVisibilityStatus()).isEqualTo(VisibilityStatus.HIDDEN_BY_ADMIN);
        }

        @DisplayName("비로그인 사용자는 신고할 수 없다")
        @Test
        void throwExceptionWhenNotLoggedIn() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason, null);

            // when & then
            RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("UNAUTHORIZED"));
        }

        @DisplayName("스페이스 소유자가 아니면 신고할 수 없다")
        @Test
        void throwExceptionWhenNotSpaceHost() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason, null);

            // when & then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("FORBIDDEN"));
        }

        @DisplayName("해당 스페이스의 방명록이 아니면 신고할 수 없다")
        @Test
        void throwExceptionWhenCardNotBelongToSpace() {
            // given
            Space anotherSpace = spaceRepository.save(SpaceFixture.createSpaceWithCode("ANOTHER123"));
            spaceHostRepository.save(new SpaceHost(anotherSpace, host));
            GuestBookCard anotherCard = guestBookCardRepository.save(new GuestBookCard(anotherSpace, "nick", "msg"));
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason, null);

            // when & then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), anotherCard.getId())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("NOT_FOUND"));
        }

        @DisplayName("이미 신고된 방명록은 재신고할 수 없다")
        @Test
        void throwExceptionWhenAlreadyReported() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason, null);
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId());

            // when & then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("CONFLICT"));
        }

        @DisplayName("잘못된 신고 사유로는 신고할 수 없다")
        @Test
        void throwExceptionWhenReasonIsInvalid() {
            // given
            String request = """
                {
                    "reason": "UNKNOWN_REASON",
                    "detail": null
                }
                """;

            // when & then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("BAD_REQUEST"));
        }
    }

    @DisplayName("신고 내역 조회")
    @Nested
    class retrieveReportHistory {

        @DisplayName("신고 내역이 있으면 200과 목록을 반환한다")
        @Test
        void retrieveReportHistory() {
            // given
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(new CreateGuestBookReportRequest(reason, null))
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId());

            // when
            ApiResponse<ReportHistoryResponse> result = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/guestbook/me/reports")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().reportHistory()).hasSize(1),
                () -> assertThat(result.data().totalCount()).isEqualTo(1),
                () -> assertThat(result.data().reportHistory().get(0).nicknameSnapshot()).isEqualTo("닉네임"),
                () -> assertThat(result.data().reportHistory().get(0).messageSnapshot()).isEqualTo("방명록 메시지")
            );
        }

        @DisplayName("신고 내역이 없으면 빈 목록을 반환한다")
        @Test
        void retrieveReportHistoryEmpty() {
            ApiResponse<ReportHistoryResponse> result = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/guestbook/me/reports")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().reportHistory()).isEmpty(),
                () -> assertThat(result.data().totalCount()).isEqualTo(0)
            );
        }

        @DisplayName("비로그인 사용자는 조회할 수 없다")
        @Test
        void throwExceptionWhenNotLoggedIn() {
            RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .when()
                .get("/guestbook/me/reports")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("UNAUTHORIZED"));
        }
    }

    @DisplayName("신고 사유가 null이면 400 예외를 발생한다")
    @Test
    void throwExceptionWhenReasonNull() {
        // given
        CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(null, null);

        // when & then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                space.getCode(), card.getId())
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("code", equalTo("VALIDATION_FAILED"));
    }

    @DisplayName("신고 내역 상세 조회")
    @Nested
    class retrieveReportDetail {

        @DisplayName("신고 내역이 있으면 200과 상세 정보를 반환한다")
        @Test
        void retrieveReportDetail() {
            // given
            String detail = "상세 신고 사유입니다";
            ApiResponse<CreateGuestBookReportResponse> reportResult = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(new CreateGuestBookReportRequest(reason, detail))
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // when
            ApiResponse<ReportDetailResponse> result = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/guestbook/me/reports/{reportId}", reportResult.data().id())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().id()).isEqualTo(reportResult.data().id()),
                () -> assertThat(result.data().space().spaceCode()).isEqualTo(space.getCode()),
                () -> assertThat(result.data().space().name()).isEqualTo(space.getName()),
                () -> assertThat(result.data().reason()).isEqualTo(reason.getLabel()),
                () -> assertThat(result.data().detail()).isEqualTo(detail),
                () -> assertThat(result.data().nicknameSnapshot()).isEqualTo("닉네임"),
                () -> assertThat(result.data().messageSnapshot()).isEqualTo("방명록 메시지"),
                () -> assertThat(result.data().createdAtSnapshot()).isNotNull(),
                () -> assertThat(result.data().createdAt()).isNotNull()
            );
        }

        @DisplayName("존재하지 않는 신고 내역은 조회할 수 없다")
        @Test
        void throwExceptionWhenReportNotFound() {
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/guestbook/me/reports/{reportId}", 999L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @DisplayName("다른 사용자의 신고 내역은 조회할 수 없다")
        @Test
        void throwExceptionWhenOtherHostReport() {
            // given
            ApiResponse<CreateGuestBookReportResponse> reportResult = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(new CreateGuestBookReportRequest(reason, null))
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // when & then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/guestbook/me/reports/{reportId}", reportResult.data().id())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @DisplayName("비로그인 사용자는 상세 조회할 수 없다")
        @Test
        void throwExceptionWhenNotLoggedIn() {
            RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .when()
                .get("/guestbook/me/reports/{reportId}", 1L)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
