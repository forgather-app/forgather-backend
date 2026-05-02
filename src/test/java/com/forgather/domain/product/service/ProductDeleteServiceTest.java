package com.forgather.domain.product.service;

import static com.forgather.fixture.ProductFixture.createProductWithSpace;
import static com.forgather.fixture.ProductPhotoFixture.createProductPhotoWithProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.AppUserRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.fake.FakeContentStorage;
import com.forgather.fixture.AppUserFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.auth.model.AppUser;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;

@Import({ProductService.class, FakeContentStorage.class})
@DataJpaTest
public class ProductDeleteServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceHostRepository spaceHostRepository;

    private AppUser user;
    private Space space;

    @BeforeEach
    void setUp() {
        space = SpaceFixture.createSpace();
        spaceRepository.save(space);

        user = AppUserFixture.createAppUser();
        userRepository.save(user);

        spaceHostRepository.save(new SpaceHost(space, user));
    }

    @DisplayName("특정 작품을 논리 삭제한다")
    @Test
    void softDeleteProduct() {
        // given
        Product product1 = productRepository.save(createProductWithSpace(space));
        Product product2 = productRepository.save(createProductWithSpace(space));
        Product product3 = productRepository.save(createProductWithSpace(space));
        ProductPhoto productPhoto1 = createProductPhotoWithProduct(product2);
        ProductPhoto productPhoto2 = createProductPhotoWithProduct(product2);
        productPhotoRepository.saveAll(List.of(productPhoto1, productPhoto2));

        // when
        productService.delete(user, space.getCode(), product2.getId());

        // then
        assertAll(
            () -> assertThat(productRepository.findAllBySpaceAndDeletedAtIsNull(space)).containsExactly(product1, product3),
            () -> assertThat(product1.getDeletedAt()).isNull(),
            () -> assertThat(product2.getDeletedAt()).isNotNull(),
            () -> assertThat(product3.getDeletedAt()).isNull(),

            () -> assertThat(productPhoto1.getDeletedAt()).isNotNull(),
            () -> assertThat(productPhoto2.getDeletedAt()).isNotNull()
        );
    }

    @DisplayName("주어진 스페이스에 속하는 모든 작품을 논리 삭제한다")
    @Test
    void softDeleteProductBySpace() {
        // given
        Product product1 = productRepository.save(createProductWithSpace(space));
        Product product2 = productRepository.save(createProductWithSpace(space));
        Product product3 = productRepository.save(createProductWithSpace(space));
        ProductPhoto productPhoto1 = createProductPhotoWithProduct(product2);
        ProductPhoto productPhoto2 = createProductPhotoWithProduct(product2);
        productPhotoRepository.saveAll(List.of(productPhoto1, productPhoto2));

        // when
        productService.deleteIfExists(user, space);

        // then
        assertAll(
            () -> assertThat(productRepository.findAllBySpaceAndDeletedAtIsNull(space)).isEmpty(),
            () -> assertThat(product1.getDeletedAt()).isNotNull(),
            () -> assertThat(product2.getDeletedAt()).isNotNull(),
            () -> assertThat(product3.getDeletedAt()).isNotNull(),

            () -> assertThat(productPhoto1.getDeletedAt()).isNotNull(),
            () -> assertThat(productPhoto2.getDeletedAt()).isNotNull()
        );
    }
}
