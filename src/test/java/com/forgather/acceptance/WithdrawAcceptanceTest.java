package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.host.model.Host;
import com.forgather.domain.host.model.KakaoHost;
import com.forgather.domain.host.repository.HostRepository;
import com.forgather.domain.host.repository.KakaoHostRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.SocialRevokeFailLog;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SocialRevokeFailLogRepository;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.external.social.SocialProvider;
import com.forgather.global.response.ResponseCode;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.response.ExtractableResponse;

@DisplayName("인수 테스트: 회원 탈퇴")
@AutoConfigureMockMvc
class WithdrawAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private KakaoHostRepository kakaoHostRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    @Autowired
    private SocialRevokeFailLogRepository socialRevokeFailLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    private ExtractableResponse<MockMvcResponse> withdraw(String token) {
        return RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .delete("/auth/me")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();
    }

    @DisplayName("탈퇴하면 계정과 소유 공간이 논리 삭제되고 소셜 매핑은 물리 삭제된다")
    @Test
    void withdrawDeletesHostAndOwnedContents() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        kakaoHostRepository.save(new KakaoHost(host, "kakao-user-1"));
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        spaceHostRepository.save(new SpaceHost(space, host));
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = withdraw(token);

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(hostRepository.findByIdAndDeletedAtIsNull(host.getId())).isEmpty(),
            () -> assertThat(kakaoHostRepository.findByUserId("kakao-user-1")).isEmpty(),
            () -> assertThat(spaceRepository.findByCodeAndDeletedAtIsNull(space.getCode())).isEmpty()
        );
    }

    @DisplayName("탈퇴 성공 시 인증 쿠키를 만료시킨다")
    @Test
    void withdrawExpiresAuthCookies() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = withdraw(token);

        // then
        List<String> setCookies = response.headers().getValues(HttpHeaders.SET_COOKIE);
        assertAll(
            () -> assertThat(setCookies).anyMatch(cookie ->
                cookie.startsWith("access_token=") && cookie.contains("Max-Age=0")),
            () -> assertThat(setCookies).anyMatch(cookie ->
                cookie.startsWith("refresh_token=") && cookie.contains("Max-Age=0"))
        );
    }

    @DisplayName("Kakao unlink에 실패해도 탈퇴는 완료되고 재시도용 실패 로그가 남는다")
    @Test
    void withdrawSucceedsEvenIfUnlinkFails() {
        // given: 테스트 환경에는 kakao unlink 설정이 없어 unlink 호출이 항상 실패한다
        Host host = hostRepository.save(HostFixture.createHost());
        kakaoHostRepository.save(new KakaoHost(host, "kakao-user-1"));
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        withdraw(token);

        // then
        List<SocialRevokeFailLog> failLogs = socialRevokeFailLogRepository.findAllByCompletedAtIsNull();
        assertAll(
            () -> assertThat(hostRepository.findByIdAndDeletedAtIsNull(host.getId())).isEmpty(),
            () -> assertThat(failLogs).hasSize(1),
            () -> assertThat(failLogs.get(0).getProvider()).isEqualTo(SocialProvider.KAKAO),
            () -> assertThat(failLogs.get(0).getSocialUserId()).isEqualTo("kakao-user-1")
        );
    }

    @DisplayName("탈퇴한 회원의 기존 액세스 토큰으로 접근하면 401을 반환한다")
    @Test
    void rejectWithdrawnHostAccessToken() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        withdraw(token);

        // when & then
        RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .get("/auth/me")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("탈퇴한 회원의 리프레시 토큰으로 세션을 갱신하면 401을 반환한다")
    @Test
    void rejectWithdrawnHostRefreshToken() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String accessToken = jwtTokenProvider.generateAccessToken(host.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(host.getId());
        withdraw(accessToken);

        // when & then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .body("{\"refreshToken\": \"" + refreshToken + "\"}")
            .when()
            .post("/auth/refresh")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
