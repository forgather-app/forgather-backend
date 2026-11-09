package com.forgather.back_office.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminProductDetailResponse;
import com.forgather.back_office.dto.AdminProductFilterRequest;
import com.forgather.back_office.dto.AdminProductResponse;
import com.forgather.back_office.dto.AdminProductSummaryResponse;
import com.forgather.back_office.repository.AdminProductRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.space.model.SpaceHost;
import com.forgather.domain.space.repository.SpaceHostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private static final String TIE_BREAK_PROPERTY = "id";
    private static final Comparator<ProductPhoto> PHOTO_ORDER = Comparator
        .comparingInt(ProductPhoto::getSortOrder)
        .thenComparing(ProductPhoto::getId);

    private final AdminProductRepository adminProductRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final SpaceHostRepository spaceHostRepository;

    public AdminProductResponse getAllProducts(Pageable pageable) {
        Page<AdminProductSummaryResponse> products = adminProductRepository.findAllActive(withTieBreak(pageable));
        return AdminProductResponse.from(products);
    }

    public AdminProductResponse getProductsByFilters(AdminProductFilterRequest request, Pageable pageable) {
        String spaceCode = request.spaceCode();
        if (spaceCode == null || spaceCode.isBlank()) {
            return getAllProducts(pageable);
        }
        Page<AdminProductSummaryResponse> products = adminProductRepository.findAllActiveBySpaceCode(
            spaceCode.strip(), withTieBreak(pageable)
        );
        return AdminProductResponse.from(products);
    }

    public AdminProductResponse searchProductsByTitle(String title, Pageable pageable) {
        if (title == null) {
            return getAllProducts(pageable);
        }
        String strippedTitle = title.strip();
        if (strippedTitle.isEmpty()) {
            return getAllProducts(pageable);
        }
        String escapedTitle = escapeLikeWildcards(strippedTitle);
        Page<AdminProductSummaryResponse> products = adminProductRepository.findAllActiveByTitleContaining(
            escapedTitle, withTieBreak(pageable)
        );
        return AdminProductResponse.from(products);
    }

    public AdminProductDetailResponse getProductDetail(Long productId) {
        Product product = adminProductRepository.getActiveByIdWithSpaceOrThrow(productId);
        Host host = spaceHostRepository.findBySpaceAndDeletedAtIsNullWithHost(product.getSpace())
            .map(SpaceHost::getHost)
            .orElse(null);
        List<ProductPhoto> photos = productPhotoRepository.findAllByProductAndDeletedAtIsNull(product).stream()
            .sorted(PHOTO_ORDER)
            .toList();
        return AdminProductDetailResponse.of(product, host, photos);
    }

    /**
     * product.created_at은 초 단위라 같은 값이 흔하다. 정렬 값이 같은 행이 페이지 경계에서 중복되거나 누락되지 않도록
     * 요청 정렬 뒤에 id 정렬을 붙인다. 방향은 첫 번째 정렬과 같게 하고, 정렬이 없으면 최신순으로 둔다.
     */
    private Pageable withTieBreak(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.getOrderFor(TIE_BREAK_PROPERTY) != null) {
            return pageable;
        }
        Sort.Direction direction = sort.stream()
            .findFirst()
            .map(Sort.Order::getDirection)
            .orElse(Sort.Direction.DESC);
        Sort sortWithTieBreak = sort.and(Sort.by(direction, TIE_BREAK_PROPERTY));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortWithTieBreak);
    }

    private String escapeLikeWildcards(String input) {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
