package com.forgather.acceptance;

import static com.forgather.domain.upload.domain.UploadCategory.GUESTBOOK;
import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.SpaceFixture.createSpace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.AwsS3Cloud;
import com.forgather.domain.upload.dto.IssuePreSignedUrlRequest;
import com.forgather.domain.upload.dto.IssuePreSignedUrlRequest.UploadFileRequest;
import com.forgather.domain.upload.dto.IssueSignedUrlRequest;
import com.forgather.domain.upload.dto.IssueSignedUrlResponse;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.util.JwtTokenProvider;
import com.forgather.global.response.ApiResponse;
import com.forgather.global.response.ResponseCode;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;

@AutoConfigureMockMvc
class UploadAcceptanceTest extends AcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AwsS3Cloud awsS3Cloud;

    private Space space;
    private Host host;
    private String token;

    @BeforeEach
    void setUp() {
        space = createSpace();
        spaceRepository.save(space);
        host = hostRepository.save(createHost());
        token = jwtTokenProvider.generateAccessToken(host.getId());
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("서명된 url 발급")
    @Test
    void issueSignedUrls() {
        // given
        IssueSignedUrlRequest request = new IssueSignedUrlRequest(GUESTBOOK, List.of("abc.jpg", "def.jpg", "hij.png"));
        when(awsS3Cloud.getRootDirectory()).thenReturn("photogather/v2");
        when(awsS3Cloud.issueSignedUrl(anyString())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return "test-prefix-" + path + "-test-suffix";
        });

        // when
        ApiResponse<IssueSignedUrlResponse> result = RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/spaces/%s/upload/signed-urls".formatted(space.getCode()))
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        Map<String, String> signedUrls = result.data().signedUrls();

        // then
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(result.message()).isNull(),
            () -> assertThat(signedUrls.get("abc.jpg"))
                .isEqualTo("test-prefix-photogather/v2/spaces/1234567890/guestbook/abc.jpg-test-suffix"),
            () -> assertThat(signedUrls.get("def.jpg"))
                .isEqualTo("test-prefix-photogather/v2/spaces/1234567890/guestbook/def.jpg-test-suffix"),
            () -> assertThat(signedUrls.get("hij.png"))
                .isEqualTo("test-prefix-photogather/v2/spaces/1234567890/guestbook/hij.png-test-suffix")
        );
    }

    @DisplayName("전시 사진 서명된 url 발급")
    @Test
    void issueExhibitionSignedUrls() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.jpg", 1024L),
            new UploadFileRequest("def.jpg", 2048L)
        ));
        when(awsS3Cloud.getRootDirectory()).thenReturn("photogather/v2");
        when(awsS3Cloud.issueSignedUrl(anyString(), anyString(), anyLong())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            return "test-prefix-" + path + "-test-suffix";
        });

        // when
        ApiResponse<IssueSignedUrlResponse> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions/upload/signed-urls")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        Map<String, String> signedUrls = result.data().signedUrls();

        // then
        String expectedPrefix = "test-prefix-photogather/v2/exhibitions/host-%d/".formatted(host.getId());
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(result.message()).isNull(),
            () -> assertThat(signedUrls.get("abc.jpg"))
                .isEqualTo(expectedPrefix + "abc.jpg-test-suffix"),
            () -> assertThat(signedUrls.get("def.jpg"))
                .isEqualTo(expectedPrefix + "def.jpg-test-suffix")
        );
    }

    @DisplayName("전시 사진 서명된 url 발급은 인증 없이 호출하면 401을 반환한다")
    @Test
    void issueExhibitionSignedUrlsRequiresAuth() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.jpg", 1024L)
        ));

        // when, then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions/upload/signed-urls")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("전시 사진 서명된 url 발급 시 업로드 파일 이름 목록이 비어있으면 400을 반환한다")
    @Test
    void issueExhibitionSignedUrlsWithEmptyFileNames() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of());

        // when
        ApiResponse<Void> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions/upload/signed-urls")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("전시 사진 서명된 url 발급 시 업로드 파일 이름 목록이 null이면 400을 반환한다")
    @Test
    void issueExhibitionSignedUrlsWithNullFileNames() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(null);

        // when
        ApiResponse<Void> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions/upload/signed-urls")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("전시 사진 업로드 url 발급 시 파일명 형식이 올바르지 않으면 400을 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"../../../etc/passwd", "a/b.png", "a\\b.png", "noext", "x.gif"})
    void issueExhibitionSignedUrlsWithInvalidFileName(String invalidFileName) {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest(invalidFileName, 1024L)
        ));

        // when
        ApiResponse<Void> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions/upload/signed-urls")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("전시 사진 업로드 url 발급 시 파일 크기가 20MB를 초과하면 400을 반환한다")
    @Test
    void issueExhibitionSignedUrlsWithOversizeFile() {
        // given
        long oversize = 20L * 1024 * 1024 + 1;
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.jpg", oversize)
        ));

        // when
        ApiResponse<Void> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/exhibitions/upload/signed-urls")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }
}
