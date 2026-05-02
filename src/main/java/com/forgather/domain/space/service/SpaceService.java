package com.forgather.domain.space.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.forgather.domain.space.dto.UserSpaceResponse;
import com.forgather.domain.space.dto.SpaceResponse;
import com.forgather.domain.space.dto.UpdateSpaceRequest;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.model.SpacePhoto;
import com.forgather.domain.space.repository.SpacePhotoRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.service.UploadService;
import com.forgather.global.auth.model.AppUser;
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
    public CreateSpaceResponse create(CreateSpaceRequest request, MultipartFile file, AppUser user) {
        validateUserNull(user);
        String spaceCode = codeGenerator.generate(10);
        Space space = spaceRepository.save(request.toEntity(spaceCode));
        spaceHostRepository.save(new SpaceHost(space, user));
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
    public SpaceResponse update(String spaceCode, UpdateSpaceRequest request, MultipartFile file, AppUser user) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(space, user);

        space.update(request.name(), request.description(), request.isPublic(), request.instagramUsername(),
            request.email());

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
        Long guestBookCardCount = guestBookCardRepository.countBySpaceAndDeletedAtIsNull(space);
        SpacePhoto spacePhoto = spacePhotoRepository.getBySpaceAndDeletedAtIsNullOrEmpty(space);
        return SpaceResponse.from(space, spacePhoto, guestBookCardCount);
    }

    @Transactional
    public void delete(String spaceCode, AppUser user) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(space, user);
        deleteGuestBookAndProduct(user, space);
        deleteSpaceHost(user, space);
        deleteSpacePhoto(space);
        space.delete();
    }

    private void validateSpaceHost(Space space, AppUser user) {
        validateUserNull(user);
        if (space == null) {
            throw new BaseNullPointerException("스페이스는 null일 수 없습니다.");
        }
        if (spaceHostRepository.findBySpaceAndAppUserAndDeletedAtIsNull(space, user).isPresent()) {
            return;
        }
        throw new ForbiddenException("권한이 존재하지 않습니다.");
    }

    private void deleteGuestBookAndProduct(AppUser user, Space space) {
        guestBookService.deleteAllCardsBySpace(user, space);
        productService.deleteIfExists(user, space);
    }

    private void deleteSpaceHost(AppUser user, Space space) {
        SpaceHost spaceHost = spaceHostRepository.getBySpaceAndAppUserAndDeletedAtIsNullOrThrow(space, user);
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
    public UserSpaceResponse getSpacesInformation(AppUser user) {
        List<SpaceHost> spaceHosts = spaceHostRepository.findAllByAppUserAndDeletedAtIsNullWithSpaceOrderByCreatedAtDesc(user);
        if (spaceHosts.isEmpty()) {
            return new UserSpaceResponse(Collections.emptyList());
        }
        List<SpaceResponse> spaceResponses = createSpaceResponses(spaceHosts);
        return new UserSpaceResponse(spaceResponses);
    }

    private List<SpaceResponse> createSpaceResponses(List<SpaceHost> spaceHosts) {
        List<Long> spaceIds = spaceHosts.stream()
            .map(spaceHost -> spaceHost.getSpace().getId())
            .toList();

        Map<Long, Long> guestBookCardCounts = guestBookCardRepository.countBySpaceIdAndDeletedAtIsNullIn(spaceIds)
            .stream()
            .collect(Collectors.toMap(
                SpaceGuestBookCountDto::spaceId,
                SpaceGuestBookCountDto::guestBookCount)
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
                Long guestBookCardCount = guestBookCardCounts.getOrDefault(space.getId(), 0L);
                SpacePhoto spacePhoto = spacePhotos.getOrDefault(space.getId(), SpacePhoto.empty(space));
                return SpaceResponse.from(space, spacePhoto, guestBookCardCount);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public CheckSpaceHostResponse checkSpaceHost(String spaceCode, AppUser user) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateUserNull(user);
        return new CheckSpaceHostResponse(spaceHostRepository.findBySpaceAndAppUserAndDeletedAtIsNull(space, user).isPresent());
    }

    private void validateUserNull(AppUser user) {
        if (user == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
    }
}
