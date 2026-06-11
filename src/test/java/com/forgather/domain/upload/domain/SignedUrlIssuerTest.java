package com.forgather.domain.upload.domain;

import static com.forgather.domain.upload.domain.UploadCategory.PRODUCT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.forgather.fake.FakeContentStorage;
import com.forgather.global.exception.BaseException;

class SignedUrlIssuerTest {

    private final SignedUrlIssuer signedUrlIssuer = new SignedUrlIssuer(new FakeContentStorage());

    private List<UploadFile> uploadFiles(String... fileNames) {
        return Arrays.stream(fileNames)
            .map(fileName -> new UploadFile(fileName, 1024L))
            .toList();
    }

    @DisplayName("스페이스 사진의 서명된 url을 발급한다")
    @Test
    void issueForSpace() {
        // given
        List<UploadFile> files = uploadFiles("abc.webp", "def.webp", "hij.webp");

        // when
        Map<String, String> result = signedUrlIssuer.issueForSpace(files, "1234567890", PRODUCT);

        // then
        assertAll(
            () -> assertThat(result.get("abc.webp"))
                .isEqualTo("test-prefix-photogather/v2/spaces/1234567890/product/abc.webp-test-suffix"),
            () -> assertThat(result.get("def.webp"))
                .isEqualTo("test-prefix-photogather/v2/spaces/1234567890/product/def.webp-test-suffix"),
            () -> assertThat(result.get("hij.webp"))
                .isEqualTo("test-prefix-photogather/v2/spaces/1234567890/product/hij.webp-test-suffix")
        );
    }

    @DisplayName("한 번에 발급 가능한 url 개수를 초과하면 예외를 던진다")
    @Test
    void throwExceptionWhenExceedMaxCount() {
        // given
        List<UploadFile> files = IntStream.range(0, 101)
            .mapToObj(number -> new UploadFile(number + ".webp", 1024L))
            .toList();

        // when, then
        assertThatThrownBy(() -> signedUrlIssuer.issueForSpace(files, "1234567890", PRODUCT))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("한번에 발급 가능한 업로드 url 개수");
    }

    @DisplayName("한 번에 발급 가능한 url 개수를 초과하지 않으면 예외를 던지지 않는다")
    @Test
    void notThrowExceptionWhenNotExceedMaxCount() {
        // given
        List<UploadFile> files = IntStream.range(0, 100)
            .mapToObj(number -> new UploadFile(number + ".webp", 1024L))
            .toList();

        // when, then
        assertThatCode(() -> signedUrlIssuer.issueForSpace(files, "1234567890", PRODUCT))
            .doesNotThrowAnyException();
    }

    @DisplayName("스페이스 사진 발급 시 파일 목록이 비어있으면 예외를 던진다")
    @Test
    void throwExceptionWhenSpaceFilesEmpty() {
        // when, then
        assertThatThrownBy(() -> signedUrlIssuer.issueForSpace(List.of(), "1234567890", PRODUCT))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("업로드 파일명 목록은 null이거나 비어있을 수 없습니다");
    }

    @DisplayName("전시 사진의 서명된 url을 발급한다")
    @Test
    void issueForExhibition() {
        // given
        List<UploadFile> files = uploadFiles("abc.webp", "def.webp");

        // when
        Map<String, String> result = signedUrlIssuer.issueForExhibition(files);

        // then
        assertAll(
            () -> assertThat(result.get("abc.webp"))
                .isEqualTo("test-prefix-photogather/v2/exhibitions/abc.webp-test-suffix"),
            () -> assertThat(result.get("def.webp"))
                .isEqualTo("test-prefix-photogather/v2/exhibitions/def.webp-test-suffix")
        );
    }

    @DisplayName("전시 사진 서명 url 발급 시 한 번에 발급 가능한 개수를 초과하면 예외를 던진다")
    @Test
    void throwExceptionWhenExhibitionExceedMaxCount() {
        // given
        List<UploadFile> files = IntStream.range(0, 101)
            .mapToObj(number -> new UploadFile(number + ".webp", 1024L))
            .toList();

        // when, then
        assertThatThrownBy(() -> signedUrlIssuer.issueForExhibition(files))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("한번에 발급 가능한 업로드 url 개수");
    }

    @DisplayName("전시 사진 서명 url 발급 시 파일 목록이 비어있으면 예외를 던진다")
    @Test
    void throwExceptionWhenExhibitionFilesEmpty() {
        // when, then
        assertThatThrownBy(() -> signedUrlIssuer.issueForExhibition(List.of()))
            .isInstanceOf(BaseException.class)
            .hasMessageContaining("업로드 파일명 목록은 null이거나 비어있을 수 없습니다");
    }
}
