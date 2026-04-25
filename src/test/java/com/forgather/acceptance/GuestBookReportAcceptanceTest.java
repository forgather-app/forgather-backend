package com.forgather.acceptance;

import static com.forgather.fixture.GuestBookReportReasonFixture.createReason;
import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.SpaceFixture.createSpace;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReportReason;
import com.forgather.domain.guestbook.model.VisibilityStatus;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.guestbook.repository.jpa.GuestBookReportReasonJpaRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHostMap;
import com.forgather.global.auth.repository.SpaceHostMapRepository;
import com.forgather.global.auth.util.JwtTokenProvider;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
@DisplayName("인수 테스트: 방명록 신고")
public class GuestBookReportAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private SpaceHostMapRepository spaceHostMapRepository;

    @Autowired
    private GuestBookCardRepository guestBookCardRepository;

    @Autowired
    private GuestBookReportReasonJpaRepository reportReasonRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Space space;
    private Host host;
    private GuestBookCard card;
    private GuestBookReportReason reason;
    private String accessToken;
    private String anotherAccessToken;

    @BeforeEach
    void setUp() {
        space = spaceRepository.save(createSpace());

        host = hostRepository.save(createHost());
        Host anotherHost = hostRepository.save(createHost());
        accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        anotherAccessToken = jwtTokenProvider.generateAccessToken(anotherHost.getId());

        spaceHostMapRepository.save(new SpaceHostMap(space, host));

        card = guestBookCardRepository.save(new GuestBookCard(space, "닉네임", "방명록 메시지"));
        reason = reportReasonRepository.save(createReason());

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("방명록 신고")
    @Nested
    class reportGuestBook {

        @DisplayName("호스트가 자신의 방명록을 신고하면 201을 반환한다")
        @Test
        void report() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);

            // when
            CreateGuestBookReportResponse response = RestAssuredMockMvc.given()
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
                .as(CreateGuestBookReportResponse.class);

            // then
            assertThat(response.id()).isNotNull();
            assertThat(response.guestBookCardId()).isEqualTo(card.getId());
        }

        @DisplayName("신고 후 방명록의 상태가 숨김으로 변경된다")
        @Test
        void cardIsHiddenAfterReport() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);

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
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);

            // when & then
            RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
        }

        @DisplayName("스페이스 소유자가 아니면 신고할 수 없다")
        @Test
        void throwExceptionWhenNotSpaceHost() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);

            // when & then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + anotherAccessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        }

        @DisplayName("해당 스페이스의 방명록이 아니면 신고할 수 없다")
        @Test
        void throwExceptionWhenCardNotBelongToSpace() {
            // given
            Space anotherSpace = spaceRepository.save(com.forgather.fixture.SpaceFixture.createSpaceWithCode("ANOTHER123"));
            spaceHostMapRepository.save(new SpaceHostMap(anotherSpace, host));
            GuestBookCard anotherCard = guestBookCardRepository.save(new GuestBookCard(anotherSpace, "nick", "msg"));
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);

            // when & then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), anotherCard.getId())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        }

        @DisplayName("이미 신고된 방명록은 재신고할 수 없다")
        @Test
        void throwExceptionWhenAlreadyReported() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(reason.getId(), null);
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
                .statusCode(HttpStatus.CONFLICT.value());
        }

        @DisplayName("존재하지 않는 신고 사유로는 신고할 수 없다")
        @Test
        void throwExceptionWhenReasonNotFound() {
            // given
            CreateGuestBookReportRequest request = new CreateGuestBookReportRequest(999L, null);

            // when & then
            RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/spaces/{spaceCode}/guestbook/{cardId}/reports",
                    space.getCode(), card.getId())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
        }
    }
}
