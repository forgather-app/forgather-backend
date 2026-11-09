package com.forgather.back_office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminProductDetailResponse;
import com.forgather.back_office.dto.AdminProductFilterRequest;
import com.forgather.back_office.dto.AdminProductPhotoResponse;
import com.forgather.back_office.dto.AdminProductResponse;
import com.forgather.back_office.dto.AdminProductSummaryResponse;
import com.forgather.container.TestOnContainer;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fixture.ProductFixture;
import com.forgather.fixture.ProductPhotoFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.NotFoundException;

import jakarta.persistence.EntityManager;

@Transactional
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AdminProductServiceTest extends TestOnContainer {

    @Autowired
    private AdminProductService adminProductService;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("스페이스 구분 없이 전체 작품 목록을 소속 스페이스 정보와 함께 조회한다.")
    @Test
    void getAllProducts() {
        // given
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName("1111111111", "첫 번째 전시"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName("2222222222", "두 번째 전시"));
        Product product1 = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space1, "작품1"));
        Product product2 = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space1, "작품2"));
        Product product3 = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space2, "작품3"));
        Pageable pageable = PageRequest.of(0, 10);

        // when
        AdminProductResponse result = adminProductService.getAllProducts(pageable);

        // then
        assertAll(
            () -> assertThat(result.products()).hasSize(3),
            () -> assertThat(result.currentPage()).isEqualTo(1),
            () -> assertThat(result.pageSize()).isEqualTo(10),
            () -> assertThat(result.totalCount()).isEqualTo(3),
            () -> assertThat(result.totalPages()).isEqualTo(1),
            () -> assertThat(result.products())
                .extracting(AdminProductSummaryResponse::id, AdminProductSummaryResponse::spaceCode,
                    AdminProductSummaryResponse::spaceName)
                .containsExactlyInAnyOrder(
                    tuple(product1.getId(), "1111111111", "첫 번째 전시"),
                    tuple(product2.getId(), "1111111111", "첫 번째 전시"),
                    tuple(product3.getId(), "2222222222", "두 번째 전시")
                )
        );
    }

    @DisplayName("작품 목록의 사진 수는 논리 삭제된 사진을 제외하고 센다.")
    @Test
    void countPhotosExcludingDeleted() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product product = productRepository.save(ProductFixture.createProductWithSpace(space));
        Product productWithoutPhoto = productRepository.save(ProductFixture.createProductWithSpace(space));
        productPhotoRepository.save(ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 1));
        productPhotoRepository.save(ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 2));
        ProductPhoto deletedPhoto = productPhotoRepository.save(
            ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 3));
        deletedPhoto.delete();
        entityManager.flush();
        entityManager.clear();

        // when
        AdminProductResponse result = adminProductService.getAllProducts(PageRequest.of(0, 10));

        // then
        assertThat(result.products())
            .extracting(AdminProductSummaryResponse::id, AdminProductSummaryResponse::photoCount)
            .containsExactlyInAnyOrder(
                tuple(product.getId(), 2L),
                tuple(productWithoutPhoto.getId(), 0L)
            );
    }

    @DisplayName("논리 삭제된 작품과 삭제된 스페이스의 작품은 목록에서 제외한다.")
    @Test
    void excludeDeletedProductsAndProductsInDeletedSpace() {
        // given
        Space activeSpace = spaceRepository.save(SpaceFixture.createSpaceWithCode("1111111111"));
        Space deletedSpace = spaceRepository.save(SpaceFixture.createSpaceWithCode("2222222222"));
        Product activeProduct = productRepository.save(ProductFixture.createProductWithSpace(activeSpace));
        Product deletedProduct = productRepository.save(ProductFixture.createProductWithSpace(activeSpace));
        productRepository.save(ProductFixture.createProductWithSpace(deletedSpace));
        deletedProduct.delete();
        deletedSpace.delete();
        entityManager.flush();
        entityManager.clear();

        // when
        AdminProductResponse result = adminProductService.getAllProducts(PageRequest.of(0, 10));

        // then
        assertAll(
            () -> assertThat(result.totalCount()).isOne(),
            () -> assertThat(result.products())
                .extracting(AdminProductSummaryResponse::id)
                .containsExactly(activeProduct.getId())
        );
    }

    @DisplayName("등록일 최신순으로 정렬하면 등록일이 같은 작품은 나중에 등록한 작품이 먼저 온다.")
    @Test
    void sortByCreatedAtDescWithIdTieBreak() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product first = productRepository.save(ProductFixture.createProductWithSpace(space));
        Product second = productRepository.save(ProductFixture.createProductWithSpace(space));
        Product third = productRepository.save(ProductFixture.createProductWithSpace(space));
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        AdminProductResponse result = adminProductService.getAllProducts(pageable);

        // then
        assertThat(result.products())
            .extracting(AdminProductSummaryResponse::id)
            .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @DisplayName("작품명 내림차순으로 정렬하면 작품명이 같은 작품은 나중에 등록한 작품이 먼저 온다.")
    @Test
    void sortByTitleDescWithIdTieBreak() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product cherry = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "cherry"));
        Product banana1 = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "banana"));
        Product apple = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "apple"));
        Product banana2 = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "banana"));
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "title"));

        // when
        AdminProductResponse result = adminProductService.getAllProducts(pageable);

        // then
        assertThat(result.products())
            .extracting(AdminProductSummaryResponse::id)
            .containsExactly(cherry.getId(), banana2.getId(), banana1.getId(), apple.getId());
    }

    @DisplayName("스페이스 코드로 필터링하면 해당 스페이스의 작품만 조회한다.")
    @Test
    void getProductsBySpaceCode() {
        // given
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCode("1111111111"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("2222222222"));
        Product product1 = productRepository.save(ProductFixture.createProductWithSpace(space1));
        Product product2 = productRepository.save(ProductFixture.createProductWithSpace(space1));
        productRepository.save(ProductFixture.createProductWithSpace(space2));
        AdminProductFilterRequest request = new AdminProductFilterRequest(" 1111111111 ");

        // when
        AdminProductResponse result = adminProductService.getProductsByFilters(request, PageRequest.of(0, 10));

        // then
        assertAll(
            () -> assertThat(result.totalCount()).isEqualTo(2),
            () -> assertThat(result.products())
                .extracting(AdminProductSummaryResponse::id)
                .containsExactlyInAnyOrder(product1.getId(), product2.getId())
        );
    }

    @DisplayName("존재하지 않는 스페이스 코드로 필터링하면 빈 목록을 반환한다.")
    @Test
    void getProductsByUnknownSpaceCode() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCode("1111111111"));
        productRepository.save(ProductFixture.createProductWithSpace(space));
        AdminProductFilterRequest request = new AdminProductFilterRequest("9999999999");

        // when
        AdminProductResponse result = adminProductService.getProductsByFilters(request, PageRequest.of(0, 10));

        // then
        assertAll(
            () -> assertThat(result.products()).isEmpty(),
            () -> assertThat(result.totalCount()).isZero()
        );
    }

    @DisplayName("스페이스 코드 필터가 비어 있으면 전체 작품을 조회한다.")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void getProductsWithoutSpaceCode(String spaceCode) {
        // given
        Space space1 = spaceRepository.save(SpaceFixture.createSpaceWithCode("1111111111"));
        Space space2 = spaceRepository.save(SpaceFixture.createSpaceWithCode("2222222222"));
        productRepository.save(ProductFixture.createProductWithSpace(space1));
        productRepository.save(ProductFixture.createProductWithSpace(space2));
        AdminProductFilterRequest request = new AdminProductFilterRequest(spaceCode);

        // when
        AdminProductResponse result = adminProductService.getProductsByFilters(request, PageRequest.of(0, 10));

        // then
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @DisplayName("작품명의 일부와 일치하면 작품명 검색으로 조회된다.")
    @ParameterizedTest
    @ValueSource(strings = {"졸업 작품", "졸업", "작품", "업 작", " 업 작 "})
    void searchProductsByTitle(String keyword) {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product matched = productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "졸업 작품"));
        productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "습작"));

        // when
        AdminProductResponse result = adminProductService.searchProductsByTitle(keyword, PageRequest.of(0, 10));

        // then
        assertThat(result.products())
            .extracting(AdminProductSummaryResponse::id)
            .containsExactly(matched.getId());
    }

    @DisplayName("작품명 검색 결과가 없으면 빈 목록을 반환한다.")
    @Test
    void searchProductsByTitleWithoutResult() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "졸업 작품"));

        // when
        AdminProductResponse result = adminProductService.searchProductsByTitle("개인전", PageRequest.of(0, 10));

        // then
        assertThat(result.products()).isEmpty();
    }

    @DisplayName("작품명 검색어가 비어 있으면 전체 작품을 조회한다.")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void searchProductsByBlankTitle(String keyword) {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "졸업 작품"));
        productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, "습작"));

        // when
        AdminProductResponse result = adminProductService.searchProductsByTitle(keyword, PageRequest.of(0, 10));

        // then
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @DisplayName("작품명 검색어의 LIKE 와일드카드 문자는 문자 그대로 검색한다.")
    @ParameterizedTest
    @MethodSource("wildcardKeywords")
    void searchProductsByTitleWithWildcard(String keyword, String expectedTitle) {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        for (String title : new String[]{"할인 50%", "할인 500", "작품_1", "작품11", "경로\\파일", "경로파일"}) {
            productRepository.save(ProductFixture.createProductWithSpaceAndTitle(space, title));
        }

        // when
        AdminProductResponse result = adminProductService.searchProductsByTitle(keyword, PageRequest.of(0, 10));

        // then
        assertThat(result.products())
            .extracting(AdminProductSummaryResponse::title)
            .containsExactly(expectedTitle);
    }

    private static Stream<Arguments> wildcardKeywords() {
        return Stream.of(
            Arguments.of("%", "할인 50%"),
            Arguments.of("_", "작품_1"),
            Arguments.of("\\", "경로\\파일")
        );
    }

    @DisplayName("작품 상세를 조회하면 소속 스페이스와 삭제되지 않은 사진을 정렬 순서대로 반환한다.")
    @Test
    void getProductDetail() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpaceWithCodeAndName("1111111111", "졸업 전시"));
        Product product = productRepository.save(new Product(space, "고귀한 의자", "검은수염", "150 x 200\n조심히 다뤄주세요.",
            "https://youtu.be/lkuAxAVgAX0", false));
        productPhotoRepository.save(ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 3));
        productPhotoRepository.save(ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 1));
        ProductPhoto deletedPhoto = productPhotoRepository.save(
            ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 4));
        productPhotoRepository.save(ProductPhotoFixture.createProductPhotoWithProductAndOrder(product, 2));
        deletedPhoto.delete();
        entityManager.flush();
        entityManager.clear();

        // when
        AdminProductDetailResponse result = adminProductService.getProductDetail(product.getId());

        // then
        assertAll(
            () -> assertThat(result.id()).isEqualTo(product.getId()),
            () -> assertThat(result.title()).isEqualTo("고귀한 의자"),
            () -> assertThat(result.authorName()).isEqualTo("검은수염"),
            () -> assertThat(result.description()).isEqualTo("150 x 200\n조심히 다뤄주세요."),
            () -> assertThat(result.videoUrl()).isEqualTo("https://youtu.be/lkuAxAVgAX0"),
            () -> assertThat(result.isVideoAfterPhoto()).isFalse(),
            () -> assertThat(result.createdAt()).isNotNull(),
            () -> assertThat(result.space().code()).isEqualTo("1111111111"),
            () -> assertThat(result.space().name()).isEqualTo("졸업 전시"),
            () -> assertThat(result.photos())
                .extracting(AdminProductPhotoResponse::sortOrder)
                .containsExactly(1, 2, 3),
            () -> assertThat(result.photos().getFirst().path()).isEqualTo("path1"),
            () -> assertThat(result.photos().getFirst().capacity()).isEqualTo(1024L)
        );
    }

    @DisplayName("존재하지 않는 작품을 상세 조회하면 예외를 던진다.")
    @Test
    void getProductDetailNotFound() {
        assertThatThrownBy(() -> adminProductService.getProductDetail(Long.MAX_VALUE))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("존재하지 않는 작품입니다.");
    }

    @DisplayName("논리 삭제된 작품을 상세 조회하면 예외를 던진다.")
    @Test
    void getDeletedProductDetail() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product product = productRepository.save(ProductFixture.createProductWithSpace(space));
        product.delete();
        entityManager.flush();
        entityManager.clear();

        // when & then
        assertThatThrownBy(() -> adminProductService.getProductDetail(product.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("삭제된 스페이스의 작품을 상세 조회하면 예외를 던진다.")
    @Test
    void getProductDetailInDeletedSpace() {
        // given
        Space space = spaceRepository.save(SpaceFixture.createSpace());
        Product product = productRepository.save(ProductFixture.createProductWithSpace(space));
        space.delete();
        entityManager.flush();
        entityManager.clear();

        // when & then
        assertThatThrownBy(() -> adminProductService.getProductDetail(product.getId()))
            .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("작품 id가 null이면 상세 조회할 수 없다.")
    @Test
    void getProductDetailWithNullId() {
        assertThatThrownBy(() -> adminProductService.getProductDetail(null))
            .isInstanceOf(BaseNullPointerException.class);
    }
}
