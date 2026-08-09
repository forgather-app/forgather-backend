package com.forgather.domain.space.service;

import static com.forgather.domain.guestbook.model.VisibilityStatus.VISIBLE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.guestbook.repository.dto.SpaceGuestBookCountDto;
import com.forgather.domain.guestbook.service.GuestBookService;
import com.forgather.domain.product.model.Product;
import com.forgather.domain.product.model.ProductPhoto;
import com.forgather.domain.product.repository.ProductPhotoRepository;
import com.forgather.domain.product.repository.ProductRepository;
import com.forgather.domain.product.service.ProductService;
import com.forgather.domain.space.dto.CheckSpaceHostResponse;
import com.forgather.domain.space.dto.CreateSpaceRequest;
import com.forgather.domain.space.dto.CreateSpaceResponse;
import com.forgather.domain.space.dto.FeatureSpacesRequest;
import com.forgather.domain.space.dto.FeaturedSpacesResponse;
import com.forgather.domain.space.dto.HostSpaceItemResponse;
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.dto.SpaceResponse;
import com.forgather.domain.space.dto.UnfeatureSpacesRequest;
import com.forgather.domain.space.dto.UpdateSpaceRequest;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;
import com.forgather.domain.space.repository.SpacePhotoRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.model.SpaceHost;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ForbiddenException;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.util.RandomCodeGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceService {

    private final ProductService productService;
    private final GuestBookService guestBookService;
    private final SpaceRepository spaceRepository;
    private final SpacePhotoRepository spacePhotoRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final GuestBookCardRepository guestBookCardRepository;
    private final ProductRepository productRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final RandomCodeGenerator codeGenerator;

    @Transactional
    public CreateSpaceResponse create(CreateSpaceRequest request, Host host) {
        validateHostNull(host);
        String spaceCode = codeGenerator.generate(10);
        Space space = spaceRepository.save(request.toEntity(spaceCode));
        spaceHostRepository.save(new SpaceHost(space, host));
        return CreateSpaceResponse.from(space);
    }

    @Transactional(readOnly = true)
    public SpaceResponse getSpaceInformation(String spaceCode) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        return createSpaceResponse(space);
    }

    @Transactional
    public SpaceResponse update(String spaceCode, UpdateSpaceRequest request, Host host) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(space, host);

        space.update(request.name(), request.description(), request.isPublic(), request.linkUrl(),
            request.linkName());

        return createSpaceResponse(space);
    }

    private SpaceResponse createSpaceResponse(Space space) {
        Long guestBookCardCount =
            guestBookCardRepository.countBySpaceAndVisibilityStatusAndDeletedAtIsNull(space, VISIBLE);
        return SpaceResponse.from(space, findRepresentativePhoto(space), guestBookCardCount);
    }

    /**
     * 스페이스 사진은 대표 작품의 첫 번째 사진이다.
     * 작품이 없거나 대표 작품에 사진이 없으면 null을 반환하고, 기본 사진 노출은 클라이언트가 담당한다.
     */
    private ProductPhoto findRepresentativePhoto(Space space) {
        return productRepository.findFirstBySpaceAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(space)
            .flatMap(productPhotoRepository::findFirstByProductAndDeletedAtIsNullOrderBySortOrderAsc)
            .orElse(null);
    }

    @Transactional
    public void delete(String spaceCode, Host host) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(space, host);
        deleteGuestBookAndProduct(host, space);
        deleteSpaceHost(host, space);
        deleteSpacePhoto(space);
        space.delete();
    }

    private void validateSpaceHost(Space space, Host host) {
        validateHostNull(host);
        if (space == null) {
            throw new BaseNullPointerException("스페이스는 null일 수 없습니다.");
        }
        if (spaceHostRepository.findBySpaceAndHostAndDeletedAtIsNull(space, host).isPresent()) {
            return;
        }
        throw new ForbiddenException("권한이 존재하지 않습니다.");
    }

    private void deleteGuestBookAndProduct(Host host, Space space) {
        guestBookService.deleteAllCardsBySpace(host, space);
        productService.deleteIfExists(host, space);
    }

    private void deleteSpaceHost(Host host, Space space) {
        SpaceHost spaceHost = spaceHostRepository.getBySpaceAndHostAndDeletedAtIsNullOrThrow(space, host);
        spaceHost.delete();
    }

    private void deleteSpacePhoto(Space space) {
        spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)
            .ifPresent(this::deleteSpacePhoto);
    }

    private void deleteSpacePhoto(SpacePhoto spacePhoto) {
        spacePhoto.delete();
    }

    @Transactional(readOnly = true)
    public HostSpaceResponse getSpacesInformation(Host host) {
        List<SpaceHost> spaceHosts =
            spaceHostRepository.findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(host);
        if (spaceHosts.isEmpty()) {
            return new HostSpaceResponse(Collections.emptyList());
        }
        List<HostSpaceItemResponse> spaceResponses = createSpaceResponses(spaceHosts);
        return new HostSpaceResponse(spaceResponses);
    }

    private List<HostSpaceItemResponse> createSpaceResponses(List<SpaceHost> spaceHosts) {
        List<Long> spaceIds = spaceHosts.stream()
            .map(spaceHost -> spaceHost.getSpace().getId())
            .toList();

        Map<Long, Long> guestBookCardCounts = toCountBySpaceId(
            guestBookCardRepository.countBySpaceIdInAndVisibilityStatusAndDeletedAtIsNull(spaceIds, VISIBLE)
        );
        Map<Long, Long> unreadGuestBookCounts = toCountBySpaceId(
            guestBookCardRepository.countBySpaceIdInAndVisibilityStatusAndIsReadAndDeletedAtIsNull(
                spaceIds, VISIBLE, false)
        );

        Map<Long, ProductPhoto> spacePhotos = findRepresentativePhotos(spaceIds);

        return spaceHosts.stream()
            .map(spaceHost -> {
                Space space = spaceHost.getSpace();
                return HostSpaceItemResponse.from(
                    space,
                    spacePhotos.get(space.getId()),
                    guestBookCardCounts.getOrDefault(space.getId(), 0L),
                    unreadGuestBookCounts.getOrDefault(space.getId(), 0L)
                );
            })
            .toList();
    }

    /**
     * 스페이스별 대표 작품의 첫 번째 사진을 스페이스 수와 무관하게 쿼리 2회로 조회한다.
     * 대표 작품이 없거나 대표 작품에 사진이 없는 스페이스는 결과에서 빠지므로, 호출부는 null을 사진 없음으로 다룬다.
     */
    private Map<Long, ProductPhoto> findRepresentativePhotos(List<Long> spaceIds) {
        Map<Long, Product> representatives = productRepository.findEarliestCreatedPerSpace(spaceIds)
            .stream()
            .collect(Collectors.toMap(
                product -> product.getSpace().getId(),
                product -> product,
                // createdAt이 동점이면 한 스페이스에서 여러 건이 온다. id 오름차순으로 조회하므로 먼저 온 것이 대표다.
                (representative, later) -> representative
            ));
        if (representatives.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> spaceIdByProductId = representatives.entrySet().stream()
            .collect(Collectors.toMap(entry -> entry.getValue().getId(), Map.Entry::getKey));

        return productPhotoRepository.findFirstPhotosByProductIdIn(List.copyOf(spaceIdByProductId.keySet()))
            .stream()
            .collect(Collectors.toMap(
                photo -> spaceIdByProductId.get(photo.getProduct().getId()),
                photo -> photo
            ));
    }

    private Map<Long, Long> toCountBySpaceId(List<SpaceGuestBookCountDto> counts) {
        return counts.stream()
            .collect(Collectors.toMap(
                SpaceGuestBookCountDto::spaceId,
                SpaceGuestBookCountDto::guestBookCount)
            );
    }

    @Transactional
    public FeaturedSpacesResponse featureSpaces(Host host, FeatureSpacesRequest request) {
        validateHostNull(host);
        return FeaturedSpacesResponse.from(
            changeFeaturedState(host, request.toUniqueSpaceCodes(), Space::feature)
        );
    }

    @Transactional
    public void unfeatureSpaces(Host host, UnfeatureSpacesRequest request) {
        validateHostNull(host);
        changeFeaturedState(host, request.toUniqueSpaceCodes(), Space::unfeature);
    }

    private List<Space> changeFeaturedState(Host host, Set<String> targetCodes, Consumer<Space> action) {
        if (targetCodes.isEmpty()) {
            throw new BaseException("스페이스 코드 목록이 존재하지 않습니다.");
        }

        List<Space> hostSpaces = spaceHostRepository.findAllByHostAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(host)
            .stream()
            .map(SpaceHost::getSpace)
            .toList();
        validateTargetCodes(targetCodes, hostSpaces);

        hostSpaces.stream()
            .filter(space -> targetCodes.contains(space.getCode()))
            .forEach(action);
        return hostSpaces;
    }

    private void validateTargetCodes(Set<String> targetCodes, List<Space> spaces) {
        List<String> invalidCodes = targetCodes.stream()
            .filter(targetCode -> spaces.stream()
                .noneMatch(space -> space.isSameCode(targetCode))
            ).toList();

        if (!invalidCodes.isEmpty()) {
            throw new BaseException("유효하지 않은 스페이스 코드입니다. spaceCodes: " + invalidCodes);
        }
    }

    @Transactional(readOnly = true)
    public CheckSpaceHostResponse checkSpaceHost(String spaceCode, Host host) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateHostNull(host);
        return new CheckSpaceHostResponse(
            spaceHostRepository.findBySpaceAndHostAndDeletedAtIsNull(space, host).isPresent()
        );
    }

    private void validateHostNull(Host host) {
        if (host == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
    }
}
