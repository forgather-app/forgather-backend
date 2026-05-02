package com.forgather.domain.guestbook.service;

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
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookCardPhoto;
import com.forgather.domain.guestbook.model.GuestBookCardPhotos;
import com.forgather.domain.guestbook.repository.GuestBookCardPhotoRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.guestbook.repository.dto.GuestBookCardListDto;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.domain.upload.domain.ContentsStorage;
import com.forgather.global.auth.model.AppUser;
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

    @Transactional(readOnly = true)
    public GuestBookResponse read(AppUser user, String spaceCode, Pageable pageable) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateCanRead(space, user);
        boolean isHost = user != null && isSpaceHost(space, user);
        Page<GuestBookCardListDto> guestBookCardDtos = guestBookCardRepository.findAllDtoBySpaceAndDeletedAtIsNull(space, pageable);
        Page<GuestBookCardSimpleResponse> simpleResponses = guestBookCardDtos.map(
            guestBookCardDto -> new GuestBookCardSimpleResponse(
                guestBookCardDto.id(),
                guestBookCardDto.nickname(),
                guestBookCardDto.isPhotoExists(),
                isHost ? guestBookCardDto.isRead() : null
            )
        );
        return new GuestBookResponse(simpleResponses);
    }

    @Transactional
    public GuestBookCardResponse readCard(AppUser user, String spaceCode, Long guestBookCardId) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateCanRead(space, user);

        GuestBookCard guestBookCard = getGuestBookCard(guestBookCardId, space);
        if (user != null && isSpaceHost(space, user)) {
            guestBookCard.read();
        }

        List<GuestBookCardPhoto> photos = guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(guestBookCard);
        return new GuestBookCardResponse(guestBookCard, photos);
    }

    private void validateCanRead(Space space, AppUser user) {
        if (space.isPublic()) { // 공개 스페이스
            return;
        }
        if (user != null && isSpaceHost(space, user)) { // 스페이스 호스트
            return;
        }
        throw new ForbiddenException("방문자는 비공개 스페이스의 방명록을 조회할 수 없습니다. spaceCode: " + space.getCode());
    }

    @Transactional
    public void deleteAllCardsBySpace(AppUser user, Space space) {
        validateSpaceHost(user, space);
        for (GuestBookCard guestBookCard : guestBookCardRepository.findAllBySpaceAndDeletedAtIsNull(space)) {
            deleteCard(user, space.getCode(), guestBookCard.getId());
        }
    }

    @Transactional
    public void deleteCard(AppUser user, String spaceCode, Long guestBookCardId) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(user, space);
        GuestBookCard guestBookCard = getGuestBookCard(guestBookCardId, space);
        deleteGuestBookCardPhotos(guestBookCard);
        guestBookCard.delete();
    }

    private void deleteGuestBookCardPhotos(GuestBookCard guestBookCard) {
        List<GuestBookCardPhoto> photos = guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(guestBookCard);
        deleteGuestBookCardPhotos(photos);
    }

    @Transactional
    public void deleteCardPhotos(
        AppUser user,
        String spaceCode,
        Long guestBookCardId,
        DeleteGuestBookCardPhotosRequest request
    ) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(user, space);
        GuestBookCardPhotos guestBookCardPhotos = getGuestBookCardPhotos(space, guestBookCardId);
        List<GuestBookCardPhoto> deletedPhotos = guestBookCardPhotos.deleteByIds(request.deletePhotoIds());
        deleteGuestBookCardPhotos(deletedPhotos);
    }

    private GuestBookCardPhotos getGuestBookCardPhotos(Space space, Long guestBookCardId) {
        GuestBookCard guestBookCard = getGuestBookCard(guestBookCardId, space);
        return new GuestBookCardPhotos(guestBookCardPhotoRepository.findAllByGuestBookCardAndDeletedAtIsNull(guestBookCard));
    }

    private GuestBookCard getGuestBookCard(Long guestBookCardId, Space space) {
        GuestBookCard guestBookCard = guestBookCardRepository.getByIdAndDeletedAtIsNullOrThrow(guestBookCardId);
        if (guestBookCard.equalsSpace(space)) {
            return guestBookCard;
        }
        throw new NotFoundException(
            "해당 스페이스에 존재하지 않는 방명록 카드입니다. spaceCode: %s, guestBookCardId: %d"
                .formatted(space.getCode(), guestBookCardId)
        );
    }

    private void validateSpaceHost(AppUser user, Space space) {
        if (isSpaceHost(space, user)) {
            return;
        }
        throw new ForbiddenException(
            "해당 스페이스에 대한 접근 권한이 없습니다. spaceCode: %s, hostId: %d".formatted(space.getCode(), user.getId())
        );
    }

    private boolean isSpaceHost(Space space, AppUser user) {
        if (space == null || user == null) {
            throw new BaseNullPointerException("스페이스와 유저는 null일 수 없습니다.", INTERNAL_SERVER_ERROR);
        }
        return spaceHostRepository.findBySpaceAndAppUserAndDeletedAtIsNull(space, user).isPresent();
    }

    private void deleteGuestBookCardPhotos(List<GuestBookCardPhoto> photos) {
        for (GuestBookCardPhoto photo : photos) {
            photo.delete();
        }
    }
}
