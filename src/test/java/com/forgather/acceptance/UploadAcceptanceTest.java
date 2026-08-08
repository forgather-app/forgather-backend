package com.forgather.acceptance;

import static com.forgather.domain.upload.domain.UploadCategory.GUESTBOOK;
import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.SpaceFixture.createSpace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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
    private static final String SPACE_PHOTO_SIGNED_URLS_PATH = "/spaces/photos/upload/signed-urls";

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

    @DisplayName("프로필 사진 서명된 url 발급")
    @Test
    void issueHostProfileSignedUrls() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", 1024L)
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
            .post("/hosts/me/profile/upload/signed-urls")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        Map<String, String> signedUrls = result.data().signedUrls();

        // then
        String expectedPrefix = "test-prefix-photogather/v2/hosts/%d/profile/".formatted(host.getId());
        assertAll(
            () -> assertThat(result.code()).isEqualTo(ResponseCode.SUCCESS),
            () -> assertThat(result.message()).isNull(),
            () -> assertThat(signedUrls.get("abc.webp"))
                .isEqualTo(expectedPrefix + "abc.webp-image/webp-1024-test-suffix")
        );
    }

    @DisplayName("프로필 사진 서명된 url은 한 장만 발급할 수 있다")
    @Test
    void rejectHostProfileSignedUrlsWhenMultipleFiles() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", 1024L),
            new UploadFileRequest("def.webp", 2048L)
        ));

        // when, then
        RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post("/hosts/me/profile/upload/signed-urls")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("프로필 사진 서명된 url 발급은 인증 없이 호출하면 401을 반환한다")
    @Test
    void issueHostProfileSignedUrlsRequiresAuth() {
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
            .post("/hosts/me/profile/upload/signed-urls")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
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

    @DisplayName("전시 사진 업로드 url 발급 시 업로드 파일 목록에 null이 포함되면 400(VALIDATION_FAILED)을 반환한다")
    @Test
    void issueExhibitionSignedUrlsWithNullFileElement() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(Arrays.asList(
            new UploadFileRequest("abc.webp", 1024L),
            null
        ));

        // when
        ApiResponse<Void> result = postExhibitionSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("스페이스 사진 서명된 url 발급")
    @Test
    void issueSpacePhotoSignedUrls() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", 1024L)
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
            .post(SPACE_PHOTO_SIGNED_URLS_PATH)
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
            () -> assertThat(signedUrls.get("abc.webp"))
                .isEqualTo("test-prefix-photogather/v2/spaces/photos/%d/abc.webp-image/webp-1024-test-suffix"
                    .formatted(host.getId()))
        );
    }

    /**
     * 신규 경로 {@code /spaces/photos/upload/signed-urls}는 레거시 {@code /spaces/{spaceCode}/upload/signed-urls}와
     * 세그먼트 수가 같아 spaceCode="photos"로도 매칭될 수 있는 형태다. Spring이 리터럴 패턴을 우선하므로
     * 신규 핸들러가 잡히고, 레거시 핸들러는 인증이 없으므로 401이 나온다는 사실로 매핑 우선순위를 고정한다.
     */
    @DisplayName("스페이스 사진 발급 경로는 레거시 발급 API보다 우선 매칭된다")
    @Test
    void spacePhotoPathTakesPrecedenceOverLegacyPath() {
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
            .post(SPACE_PHOTO_SIGNED_URLS_PATH)
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * 파일명은 클라이언트가 정하므로, 경로 격리가 없으면 다른 호스트가 쓰는 파일명을 그대로 요청해
     * 같은 객체 키에 대한 PUT URL을 받아 덮어쓸 수 있다. 스페이스 사진 경로는 공개 조회 응답에
     * 그대로 노출되므로 파일명 추측도 필요 없다. 같은 파일명이라도 호스트별로 키가 갈리는지 고정한다.
     */
    @DisplayName("같은 파일명이라도 호스트마다 다른 경로로 발급된다")
    @Test
    void issueSpacePhotoSignedUrlsIsolatesByHost() {
        // given
        Host otherHost = hostRepository.save(createHost());
        String otherToken = jwtTokenProvider.generateAccessToken(otherHost.getId());
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", 1024L)
        ));
        when(awsS3Cloud.getRootDirectory()).thenReturn("photogather/v2");
        when(awsS3Cloud.issueSignedUrl(anyString(), anyString(), anyLong()))
            .thenAnswer(invocation -> invocation.getArgument(0).toString());

        // when
        String mine = issueSpacePhotoSignedUrl(token, request);
        String others = issueSpacePhotoSignedUrl(otherToken, request);

        // then
        assertAll(
            () -> assertThat(mine).isEqualTo("photogather/v2/spaces/photos/%d/abc.webp".formatted(host.getId())),
            () -> assertThat(others)
                .isEqualTo("photogather/v2/spaces/photos/%d/abc.webp".formatted(otherHost.getId())),
            () -> assertThat(mine).isNotEqualTo(others)
        );
    }

    private String issueSpacePhotoSignedUrl(String accessToken, IssuePreSignedUrlRequest request) {
        ApiResponse<IssueSignedUrlResponse> result = RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post(SPACE_PHOTO_SIGNED_URLS_PATH)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
        return result.data().signedUrls().get("abc.webp");
    }

    @DisplayName("스페이스 사진 서명된 url은 한 장만 발급할 수 있다")
    @Test
    void rejectSpacePhotoSignedUrlsWhenMultipleFiles() {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", 1024L),
            new UploadFileRequest("def.webp", 2048L)
        ));

        // when
        ApiResponse<Void> result = postSpacePhotoSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.BAD_REQUEST);
    }

    @DisplayName("스페이스 사진 업로드 url 발급 시 형식은 맞지만 지원하지 않는 확장자면 BAD_REQUEST를 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"photo.png", "photo.jpg", "x.gif", "icon.svg", "document.pdf"})
    void issueSpacePhotoSignedUrlsWithUnsupportedExtension(String unsupportedFileName) {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest(unsupportedFileName, 1024L)
        ));

        // when
        ApiResponse<Void> result = postSpacePhotoSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.BAD_REQUEST);
    }

    @DisplayName("스페이스 사진 업로드 url 발급 시 파일명 형식이 올바르지 않으면 400(VALIDATION_FAILED)을 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"../../../etc/passwd", "a/b.webp", "a\\b.webp", "noext", "abc.WEBP"})
    void issueSpacePhotoSignedUrlsWithInvalidFileName(String invalidFileName) {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest(invalidFileName, 1024L)
        ));

        // when
        ApiResponse<Void> result = postSpacePhotoSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("스페이스 사진 업로드 url 발급 시 업로드 파일 목록이 비어있거나 null이면 400(VALIDATION_FAILED)을 반환한다")
    @ParameterizedTest
    @MethodSource("emptyOrNullFileRequests")
    void issueSpacePhotoSignedUrlsWithEmptyOrNullFileNames(IssuePreSignedUrlRequest request) {
        // when
        ApiResponse<Void> result = postSpacePhotoSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    @DisplayName("스페이스 사진 업로드 url 발급 시 파일 크기가 0 이하이거나 20MB를 초과하면 400(VALIDATION_FAILED)을 반환한다")
    @ParameterizedTest
    @MethodSource("invalidFileSizes")
    void issueSpacePhotoSignedUrlsWithInvalidSize(long invalidSize) {
        // given
        IssuePreSignedUrlRequest request = new IssuePreSignedUrlRequest(List.of(
            new UploadFileRequest("abc.webp", invalidSize)
        ));

        // when
        ApiResponse<Void> result = postSpacePhotoSignedUrlsExpectingBadRequest(request);

        // then
        assertThat(result.code()).isEqualTo(ResponseCode.VALIDATION_FAILED);
    }

    private static Stream<Named<Long>> invalidFileSizes() {
        return Stream.of(
            Named.of("0", 0L),
            Named.of("음수", -1L),
            Named.of("20MB 초과", UploadFileMetadata.MAX_FILE_SIZE_BYTES + 1)
        );
    }

    private ApiResponse<Void> postSpacePhotoSignedUrlsExpectingBadRequest(IssuePreSignedUrlRequest request) {
        return RestAssuredMockMvc.given()
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(request)
            .when()
            .post(SPACE_PHOTO_SIGNED_URLS_PATH)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .body()
            .as(new TypeRef<>() {
            });
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
