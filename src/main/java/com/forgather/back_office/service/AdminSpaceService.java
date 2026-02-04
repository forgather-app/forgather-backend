package com.forgather.back_office.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.back_office.dto.AdminSpaceFilterRequest;
import com.forgather.back_office.dto.AdminSpaceResponse;
import com.forgather.back_office.dto.SpaceDetailResponse;
import com.forgather.back_office.repository.AdminSpaceRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.space.model.Space;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSpaceService {

    private final AdminSpaceRepository adminSpaceRepository;
    private final ProductRepository productRepository;
    private final GuestBookCardRepository guestBookCardRepository;

    public AdminSpaceResponse getAllSpaces(Pageable pageable) {
        Page<Space> spaces = adminSpaceRepository.findAllByDeletedAtIsNull(pageable);
        return AdminSpaceResponse.from(spaces);
    }

    public SpaceDetailResponse getSpaceDetail(String spaceCode) {
        Space space = adminSpaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        Long productCount = productRepository.countBySpaceAndDeletedAtIsNull(space);
        Long guestBookCardCount = guestBookCardRepository.countBySpaceAndDeletedAtIsNull(space);

        return SpaceDetailResponse.of(space, productCount, guestBookCardCount);
    }

    public AdminSpaceResponse getSpacesByFilters(AdminSpaceFilterRequest request, Pageable pageable) {
        if (request.hasProduct() == null) {
            Page<Space> allSpaces = adminSpaceRepository.findAllByDeletedAtIsNull(pageable);
            return AdminSpaceResponse.from(allSpaces);
        }

        Page<Space> filteredSpaces = adminSpaceRepository.findActiveSpacesFilteredByProductExistence(
            request.hasProduct(), pageable
        );
        return AdminSpaceResponse.from(filteredSpaces);
    }

    public AdminSpaceResponse searchSpacesByName(String name, Pageable pageable) {
        if (name == null) {
            return AdminSpaceResponse.from(adminSpaceRepository.findAllByDeletedAtIsNull(pageable));
        }
        String strippedName = name.strip();
        if (strippedName.isEmpty()) {
            return AdminSpaceResponse.from(adminSpaceRepository.findAllByDeletedAtIsNull(pageable));
        }
        String escapedName = escapeLikeWildcards(strippedName);
        Page<Space> spaces = adminSpaceRepository.findByNameContainingAndDeletedAtIsNull(escapedName, pageable);
        return AdminSpaceResponse.from(spaces);
    }

    private String escapeLikeWildcards(String input) {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
