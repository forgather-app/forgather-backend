package com.forgather.domain.export.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.export.config.SpacePdfExportProperties;
import com.forgather.domain.export.dto.ExportSpacePdfRequest;
import com.forgather.domain.export.dto.ExportSpacePdfResponse;
import com.forgather.domain.export.dto.SpaceArchive;
import com.forgather.domain.export.dto.SpacePdfExportPayload;
import com.forgather.domain.export.event.SpacePdfExportRequested;
import com.forgather.domain.export.model.SpacePdfExport;
import com.forgather.domain.export.repository.SpacePdfExportRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardPhotoRepository;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.guestbook.repository.dto.GuestBookArchiveCardDto;
import com.forgather.domain.guestbook.repository.dto.GuestBookArchivePhotoDto;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.global.exception.BaseException;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated
@RequiredArgsConstructor
public class SpacePdfExportService {

    private final SpaceRepository spaceRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final GuestBookCardRepository cardRepository;
    private final GuestBookCardPhotoRepository photoRepository;
    private final SpacePdfExportRepository exportRepository;
    private final SpacePdfExportProperties properties;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ExportSpacePdfResponse request(String spaceCode, Host host, @Valid ExportSpacePdfRequest request) {
        if (host == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        spaceHostRepository.getBySpaceAndHostAndDeletedAtIsNullOrThrow(space, host);
        properties.validateConfigured();
        SpaceArchive archive = createArchive(space, host);
        String jobId = UUID.randomUUID().toString();
        exportRepository.save(new SpacePdfExport(jobId, serialize(archive), request.email()));
        Outbox outbox = outboxService.save(OutboxType.SPACE_PDF_EXPORT, new SpacePdfExportPayload(jobId));
        eventPublisher.publishEvent(new SpacePdfExportRequested(outbox.getId()));
        return new ExportSpacePdfResponse(jobId);
    }

    private SpaceArchive createArchive(Space space, Host host) {
        List<GuestBookArchiveCardDto> cards = cardRepository.findArchiveCards(space.getId());
        Map<Long, List<String>> photoPaths = findPhotoPaths(cards);
        List<SpaceArchive.GuestbookInfo> guestbooks = cards.stream()
            .map(card -> new SpaceArchive.GuestbookInfo(card.nickname(), card.message(),
                card.createdAt().toString(), photoPaths.getOrDefault(card.id(), List.of())))
            .toList();
        return new SpaceArchive(space.getName(), host.getNickname(), guestbooks);
    }

    private Map<Long, List<String>> findPhotoPaths(List<GuestBookArchiveCardDto> cards) {
        if (cards.isEmpty()) {
            return Map.of();
        }
        List<Long> cardIds = cards.stream().map(GuestBookArchiveCardDto::id).toList();
        return photoRepository.findArchivePhotos(cardIds).stream()
            .collect(Collectors.groupingBy(GuestBookArchivePhotoDto::cardId,
                Collectors.mapping(GuestBookArchivePhotoDto::path, Collectors.toList())));
    }

    private String serialize(SpaceArchive archive) {
        try {
            return objectMapper.writeValueAsString(archive);
        } catch (JsonProcessingException e) {
            throw new BaseException("PDF 스냅샷 직렬화에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
