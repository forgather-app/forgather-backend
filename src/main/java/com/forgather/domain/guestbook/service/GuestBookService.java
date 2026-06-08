package com.forgather.domain.guestbook.service;

import static com.forgather.domain.guestbook.model.VisibilityStatus.VISIBLE;
import static com.forgather.domain.upload.domain.FilePathGenerator.generateContentsFilePath;
import static com.forgather.domain.upload.domain.UploadCategory.GUESTBOOK;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.guestbook.dto.DeleteGuestBookCardPhotosRequest;
import com.forgather.domain.guestbook.dto.GuestBookCardResponse;
import com.forgather.domain.guestbook.dto.GuestBookCardSimpleResponse;
import com.forgather.domain.guestbook.dto.GuestBookResponse;
import com.forgather.domain.guestbook.dto.WriteGuestBookCardPhotoRequest;
import com.forgather.domain.guestbook.dto.WriteGuestBookCardRequest;
import com.forgather.domain.guestbook.dto.WriteGuestBookCardResponse;
import com.forgather.domain.guestbook.exception.GuestbookCardNotReadableException;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookCardPhoto;
import com.forgather.domain.guestbook.model.GuestBookCardPhotos;
import com.forgather.domain.guestbook.repository.GuestBookCardPhotoRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.global.auth.model.Host;
import com.forgather.global.auth.repository.SpaceHostRepository;
import com.forgather.global.exception.BaseNullPointerException;
import com.forgather.global.exception.ForbiddenException;
import com.forgather.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class GuestBookService {

    private final SpaceRepository spaceRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final GuestBookCardRepository guestBookCardRepository;
    private final GuestBookCardPhotoRepository guestBookCardPhotoRepository;
    private final ContentsStorage contentsStorage;

    @Transactional
    public WriteGuestBookCardResponse writeCard(String spaceCode, WriteGuestBookCardRequest request) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        GuestBookCard guestBookCard = guestBookCardRepository.save(request.toEntity(space));

        GuestBookCardPhotos guestBookCardPhotos = getGuestBookCardPhotos(spaceCode, request, guestBookCard);
        guestBookCardPhotoRepository.saveAll(guestBookCardPhotos.getAll());

        return new WriteGuestBookCardResponse(guestBookCard, guestBookCardPhotos.getAll());
    }

    private GuestBookCardPhotos getGuestBookCardPhotos(
        String spaceCode,
        WriteGuestBookCardRequest request,
        GuestBookCard guestBookCard
    ) {
        List<GuestBookCardPhoto> photos = new ArrayList<>();
        for (WriteGuestBookCardPhotoRequest photoRequest : request.photos()) {
            String path = generateContentsFilePath(
                contentsStorage.getRootDirectory(),
                spaceCode,
                GUESTBOOK,
                photoRequest.uploadFileName()
            );
            GuestBookCardPhoto photo = photoRequest.toEntity(path, guestBookCard);
            photos.add(photo);
        }
        return new GuestBookCardPhotos(photos);
    }

    /**
     *  VISIBLE 상태인 방명록 조회
     * - 스페이스 호스트: isRead=true인 방명록만 조회, isRead=false인 방명록 개수 응답
     * - 게스트: 모든 방명록 조회, 안읽은 방명록 개수 응답x
     */
    @Transactional(readOnly = true)
    public GuestBookResponse read(Host host, String spaceCode, Pageable pageable) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        boolean isSpaceHost = host != null && isSpaceHost(space, host);
        validateCanRead(space, isSpaceHost);

        if (isSpaceHost) { // 스페이스 호스트: 읽은 방명록만 조회, 안읽은 방명록 개수 응답
            Page<GuestBookCardSimpleResponse> readResponses =
                guestBookCardRepository.findAllDtoBySpaceAndVisibilityStatusAndIsReadAndDeletedAtIsNull(
                    space,
                    VISIBLE,
                    true,
                    pageable
                ).map(GuestBookCardSimpleResponse::from);

            long newCount = guestBookCardRepository.countBySpaceAndVisibilityStatusAndIsReadAndDeletedAtIsNull(
                space,
                VISIBLE,
                false
            );

            return new GuestBookResponse(readResponses, newCount);
        }

        // 방문객: 읽음 여부와 상관 없이 모두 조회, 안읽은 방명록 개수 응답 x
        Page<GuestBookCardSimpleResponse> simpleResponses =
            guestBookCardRepository.findAllDtoBySpaceAndVisibilityStatusAndDeletedAtIsNull(
                space,
                VISIBLE,
                pageable
            ).map(GuestBookCardSimpleResponse::from);

        return new GuestBookResponse(simpleResponses);
    }

    /**
     * 스페이스 호스트인 경우에만 isRead=false인 방명록 조회
     */
    @Transactional(readOnly = true)
    public GuestBookResponse readUnread(Host host, String spaceCode, Pageable pageable) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(host, space);

        Page<GuestBookCardSimpleResponse> unreadResponses =
            guestBookCardRepository.findAllDtoBySpaceAndVisibilityStatusAndIsReadAndDeletedAtIsNull(
                space,
                VISIBLE,
                false,
                pageable
            ).map(GuestBookCardSimpleResponse::from);

        return new GuestBookResponse(unreadResponses);
    }

    @Transactional
    public GuestBookCardResponse readCard(Host host, String spaceCode, Long guestBookCardId) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        boolean isSpaceHost = host != null && isSpaceHost(space, host);
        validateCanRead(space, isSpaceHost);

        GuestBookCard guestBookCard = getGuestBookCard(guestBookCardId, space);
        try {
            guestBookCard.read(isSpaceHost);
        } catch (GuestbookCardNotReadableException e) {
            throw new NotFoundException("존재하지 않는 방명록 카드입니다. guestBookCardId: %d".formatted(guestBookCardId));
        }

        List<GuestBookCardPhoto> photos = guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(
            guestBookCard
        );
        return new GuestBookCardResponse(guestBookCard, photos);
    }

    private void validateCanRead(Space space, boolean isSpaceHost) {
        if (space.isPublic()) { // 공개 스페이스
            return;
        }
        if (isSpaceHost) { // 스페이스 호스트
            return;
        }
        throw new ForbiddenException("방문자는 비공개 스페이스의 방명록을 조회할 수 없습니다. spaceCode: " + space.getCode());
    }

    @Transactional
    public void deleteAllCardsBySpace(Host host, Space space) {
        validateSpaceHost(host, space);
        for (GuestBookCard guestBookCard : guestBookCardRepository.findAllBySpaceAndDeletedAtIsNull(space)) {
            deleteCard(host, space.getCode(), guestBookCard.getId());
        }
    }

    @Transactional
    public void deleteCard(Host host, String spaceCode, Long guestBookCardId) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(host, space);
        GuestBookCard guestBookCard = getGuestBookCard(guestBookCardId, space);
        deleteGuestBookCardPhotos(guestBookCard);
        guestBookCard.delete();
    }

    private void deleteGuestBookCardPhotos(GuestBookCard guestBookCard) {
        List<GuestBookCardPhoto> photos = guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(
            guestBookCard);
        deleteGuestBookCardPhotos(photos);
    }

    @Transactional
    public void deleteCardPhotos(
        Host host,
        String spaceCode,
        Long guestBookCardId,
        DeleteGuestBookCardPhotosRequest request
    ) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(host, space);
        GuestBookCardPhotos guestBookCardPhotos = getGuestBookCardPhotos(space, guestBookCardId);
        List<GuestBookCardPhoto> deletedPhotos = guestBookCardPhotos.deleteByIds(request.deletePhotoIds());
        deleteGuestBookCardPhotos(deletedPhotos);
    }

    private GuestBookCardPhotos getGuestBookCardPhotos(Space space, Long guestBookCardId) {
        GuestBookCard guestBookCard = getGuestBookCard(guestBookCardId, space);
        return new GuestBookCardPhotos(
            guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(guestBookCard));
    }

    private GuestBookCard getGuestBookCard(Long guestBookCardId, Space space) {
        GuestBookCard guestBookCard = guestBookCardRepository.getByIdAndDeletedAtIsNullOrThrow(guestBookCardId);
        if (guestBookCard.equalsSpace(space)) {
            return guestBookCard;
        }
        throw new NotFoundException("존재하지 않는 방명록 카드입니다. guestBookCardId: %d".formatted(guestBookCardId));
    }

    private void validateSpaceHost(Host host, Space space) {
        if (isSpaceHost(space, host)) {
            return;
        }
        throw new ForbiddenException(
            "해당 스페이스에 대한 접근 권한이 없습니다. spaceCode: %s, hostId: %d".formatted(space.getCode(), host.getId())
        );
    }

    private boolean isSpaceHost(Space space, Host host) {
        if (space == null || host == null) {
            throw new BaseNullPointerException("스페이스와 호스트는 null일 수 없습니다.", INTERNAL_SERVER_ERROR);
        }
        return spaceHostRepository.findBySpaceAndHostAndDeletedAtIsNull(space, host).isPresent();
    }

    private void deleteGuestBookCardPhotos(List<GuestBookCardPhoto> photos) {
        for (GuestBookCardPhoto photo : photos) {
            photo.delete();
        }
    }
}
