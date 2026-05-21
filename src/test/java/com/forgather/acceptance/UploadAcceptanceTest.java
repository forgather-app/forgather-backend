package com.forgather.acceptance;

import static com.forgather.domain.upload.domain.UploadCategory.EXHIBITION;
import static com.forgather.domain.upload.domain.UploadCategory.GUESTBOOK;
import static com.forgather.fixture.HostFixture.createHost;
import static com.forgather.fixture.SpaceFixture.createSpace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.HostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.AwsS3Cloud;
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
        IssueSignedUrlRequest request = new IssueSignedUrlRequest(EXHIBITION, List.of("abc.jpg", "def.jpg"));
        when(awsS3Cloud.getRootDirectory()).thenReturn("photogather/v2");
        when(awsS3Cloud.issueSignedUrl(anyString())).thenAnswer(invocation -> {
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
        IssueSignedUrlRequest request = new IssueSignedUrlRequest(EXHIBITION, List.of("abc.jpg"));

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
}
