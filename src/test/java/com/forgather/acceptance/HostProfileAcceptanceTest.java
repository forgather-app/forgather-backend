package com.forgather.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.host.model.HostProfilePhoto;
import com.forgather.domain.host.repository.HostProfilePhotoRepository;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.fixture.HostFixture;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ResponseCode;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.response.ExtractableResponse;

@DisplayName("인수 테스트: 마이 프로필")
@AutoConfigureMockMvc
class HostProfileAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private HostProfilePhotoRepository photoRepository;

    @Autowired
    private ContentsStorage contentsStorage;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUpMockMvc() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    private Host saveHostWithProfile() {
        Host host = HostFixture.createHost();
        host.updateProfile("포스티", "안녕하세요, 포게더 작가입니다.", "https://forgather.app/");
        return hostRepository.save(host);
    }

    private HostProfilePhoto saveProfilePhoto(Host host, String uploadFileName) {
        return photoRepository.save(new HostProfilePhoto(photoPath(host, uploadFileName), 1024L, host));
    }

    private String photoPath(Host host, String uploadFileName) {
        return "%s/hosts/%d/profile/%s".formatted(contentsStorage.getRootDirectory(), host.getId(), uploadFileName);
    }

    @DisplayName("내 프로필을 조회한다")
    @Test
    void getProfile() {
        // given
        Host host = saveHostWithProfile();
        saveProfilePhoto(host, "a.webp");
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .accept(ContentType.JSON)
            .when()
            .get("/hosts/me/profile")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(response.jsonPath().getString("data.nickname")).isEqualTo("포스티"),
            () -> assertThat(response.jsonPath().getString("data.introduction")).isEqualTo("안녕하세요, 포게더 작가입니다."),
            () -> assertThat(response.jsonPath().getString("data.linkUrl")).isEqualTo("https://forgather.app/"),
            () -> assertThat(response.jsonPath().getString("data.photoPath"))
                .isEqualTo(photoPath(host, "a.webp"))
        );
    }

    @DisplayName("로그인하지 않으면 프로필을 조회할 수 없다")
    @Test
    void getProfileWithoutLogin() {
        RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/hosts/me/profile")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("비로그인 방문자가 호스트 코드로 공개 프로필을 조회한다")
    @Test
    void getPublicProfile() {
        // given
        Host host = saveHostWithProfile();
        saveProfilePhoto(host, "a.webp");

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/hosts/{hostCode}/profile", host.getCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(response.jsonPath().getString("data.hostCode")).isEqualTo(host.getCode()),
            () -> assertThat(response.jsonPath().getString("data.nickname")).isEqualTo("포스티"),
            () -> assertThat(response.jsonPath().getString("data.introduction")).isEqualTo("안녕하세요, 포게더 작가입니다."),
            () -> assertThat(response.jsonPath().getString("data.linkUrl")).isEqualTo("https://forgather.app/"),
            () -> assertThat(response.jsonPath().getString("data.photoPath")).isEqualTo(photoPath(host, "a.webp")),
            () -> assertThat(response.jsonPath().getMap("data")).doesNotContainKeys("id", "name", "email")
        );
    }

    @DisplayName("프로필을 설정하지 않은 호스트는 닉네임 외 필드가 null이다")
    @Test
    void getPublicProfileWithoutOptionalFields() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/hosts/{hostCode}/profile", host.getCode())
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        assertAll(
            () -> assertThat(response.jsonPath().getString("data.nickname")).isEqualTo("포스티"),
            () -> assertThat(response.jsonPath().getString("data.introduction")).isNull(),
            () -> assertThat(response.jsonPath().getString("data.linkUrl")).isNull(),
            () -> assertThat(response.jsonPath().getString("data.photoPath")).isNull()
        );
    }

    @DisplayName("존재하지 않는 호스트 코드로 조회하면 찾을 수 없다")
    @Test
    void getPublicProfileWithUnknownCode() {
        RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/hosts/{hostCode}/profile", "zzzzzzzzzz")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("code", equalTo(ResponseCode.NOT_FOUND.name()));
    }

    @DisplayName("탈퇴한 호스트의 공개 프로필은 조회할 수 없다")
    @Test
    void getPublicProfileOfWithdrawnHost() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        host.delete();
        hostRepository.save(host);

        // when & then
        RestAssuredMockMvc.given()
            .accept(ContentType.JSON)
            .when()
            .get("/hosts/{hostCode}/profile", host.getCode())
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("code", equalTo(ResponseCode.NOT_FOUND.name()));
    }

    @DisplayName("프로필 전체를 수정한다")
    @Test
    void updateProfile() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of(
                "nickname", "새닉네임",
                "introduction", "새로운 한 줄 소개",
                "linkUrl", "https://forgather.app/new",
                "photo", Map.of("uploadFileName", "b.webp", "capacity", 1024)
            ))
            .when()
            .patch("/hosts/me/profile")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        Host savedHost = hostRepository.getByIdOrThrow(host.getId());

        assertAll(
            () -> assertThat(response.jsonPath().getString("code")).isEqualTo(ResponseCode.SUCCESS.name()),
            () -> assertThat(response.jsonPath().getString("data.nickname")).isEqualTo("새닉네임"),
            () -> assertThat(response.jsonPath().getString("data.photoPath")).isEqualTo(photoPath(host, "b.webp")),
            () -> assertThat(savedHost.getNickname()).isEqualTo("새닉네임"),
            () -> assertThat(savedHost.getIntroduction()).isEqualTo("새로운 한 줄 소개"),
            () -> assertThat(savedHost.getLinkUrl()).isEqualTo("https://forgather.app/new"),
            () -> assertThat(photoRepository.findByHostAndDeletedAtIsNull(savedHost))
                .get()
                .extracting(HostProfilePhoto::getPath)
                .isEqualTo(photoPath(host, "b.webp"))
        );
    }

    @DisplayName("전달하지 않은 필드는 유지되고, 빈 문자열 필드와 삭제 요청한 사진은 제거된다")
    @Test
    void updateProfilePartially() {
        // given
        Host host = saveHostWithProfile();
        saveProfilePhoto(host, "a.webp");
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        Map<String, Object> request = new HashMap<>();
        request.put("introduction", "");
        request.put("linkUrl", "");
        request.put("isDeletePhoto", true);

        // when
        ExtractableResponse<MockMvcResponse> response = RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .patch("/hosts/me/profile")
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract();

        // then
        Host savedHost = hostRepository.getByIdOrThrow(host.getId());

        assertAll(
            () -> assertThat(response.jsonPath().getString("data.photoPath")).isNull(),
            () -> assertThat(savedHost.getNickname()).isEqualTo("포스티"),
            () -> assertThat(savedHost.getIntroduction()).isNull(),
            () -> assertThat(savedHost.getLinkUrl()).isNull(),
            () -> assertThat(photoRepository.findByHostAndDeletedAtIsNull(savedHost)).isEmpty()
        );
    }

    @DisplayName("업로드 파일명 형식이 올바르지 않으면 프로필 수정에 실패한다")
    @Test
    void rejectUpdateWhenInvalidUploadFileName() {
        // given
        Host host = saveHostWithProfile();
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when & then
        RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of("photo", Map.of("uploadFileName", "hosts/a.webp", "capacity", 1024)))
            .when()
            .patch("/hosts/me/profile")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("닉네임이 10자를 초과하면 프로필 수정에 실패한다")
    @Test
    void rejectUpdateWhenNicknameTooLong() {
        // given
        Host host = saveHostWithProfile();
        String token = jwtTokenProvider.generateAccessToken(host.getId());

        // when
        RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of("nickname", "가나다라마바사아자차카"))
            .when()
            .patch("/hosts/me/profile")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());

        // then
        Host savedHost = hostRepository.getByIdOrThrow(host.getId());
        assertThat(savedHost.getNickname()).isEqualTo("포스티");
    }

    @DisplayName("이모지 닉네임은 보이는 글자 수 10자까지 실제 DB에 저장된다")
    @Test
    void updateProfileWithEmojiNickname() {
        // given
        Host host = hostRepository.save(HostFixture.createHost());
        String token = jwtTokenProvider.generateAccessToken(host.getId());
        String emojiNickname = "🧑‍🧑‍🧒‍🧒".repeat(10);

        // when
        RestAssuredMockMvc.given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of("nickname", emojiNickname))
            .when()
            .patch("/hosts/me/profile")
            .then()
            .statusCode(HttpStatus.OK.value());

        // then
        Host savedHost = hostRepository.getByIdOrThrow(host.getId());
        assertThat(savedHost.getNickname()).isEqualTo(emojiNickname);
    }

    @DisplayName("로그인하지 않으면 프로필을 수정할 수 없다")
    @Test
    void updateProfileWithoutLogin() {
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(Map.of("nickname", "새닉네임"))
            .when()
            .patch("/hosts/me/profile")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
