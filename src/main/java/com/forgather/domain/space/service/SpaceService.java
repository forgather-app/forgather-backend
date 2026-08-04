package com.forgather.domain.space.service;

import static com.forgather.domain.guestbook.model.VisibilityStatus.VISIBLE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import com.forgather.domain.space.dto.SpaceResponse;
import com.forgather.domain.space.dto.UnfeatureSpacesRequest;
import com.forgather.domain.space.dto.UpdateSpaceRequest;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;
import com.forgather.domain.space.repository.SpacePhotoRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.service.UploadService;
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
    private final UploadService uploadService;
    private final SpaceRepository spaceRepository;
    private final SpacePhotoRepository spacePhotoRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final GuestBookCardRepository guestBookCardRepository;
    private final RandomCodeGenerator codeGenerator;

    @Transactional
    public CreateSpaceResponse create(CreateSpaceRequest request, MultipartFile file, Host host) {
        validateHostNull(host);
        String spaceCode = codeGenerator.generate(10);
        Space space = spaceRepository.save(request.toEntity(spaceCode));
        spaceHostRepository.save(new SpaceHost(space, host));
        if (file == null || file.isEmpty()) {
            return CreateSpaceResponse.from(space);
        }
        String path = uploadService.upload(spaceCode, file);
        spacePhotoRepository.save(new SpacePhoto(space, file.getOriginalFilename(), path, file.getSize()));
        return CreateSpaceResponse.from(space);
    }

    @Transactional(readOnly = true)
    public SpaceResponse getSpaceInformation(String spaceCode) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        return createSpaceResponse(space);
    }

    @Transactional
    public SpaceResponse update(String spaceCode, UpdateSpaceRequest request, MultipartFile file, Host host) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(space, host);

        space.update(request.name(), request.description(), request.isPublic(), request.instagramUsername(),
            request.email(), request.linkUrl(), request.linkName());

        if (request.isDeletingPhoto()) {
            handlePhotoWithDeleteRequest(space, file, spaceCode);
        } else {
            handlePhotoWithoutDeleteRequest(space, file, spaceCode);
        }
        return createSpaceResponse(space);
    }

    /**
     * 삭제 요청이 없는 경우: 새 파일이 있으면 업로드 (기존 사진이 없어야 함)
     */
    private void handlePhotoWithoutDeleteRequest(Space space, MultipartFile file, String spaceCode) {
        if (file != null && !file.isEmpty()) {
            uploadNewPhoto(space, file, spaceCode);
        }
    }

    /**
     * 삭제 요청이 있는 경우: 기존 사진을 삭제하고, 파일이 존재하면 업로드
     */
    private void handlePhotoWithDeleteRequest(Space space, MultipartFile file, String spaceCode) {
        deleteExistingPhoto(space);
        if (file != null && !file.isEmpty()) {
            uploadNewPhoto(space, file, spaceCode);
        }
    }

    private void uploadNewPhoto(Space space, MultipartFile file, String spaceCode) {
        spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)
            .ifPresent(photo -> {
                throw new BaseException("스페이스 사진이 이미 존재합니다. 기존 스페이스 사진을 삭제 해주세요.");
            });

        String path = uploadService.upload(spaceCode, file);
        spacePhotoRepository.save(new SpacePhoto(space, file.getOriginalFilename(), path, file.getSize()));
    }

    private void deleteExistingPhoto(Space space) {
        SpacePhoto existingPhoto = spacePhotoRepository.findBySpaceAndDeletedAtIsNull(space)
            .orElseThrow(() -> new BaseException("삭제할 스페이스 사진이 존재하지 않습니다."));

        deleteSpacePhoto(existingPhoto);
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
        Set<String> targetCodes = request.toUniqueSpaceCodes();
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
            .forEach(Space::feature);
        return FeaturedSpacesResponse.from(hostSpaces);
    }

    @Transactional
    public FeaturedSpacesResponse unfeatureSpaces(Host host, UnfeatureSpacesRequest request) {
        validateHostNull(host);
        Set<String> targetCodes = request.toUniqueSpaceCodes();
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
            .forEach(Space::unfeature);
        return FeaturedSpacesResponse.from(hostSpaces);
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
