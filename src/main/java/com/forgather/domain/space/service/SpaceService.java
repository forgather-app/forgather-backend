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
import com.forgather.domain.product.service.ProductService;
import com.forgather.domain.space.dto.CheckSpaceHostResponse;
import com.forgather.domain.space.dto.CreateSpaceRequest;
import com.forgather.domain.space.dto.CreateSpaceResponse;
import com.forgather.domain.space.dto.FeatureSpacesRequest;
import com.forgather.domain.space.dto.FeaturedSpacesResponse;
import com.forgather.domain.space.dto.HostSpaceItemResponse;
import com.forgather.domain.space.dto.HostSpaceResponse;
import com.forgather.domain.space.dto.SpacePhotoRequest;
import com.forgather.domain.space.dto.SpaceResponse;
import com.forgather.domain.space.dto.UnfeatureSpacesRequest;
import com.forgather.domain.space.dto.UpdateSpaceRequest;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;
import com.forgather.domain.space.repository.SpacePhotoRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.domain.upload.domain.FilePathGenerator;
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
    private final ContentsStorage contentsStorage;
    private final SpaceRepository spaceRepository;
    private final SpacePhotoRepository spacePhotoRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final GuestBookCardRepository guestBookCardRepository;
    private final RandomCodeGenerator codeGenerator;

    @Transactional
    public CreateSpaceResponse create(CreateSpaceRequest request, Host host) {
        validateHostNull(host);
        String spaceCode = codeGenerator.generate(10);
        Space space = spaceRepository.save(request.toEntity(spaceCode));
        spaceHostRepository.save(new SpaceHost(space, host));
        if (request.photo() != null) {
            savePhoto(space, request.photo());
        }
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

        space.update(request.name(), request.description(), request.isPublic(), request.instagramUsername(),
            request.email(), request.linkUrl(), request.linkName());

        updatePhoto(space, request);
        return createSpaceResponse(space);
    }

    /**
     * 새 사진이 오면 기존 사진 유무와 무관하게 교체하고, 사진 없이 삭제 요청만 오면 삭제한다.
     * 둘 다 없으면 기존 사진을 그대로 둔다. 세 갈래 모두 기존 상태와 무관하며 멱등하다.
     */
    private void updatePhoto(Space space, UpdateSpaceRequest request) {
        if (request.photo() != null) {
            deleteSpacePhoto(space);
            savePhoto(space, request.photo());
            return;
        }
        if (request.isDeletingPhoto()) {
            deleteSpacePhoto(space);
        }
    }

    private void savePhoto(Space space, SpacePhotoRequest photo) {
        String path = FilePathGenerator.generateSpacePhotoFilePath(
            contentsStorage.getRootDirectory(), photo.uploadFileName()
        );
        spacePhotoRepository.save(photo.toEntity(space, path));
    }

    private SpaceResponse createSpaceResponse(Space space) {
        Long guestBookCardCount =
            guestBookCardRepository.countBySpaceAndVisibilityStatusAndDeletedAtIsNull(space, VISIBLE);
        SpacePhoto spacePhoto = spacePhotoRepository.getBySpaceAndDeletedAtIsNullOrEmpty(space);
        return SpaceResponse.from(space, spacePhoto, guestBookCardCount);
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

        Map<Long, SpacePhoto> spacePhotos = spacePhotoRepository.findAllBySpaceIdInAndDeletedAtIsNull(spaceIds)
            .stream()
            .collect(Collectors.toMap(
                spacePhoto -> spacePhoto.getSpace().getId(),
                spacePhoto -> spacePhoto)
            );

        return spaceHosts.stream()
            .map(spaceHost -> {
                Space space = spaceHost.getSpace();
                return HostSpaceItemResponse.from(
                    space,
                    spacePhotos.getOrDefault(space.getId(), SpacePhoto.empty(space)),
                    guestBookCardCounts.getOrDefault(space.getId(), 0L),
                    unreadGuestBookCounts.getOrDefault(space.getId(), 0L)
                );
            })
            .toList();
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
