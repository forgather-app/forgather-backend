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
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import com.forgather.domain.upload.domain.UploadFileMetadata;
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

    private static final String EXHIBITION_SIGNED_URLS_PATH = "/exhibitions/upload/signed-urls";

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
            new UploadFileRequest("abc.webp", 1024L),
            new UploadFileRequest("def.webp", 2048L)
        ));
        when(awsS3Cloud.getRootDirectory()).thenReturn("photogather/v2");
        when(awsS3Cloud.issueSignedUrl(anyString(), anyString(), anyLong())).thenAnswer(invocation -> {
            String path = invocation.getArgument(0);
            String contentType = invocation.getArgument(1);
            long contentLength = invocation.getArgument(2);
            return "test-prefix-" + path + "-" + contentType + "-" + contentLength + "-test-suffix";
        });

        // when
        ApiResponse<IssueSignedUrlResponse> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post(EXHIBITION_SIGNED_URLS_PATH)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        Map<String, String> signedUrls = result.data().signedUrls();

        // then
        String expectedPrefix = "test-prefix-photogather/v2/exhibitions/";
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(result.message()).isNull(),
            () -> assertThat(signedUrls.get("abc.webp"))
                .isEqualTo(expectedPrefix + "abc.webp-image/webp-1024-test-suffix"),
            () -> assertThat(signedUrls.get("def.webp"))
                .isEqualTo(expectedPrefix + "def.webp-image/webp-2048-test-suffix")
        );
    }

    @DisplayName("전시 사진 서명된 url 발급은 인증 없이 호출하면 401을 반환한다")
    @Test
    void issueExhibitionSignedUrlsRequiresAuth() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", 1024L)
        ));

        // when, then
        RestAssuredMockMvc.given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post(EXHIBITION_SIGNED_URLS_PATH)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("전시 사진 서명된 url 발급 시 업로드 파일 목록이 비어있거나 null이면 400(VALIDATION_FAILED)을 반환한다")
    @ParameterizedTest
    @MethodSource("emptyOrNullFileRequests")
    void issueExhibitionSignedUrlsWithEmptyOrNullFileNames(IssuePreSignedUrlRequest request) {
        // when
        ApiResponse<Void> result = postExhibitionSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("전시 사진 업로드 url 발급 시 파일명 형식이 올바르지 않으면 400(VALIDATION_FAILED)을 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"../../../etc/passwd", "a/b.webp", "a\\b.webp", "noext", "abc.WEBP"})
    void issueExhibitionSignedUrlsWithInvalidFileName(String invalidFileName) {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest(invalidFileName, 1024L)
        ));

        // when
        ApiResponse<Void> result = postExhibitionSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("전시 사진 업로드 url 발급 시 형식은 맞지만 지원하지 않는 확장자면 BAD_REQUEST를 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"photo.png", "photo.jpg", "x.gif", "icon.svg", "document.pdf"})
    void issueExhibitionSignedUrlsWithUnsupportedExtension(String unsupportedFileName) {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest(unsupportedFileName, 1024L)
        ));

        // when
        ApiResponse<Void> result = postExhibitionSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.BAD_REQUEST);
    }

    @DisplayName("전시 사진 업로드 url 발급 시 파일 크기가 20MB를 초과하면 400을 반환한다")
    @Test
    void issueExhibitionSignedUrlsWithOversizeFile() {
        // given
        long oversize = UploadFileMetadata.MAX_FILE_SIZE_BYTES + 1;
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", oversize)
        ));

        // when
        ApiResponse<Void> result = postExhibitionSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("전시 사진 업로드 url 발급 시 파일 크기가 정확히 최대치(20MB)이면 발급에 성공한다")
    @Test
    void issueExhibitionSignedUrlsWithExactlyMaxSizeFile() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", UploadFileMetadata.MAX_FILE_SIZE_BYTES)
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
            .post(EXHIBITION_SIGNED_URLS_PATH)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS);
    }

    @DisplayName("전시 사진 업로드 url 발급 시 파일 크기가 0 이하이면 400(VALIDATION_FAILED)을 반환한다")
    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void issueExhibitionSignedUrlsWithNonPositiveSize(long nonPositiveSize) {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", nonPositiveSize)
        ));

        // when
        ApiResponse<Void> result = postExhibitionSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("전시 사진 업로드 url 발급 시 파일명이 null이면 400(VALIDATION_FAILED)을 반환한다")
    @Test
    void issueExhibitionSignedUrlsWithNullFileName() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest(null, 1024L)
        ));

        // when
        ApiResponse<Void> result = postExhibitionSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    private static Stream<Named<IssuePreSignedUrlRequest>> emptyOrNullFileRequests() {
        return Stream.of(
            Named.of("빈 목록", new IssuePreSignedUrlRequest(List.of())),
            Named.of("null", new IssuePreSignedUrlRequest(null))
        );
    }

    private ApiResponse<Void> postExhibitionSignedUrlsExpectingBadRequest(IssuePreSignedUrlRequest request) {
        return RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post(EXHIBITION_SIGNED_URLS_PATH)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
    }
}
