package com.forgather.domain.export.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.export.config.SpacePdfExportProperties;
import com.forgather.domain.export.dto.ExportSpacePdfRequest;
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
import com.forgather.fixture.HostFixture;
import com.forgather.fixture.SpaceFixture;
import com.forgather.global.exception.NotFoundException;
import com.forgather.global.exception.UnauthorizedException;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxService;
import com.forgather.global.outbox.OutboxType;

@ExtendWith(MockitoExtension.class)
class SpacePdfExportServiceTest {

    @Mock private SpaceRepository spaceRepository;
    @Mock private SpaceHostRepository spaceHostRepository;
    @Mock private GuestBookCardRepository guestBookCardRepository;
    @Mock private GuestBookCardPhotoRepository guestBookCardPhotoRepository;
    @Mock private SpacePdfExportRepository spacePdfExportRepository;
    @Mock private OutboxService outboxService;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Host host = HostFixture.createHostWithName("주최자");
    private final Space space = SpaceFixture.createSpaceWithCodeAndName("abcdefghij", "졸업 전시");
    private final ExportSpacePdfRequest request = new ExportSpacePdfRequest("reader@example.com");
    private SpacePdfExportService spacePdfExportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(space, "id", 1L);
        spacePdfExportService = new SpacePdfExportService(
            spaceRepository, spaceHostRepository, guestBookCardRepository, guestBookCardPhotoRepository,
            spacePdfExportRepository, new SpacePdfExportProperties("pdf", "ap-northeast-2"),
            objectMapper, outboxService, applicationEventPublisher);
    }

    @DisplayName("방명록별 사진 경로를 스냅샷에 저장하고 같은 작업 ID로 아웃박스를 등록한다")
    @Test
    void savesSnapshotAndRequestsDispatch() throws Exception {
        when(spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(space.getCode())).thenReturn(space);
        LocalDateTime time = LocalDateTime.of(2026, 11, 2, 12, 30, 15);
        when(guestBookCardRepository.findArchiveCards(1L)).thenReturn(List.of(
            new GuestBookArchiveCardDto(8L, "첫째", "축하해요", time),
            new GuestBookArchiveCardDto(3L, "둘째", "잘 봤어요", time.plusMinutes(1))));
        when(guestBookCardPhotoRepository.findArchivePhotos(List.of(8L, 3L))).thenReturn(List.of(
            new GuestBookArchivePhotoDto(3L, "/한 글?#%.jpg"), new GuestBookArchivePhotoDto(3L, "두번째.jpg")));
        Outbox outbox = mock(Outbox.class);
        when(outbox.getId()).thenReturn(42L);
        when(outboxService.save(eq(OutboxType.SPACE_PDF_EXPORT), any(SpacePdfExportPayload.class))).thenReturn(outbox);

        String jobId = spacePdfExportService.request(space.getCode(), host, request).jobId();

        ArgumentCaptor<SpacePdfExport> saved = ArgumentCaptor.forClass(SpacePdfExport.class);
        verify(spacePdfExportRepository).save(saved.capture());
        assertThat(UUID.fromString(jobId).toString()).isEqualTo(jobId);
        assertThat(saved.getValue().getJobId()).isEqualTo(jobId);
        assertThat(saved.getValue().getEmail()).isEqualTo(request.email());
        assertThat(objectMapper.readTree(saved.getValue().getSnapshot())).isEqualTo(objectMapper.readTree("""
            {"spaceName":"졸업 전시","nickname":"주최자","guestbookInfos":[
                {"author":"첫째","message":"축하해요","createdAt":"2026-11-02T12:30:15","photoPaths":[]},
                {"author":"둘째","message":"잘 봤어요","createdAt":"2026-11-02T12:31:15",
                 "photoPaths":["/한 글?#%.jpg","두번째.jpg"]}
            ]}
            """));
        verify(outboxService).save(OutboxType.SPACE_PDF_EXPORT, new SpacePdfExportPayload(jobId));
        verify(applicationEventPublisher).publishEvent(new SpacePdfExportRequested(42L));
    }

    @DisplayName("방명록이 없는 스페이스도 매 요청마다 새 작업 ID로 추출한다")
    @Test
    void createsFreshJobForEachEmptySpaceRequest() throws Exception {
        when(spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(space.getCode())).thenReturn(space);
        when(guestBookCardRepository.findArchiveCards(1L)).thenReturn(List.of());
        when(outboxService.save(eq(OutboxType.SPACE_PDF_EXPORT), any(SpacePdfExportPayload.class)))
            .thenReturn(mock(Outbox.class));

        String first = spacePdfExportService.request(space.getCode(), host, request).jobId();
        String second = spacePdfExportService.request(space.getCode(), host, request).jobId();

        assertThat(second).isNotEqualTo(first);
        ArgumentCaptor<SpacePdfExport> saved = ArgumentCaptor.forClass(SpacePdfExport.class);
        verify(spacePdfExportRepository, times(2)).save(saved.capture());
        assertThat(objectMapper.readTree(saved.getValue().getSnapshot()).get("guestbookInfos"))
            .isEqualTo(objectMapper.createArrayNode());
        verifyNoInteractions(guestBookCardPhotoRepository);
    }

    @DisplayName("로그인하지 않은 사용자가 추출을 요청하면 UnauthorizedException을 던진다")
    @Test
    void rejectsAnonymousRequest() {
        assertThatThrownBy(() -> spacePdfExportService.request(space.getCode(), null, request))
            .isInstanceOf(UnauthorizedException.class);
    }

    @DisplayName("다른 호스트의 스페이스 추출을 요청하면 NotFoundException을 던진다")
    @Test
    void rejectsNonOwner() {
        when(spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(space.getCode())).thenReturn(space);
        when(spaceHostRepository.getBySpaceAndHostAndDeletedAtIsNullOrThrow(space, host))
            .thenThrow(new NotFoundException("존재하지 않는 자원입니다."));

        assertThatThrownBy(() -> spacePdfExportService.request(space.getCode(), host, request))
            .isInstanceOf(NotFoundException.class);
    }
}
