package com.forgather.acceptance;

import static com.forgather.domain.guestbook.model.GuestBookReportReason.ADVERTISEMENT_SPAM;
import static com.forgather.domain.guestbook.model.VisibilityStatus.HIDDEN_BY_ADMIN;
import static com.forgather.domain.guestbook.model.VisibilityStatus.HIDDEN_BY_HOST;
import static com.forgather.fixture.GuestBookCardFixture.createWithSpaceAndVisibilityStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.guestbook.dto.CreateGuestBookReportRequest;
import com.forgather.domain.guestbook.dto.DeleteGuestBookCardPhotosRequest;
import com.forgather.domain.guestbook.dto.GuestBookCardResponse;
import com.forgather.domain.guestbook.dto.GuestBookResponse;
import com.forgather.domain.guestbook.dto.WriteGuestBookCardPhotoRequest;
import com.forgather.domain.guestbook.dto.WriteGuestBookCardRequest;
import com.forgather.domain.guestbook.dto.WriteGuestBookCardResponse;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.AwsS3Cloud;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
class GuestBookCardAcceptanceTest extends AcceptanceTest {

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

    @MockitoBean
    private AwsS3Cloud awsS3Cloud;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Space publicSpace;
    private Space privateSpace;
    private String accessToken;
    private String anotherAccessToken;
    private WriteGuestBookCardRequest writeRequest = new WriteGuestBookCardRequest(
        "nickname",
        "message",
        List.of(
            new WriteGuestBookCardPhotoRequest("photo1.jpg", "abc.jpg", 1024L),
            new WriteGuestBookCardPhotoRequest("photo2.jpg", "def.jpg", 2048L),
            new WriteGuestBookCardPhotoRequest("photo3.jpg", "ghi.jpg", 4096L)
        )
    );

    @BeforeEach
    void setUp() {
        publicSpace = SpaceFixture.createSpace();
        privateSpace = SpaceFixture.createPrivateSpace();
        spaceRepository.save(publicSpace);
        spaceRepository.save(privateSpace);

        Host host = HostFixture.createHost();
        Host anotherHost = HostFixture.createHost();
        hostRepository.save(host);
        hostRepository.save(anotherHost);
        accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        anotherAccessToken = jwtTokenProvider.generateAccessToken(anotherHost.getId());

        spaceHostRepository.save(new SpaceHost(publicSpace, host));
        spaceHostRepository.save(new SpaceHost(privateSpace, host));

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("방명록 조회")
    @Nested
    class readGuestBook {
        @DisplayName("공개 스페이스인 경우 방문자도 방명록을 조회할 수 있다")
        @Test
        void guestCanReadGuestBookInPublicSpace() {
            // given
            writeGuestBookCard(publicSpace);
            writeGuestBookCard(publicSpace);

            // when
            ApiResponse<GuestBookResponse> result = RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().guestBookCards()).size().isEqualTo(2),
                () -> assertThat(result.data().currentPage()).isEqualTo(1),
                () -> assertThat(result.data().pageSize()).isEqualTo(15),
                () -> assertThat(result.data().totalCount()).isEqualTo(2),
                () -> assertThat(result.data().totalPages()).isEqualTo(1)
            );
        }

        @DisplayName("방문자가 공개 스페이스의 방명록을 조회할 경우 읽지 않은 방명록 개수는 알지 못한다")
        @Test
        void guestCannotKnowUnreadCount() {
            // given
            writeGuestBookCard(publicSpace);

            // when
            String response = RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

            // then
            assertAll(
                () -> assertThat(response).doesNotContain("\"unreadCount\"")
            );
        }

        @DisplayName("방문자 조회 시 방명록은 각 방명록 카드의 방문자 닉네임과 사진 여부를 포함한다")
        @Test
        void guestBookContainsNicknameAndPhoto() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);
            WriteGuestBookCardResponse writeResponseWithNoPhoto = writeGuestBookCardWithNoPhoto(publicSpace);

            // when
            ApiResponse<GuestBookResponse> result = RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().guestBookCards()).size().isEqualTo(2),
                () -> assertThat(result.data().guestBookCards().getFirst().nickname()).isEqualTo(
                    writeResponseWithNoPhoto.nickname()),
                () -> assertThat(result.data().guestBookCards().getFirst().containsPhoto()).isFalse(),
                () -> assertThat(result.data().guestBookCards().getLast().nickname()).isEqualTo(
                    writeResponse.nickname()),
                () -> assertThat(result.data().guestBookCards().getLast().containsPhoto()).isTrue(),
                () -> assertThat(result.data().currentPage()).isEqualTo(1),
                () -> assertThat(result.data().pageSize()).isEqualTo(15),
                () -> assertThat(result.data().totalCount()).isEqualTo(2),
                () -> assertThat(result.data().totalPages()).isEqualTo(1)
            );
        }

        @DisplayName("방명록 목록 조회 시 숨김 처리된 방명록은 제외된다")
        @Test
        void readGuestBookExcludesHiddenCards() {
            // given
            WriteGuestBookCardResponse hiddenCard = writeGuestBookCard(publicSpace);
            WriteGuestBookCardResponse visibleCard = writeGuestBookCardWithNoPhoto(publicSpace);
            reportGuestBookCard(publicSpace, hiddenCard.id());

            // when
            ApiResponse<GuestBookResponse> result = RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().guestBookCards()).size().isEqualTo(1),
                () -> assertThat(result.data().guestBookCards().getFirst().id()).isEqualTo(visibleCard.id()),
                () -> assertThat(result.data().totalCount()).isEqualTo(1),
                () -> assertThat(result.data().totalPages()).isEqualTo(1)
            );
        }

        @DisplayName("비공개 스페이스인 경우 방문자는 방명록을 조회할 수 없다")
        @Test
        void throwExceptionWhenGuestReadGuestBookInPrivateSpace() {
            // when, then
            RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(privateSpace.getCode()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("방문자는 비공개 스페이스의 방명록을 조회할 수 없습니다."));
        }

        @DisplayName("호스트는 자신의 비공개 스페이스 방명록을 조회할 수 있다")
        @Test
        void hostCanReadGuestBookInPrivateSpace() {
            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(privateSpace.getCode()))
                .then()
                .statusCode(200);
        }

        @DisplayName("다른 호스트의 비공개 스페이스 방명록을 조회하면 예외를 던진다")
        @Test
        void throwExceptionWhenAnotherHostReadGuestBookInPrivateSpace() {
            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(privateSpace.getCode()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("방문자는 비공개 스페이스의 방명록을 조회할 수 없습니다."));
        }

        @DisplayName("호스트가 기본 버전 방명록을 조회할 경우 전체 방명록을 반환하고 읽지 않은 방명록 수를 응답하지 않는다")
        @Test
        void hostReadGuestBookWithDefaultVersionKeepsLegacyResponse() {
            // given
            WriteGuestBookCardResponse readCard = writeGuestBookCard(publicSpace);
            WriteGuestBookCardResponse unreadCard = writeGuestBookCardWithNoPhoto(publicSpace);
            readGuestBookCardAsHost(publicSpace, readCard.id());

            // when, then
            ApiResponse<GuestBookResponse> result = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.data().guestBookCards()).hasSize(2),
                () -> assertThat(result.data().guestBookCards().getFirst().id()).isEqualTo(unreadCard.id()),
                () -> assertThat(result.data().guestBookCards().getFirst().isRead()).isFalse(),
                () -> assertThat(result.data().guestBookCards().getLast().id()).isEqualTo(readCard.id()),
                () -> assertThat(result.data().guestBookCards().getLast().isRead()).isTrue(),
                () -> assertThat(result.data().totalCount()).isEqualTo(2)
            );
        }

        @DisplayName("방명록 조회 v2 응답의 각 방명록 카드는 메세지와 생성 시각을 포함한다")
        @Test
        void guestBookCardContainsMessageAndCreatedAtInV2() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);

            // when
            ApiResponse<GuestBookResponse> result = RestAssuredMockMvc.given()
                .header("X-API-Version", "2")
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.data().guestBookCards()).hasSize(1),
                () -> assertThat(result.data().guestBookCards().getFirst().id()).isEqualTo(writeResponse.id()),
                () -> assertThat(result.data().guestBookCards().getFirst().message()).isEqualTo(
                    writeRequest.message()),
                () -> assertThat(result.data().guestBookCards().getFirst().createdAt()).isBetween(
                    LocalDateTime.now().minusMinutes(1), LocalDateTime.now())
            );
        }

        @DisplayName("호스트는 읽지 않은 방명록 목록을 조회할 수 있다")
        @Test
        void hostCanReadUnreadGuestBookCards() {
            // given
            WriteGuestBookCardResponse readCard = writeGuestBookCard(publicSpace);
            WriteGuestBookCardResponse unreadCard = writeGuestBookCardWithNoPhoto(publicSpace);
            readGuestBookCardAsHost(publicSpace, readCard.id());

            // when
            ApiResponse<GuestBookResponse> result = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook/unread".formatted(publicSpace.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.data().unreadCount()).isNull(),
                () -> assertThat(result.data().guestBookCards()).hasSize(1),
                () -> assertThat(result.data().guestBookCards().getFirst().id()).isEqualTo(unreadCard.id()),
                () -> assertThat(result.data().guestBookCards().getFirst().message()).isEqualTo("message2"),
                () -> assertThat(result.data().guestBookCards().getFirst().createdAt()).isNotNull(),
                () -> assertThat(result.data().guestBookCards().getFirst().isRead()).isNull(),
                () -> assertThat(result.data().totalCount()).isOne()
            );
        }
    }

    @DisplayName("방명록 카드 조회")
    @Nested
    class readGuestBookCard {
        @DisplayName("공개 스페이스인 경우 방문자는 방명록 카드를 조회할 수 있다")
        @Test
        void guestCanReadCardInPublicSpace() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);

            // when
            ApiResponse<GuestBookCardResponse> result = RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().id()).isNotNull(),
                () -> assertThat(result.data().nickname()).isEqualTo(writeRequest.nickname()),
                () -> assertThat(result.data().message()).isEqualTo(writeRequest.message()),
                () -> assertThat(result.data().createdAt()).isBetween(LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now()),

                () -> assertThat(result.data().photos().get(0).originalName()).isEqualTo("photo1.jpg"),
                () -> assertThat(result.data().photos().get(0).path()).endsWith("/spaces/1234567890/guestbook/abc.jpg"),

                () -> assertThat(result.data().photos().get(1).originalName()).isEqualTo("photo2.jpg"),
                () -> assertThat(result.data().photos().get(1).path()).endsWith("/spaces/1234567890/guestbook/def.jpg"),

                () -> assertThat(result.data().photos().get(2).originalName()).isEqualTo("photo3.jpg"),
                () -> assertThat(result.data().photos().get(2).path()).endsWith("/spaces/1234567890/guestbook/ghi.jpg")
            );
        }

        @DisplayName("비공개 스페이스인 경우 방문자는 방명록 카드를 조회할 수 없다")
        @Test
        void throwExceptionWhenGuestReadCardInPrivateSpace() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(privateSpace);

            // when, then
            RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(privateSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("방문자는 비공개 스페이스의 방명록을 조회할 수 없습니다."));
        }

        @DisplayName("다른 호스트의 비공개 스페이스 방명록 카드를 조회할 수 없다")
        @Test
        void throwExceptionWhenAnotherHostReadCardInPrivateSpace() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(privateSpace);

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(privateSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("방문자는 비공개 스페이스의 방명록을 조회할 수 없습니다."));
        }

        @DisplayName("호스트는 비공개 스페이스의 방명록 카드를 조회할 수 있다")
        @Test
        void hostCanReadCardInPrivateSpace() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(privateSpace);

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(privateSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(200);
        }

        @DisplayName("스페이스 호스트가 숨김 처리한 방명록 카드는 스페이스 호스트가 조회할 수 있다")
        @Test
        void hostCanReadHiddenByHostCard() {
            // given
            GuestBookCard guestBookCard = createWithSpaceAndVisibilityStatus(publicSpace, HIDDEN_BY_HOST);
            guestBookCardRepository.save(guestBookCard);

            // when
            ApiResponse<GuestBookCardResponse> result = RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), guestBookCard.getId()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });

            // then
            assertAll(
                () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(result.message()).isNull(),
                () -> assertThat(result.data().id()).isEqualTo(guestBookCard.getId())
            );
        }

        @DisplayName("스페이스 호스트가 숨김 처리한 방명록 카드는 방문자가 조회할 수 없다")
        @Test
        void guestCannotReadHiddenByHostCard() {
            // given
            GuestBookCard guestBookCard = createWithSpaceAndVisibilityStatus(publicSpace, HIDDEN_BY_HOST);
            guestBookCardRepository.save(guestBookCard);

            // when, then
            RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), guestBookCard.getId()))
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", containsString("존재하지 않는 방명록 카드입니다."));
        }

        @DisplayName("관리자가 숨김 처리한 방명록 카드는 스페이스 호스트도 조회할 수 없다")
        @Test
        void hostCannotReadHiddenByAdminCard() {
            // given
            GuestBookCard guestBookCard = createWithSpaceAndVisibilityStatus(publicSpace, HIDDEN_BY_ADMIN);
            guestBookCardRepository.save(guestBookCard);

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), guestBookCard.getId()))
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", containsString("존재하지 않는 방명록 카드입니다."));
        }

        @DisplayName("호스트가 방명록 카드를 조회하면 읽음 처리된다")
        @Test
        void markCardAsReadWhenHostRead() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);

            // when
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(200);

            // then
            String response = RestAssuredMockMvc.given()
                .header("X-API-Version", "2")
                .header("Authorization", "Bearer " + accessToken)
                .accept(ContentType.JSON)
                .queryParam("page", 1)
                .queryParam("size", 15)
                .queryParam("sort", "createdAt,desc")
                .queryParam("sort", "id,desc")
                .when()
                .get("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

            assertAll(
                () -> assertThat(response).contains("\"code\":\"SUCCESS\""),
                () -> assertThat(response).contains("\"unreadCount\":0"),
                () -> assertThat(response).doesNotContain("\"newCount\""),
                () -> assertThat(response).contains("\"id\":%d".formatted(writeResponse.id()))
            );
        }
    }

    @DisplayName("방명록 카드 작성")
    @Nested
    class writeGuestBookCard {
        @DisplayName("방명록 카드 작성")
        @Test
        void write() {
            // when
            WriteGuestBookCardResponse result = writeGuestBookCard(publicSpace);

            // then
            assertAll(
                () -> assertThat(result.id()).isNotNull(),
                () -> assertThat(result.nickname()).isEqualTo(writeRequest.nickname()),
                () -> assertThat(result.message()).isEqualTo(writeRequest.message()),
                () -> assertThat(result.isRead()).isFalse(),
                () -> assertThat(result.createdAt()).isBetween(LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now()),

                () -> assertThat(result.photos().get(0).originalName()).isEqualTo("photo1.jpg"),
                () -> assertThat(result.photos().get(0).path()).endsWith(
                    "/spaces/%s/guestbook/abc.jpg".formatted(publicSpace.getCode())),

                () -> assertThat(result.photos().get(1).originalName()).isEqualTo("photo2.jpg"),
                () -> assertThat(result.photos().get(1).path()).endsWith(
                    "/spaces/%s/guestbook/def.jpg".formatted(publicSpace.getCode())),

                () -> assertThat(result.photos().get(2).originalName()).isEqualTo("photo3.jpg"),
                () -> assertThat(result.photos().get(2).path()).endsWith(
                    "/spaces/%s/guestbook/ghi.jpg".formatted(publicSpace.getCode()))
            );
        }

        @DisplayName("방문자 닉네임이 10자를 초과하면 예외를 던진다")
        @Test
        void throwExceptionWhenNicknameExceedMaxLength() {
            // given
            WriteGuestBookCardRequest request = new WriteGuestBookCardRequest(
                "12345678901",
                "message",
                List.of(
                    new WriteGuestBookCardPhotoRequest("photo1.jpg", "abc.jpg", 1024L),
                    new WriteGuestBookCardPhotoRequest("photo2.jpg", "def.jpg", 2048L),
                    new WriteGuestBookCardPhotoRequest("photo3.jpg", "ghi.jpg", 4096L)
                )
            );

            // when, then
            RestAssuredMockMvc.given()
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_FAILED"));
        }

        @DisplayName("방명록 카드 사진이 20개를 초과하면 예외를 던진다")
        @Test
        void throwExceptionWhenPhotoExceedMaxSize() {
            // given
            List<WriteGuestBookCardPhotoRequest> photos = IntStream.range(0, 21)
                .mapToObj(i -> new WriteGuestBookCardPhotoRequest("photo.jpg", "abc.jpg", 1024L))
                .toList();
            WriteGuestBookCardRequest request = new WriteGuestBookCardRequest(
                "nickname",
                "message",
                photos
            );

            // when, then
            RestAssuredMockMvc.given()
                .body(request)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/spaces/%s/guestbook".formatted(publicSpace.getCode()))
                .then()
                .statusCode(400)
                .body("code", equalTo("BAD_REQUEST"))
                .body("message", containsString("방명록 카드 사진은 최대"));
        }
    }

    @DisplayName("방명록 카드 삭제")
    @Nested
    class deleteGuestBookCard {
        @DisplayName("방명록 카드를 삭제한다")
        @Test
        void deleteCard() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);

            // when
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(204);

            // then
            RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
        }

        @DisplayName("방문자는 방명록 카드를 삭제하지 못한다")
        @Test
        void throwExceptionWhenGuestDeleteCard() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);

            // when, then
            RestAssuredMockMvc.given()
                .when()
                .delete("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("message", containsString("로그인이 필요합니다."));
        }

        @DisplayName("다른 호스트의 스페이스에 속한 방명록 카드를 삭제하지 못한다")
        @Test
        void throwExceptionWhenAnotherHostDeleteCard() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .when()
                .delete("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("해당 스페이스에 대한 접근 권한이 없습니다."));
        }

        @DisplayName("다른 스페이스에 속한 방명록 카드를 삭제하지 못한다")
        @Test
        void throwExceptionWhenDeleteCardOnAnotherSpace() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);

            // when, then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/spaces/%s/guestbook/%d".formatted(privateSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(404)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", containsString("존재하지 않는 방명록 카드입니다."));
        }
    }

    @DisplayName("방명록 카드 사진 삭제")
    @Nested
    class deleteGuestBookCardPhotos {
        @DisplayName("방명록 카드 사진을 일부 삭제한다")
        @Test
        void deleteCardPhotos() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);
            DeleteGuestBookCardPhotosRequest request = new DeleteGuestBookCardPhotosRequest(
                List.of(
                    writeResponse.photos().get(0).id(),
                    writeResponse.photos().get(2).id()
                )
            );

            // when
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .delete("/spaces/%s/guestbook/%d/photos".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(204);

            // then
            ApiResponse<GuestBookCardResponse> response = RestAssuredMockMvc.given()
                .accept(ContentType.JSON)
                .when()
                .get("/spaces/%s/guestbook/%d".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<>() {
                });
            assertAll(
                () -> assertThat(response.code()).isEqualTo(ResponseCode.SUCCESS),
                () -> assertThat(response.message()).isNull(),
                () -> assertThat(response.data().photos()).size().isEqualTo(1),
                () -> assertThat(response.data().photos().getFirst().id()).isEqualTo(writeResponse.photos().get(1).id())
            );
        }

        @DisplayName("방문자가 방명록 카드 사진을 삭제하면 예외를 던진다")
        @Test
        void throwExceptionWhenGuestDeleteCardPhotos() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);
            DeleteGuestBookCardPhotosRequest request = new DeleteGuestBookCardPhotosRequest(
                List.of(
                    writeResponse.photos().get(0).id(),
                    writeResponse.photos().get(2).id()
                )
            );

            // when
            RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .delete("/spaces/%s/guestbook/%d/photos".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(401)
                .body("code", equalTo("UNAUTHORIZED"))
                .body("message", containsString("로그인이 필요합니다."));
        }

        @DisplayName("다른 호스트의 방명록 카드 사진을 삭제하면 예외를 던진다")
        @Test
        void throwExceptionWhenAnotherHostDeleteCardPhotos() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);
            DeleteGuestBookCardPhotosRequest request = new DeleteGuestBookCardPhotosRequest(
                List.of(
                    writeResponse.photos().get(0).id(),
                    writeResponse.photos().get(2).id()
                )
            );

            // when
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .delete("/spaces/%s/guestbook/%d/photos".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(403)
                .body("code", equalTo("FORBIDDEN"))
                .body("message", containsString("해당 스페이스에 대한 접근 권한이 없습니다."));
        }

        @DisplayName("다른 방명록 카드의 사진을 삭제하면 예외를 던진다")
        @Test
        void throwExceptionWhenDeletePhotosInAnotherCard() {
            // given
            WriteGuestBookCardResponse writeResponse = writeGuestBookCard(publicSpace);
            DeleteGuestBookCardPhotosRequest request = new DeleteGuestBookCardPhotosRequest(
                List.of(
                    writeResponse.photos().get(2).id() + 1
                )
            );

            // when
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .delete("/spaces/%s/guestbook/%d/photos".formatted(publicSpace.getCode(), writeResponse.id()))
                .then()
                .statusCode(400)
                .body("code", equalTo("BAD_REQUEST"))
                .body("message", containsString("해당 방명록 카드에 존재하지 않는 사진입니다."));
        }
    }

    private WriteGuestBookCardResponse writeGuestBookCard(Space space) {
        ApiResponse<WriteGuestBookCardResponse> response = RestAssuredMockMvc.given()
            .body(writeRequest)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .when()
            .post("/spaces/%s/guestbook".formatted(space.getCode()))
            .then()
            .statusCode(201)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        return response.data();
    }

    private WriteGuestBookCardResponse writeGuestBookCardWithNoPhoto(Space space) {
        WriteGuestBookCardRequest writeRequestWithNoPicture = new WriteGuestBookCardRequest(
            "nickname2",
            "message2",
            List.of()
        );
        ApiResponse<WriteGuestBookCardResponse> response = RestAssuredMockMvc.given()
            .body(writeRequestWithNoPicture)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .when()
            .post("/spaces/%s/guestbook".formatted(space.getCode()))
            .then()
            .statusCode(201)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        return response.data();
    }

    private void reportGuestBookCard(Space space, Long guestBookCardId) {
        CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(ADVERTISEMENT_SPAM, null);

        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/spaces/{spaceCode}/guestbook/{guestBookCardId}/reports", space.getCode(), guestBookCardId)
            .then()
            .statusCode(201);
    }

    private void readGuestBookCardAsHost(Space space, Long guestBookCardId) {
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + accessToken)
            .accept(ContentType.JSON)
            .when()
            .get("/spaces/%s/guestbook/%d".formatted(space.getCode(), guestBookCardId))
            .then()
            .statusCode(200);
    }
}
