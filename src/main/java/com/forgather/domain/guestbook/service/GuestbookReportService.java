package com.forgather.domain.guestbook.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forgather.domain.guestbook.dto.CreateGuestBookReportRequest;
import com.forgather.domain.guestbook.dto.CreateGuestBookReportResponse;
import com.forgather.domain.guestbook.dto.ReportDetailResponse;
import com.forgather.domain.guestbook.dto.ReportHistoryResponse;
import com.forgather.domain.guestbook.model.GuestBookCard;
import com.forgather.domain.guestbook.model.GuestBookReport;
import com.forgather.domain.guestbook.model.ReporterType;
import com.forgather.domain.guestbook.repository.GuestBookCardRepository;
import com.forgather.domain.guestbook.repository.GuestBookReportRepository;
import com.forgather.domain.host.model.Host;
import com.forgather.domain.space.model.Space;
import com.forgather.domain.space.repository.SpaceHostRepository;
import com.forgather.domain.space.repository.SpaceRepository;
import com.forgather.global.exception.ConflictException;
import com.forgather.global.exception.ForbiddenException;
import com.forgather.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class GuestbookReportService {

    private final SpaceRepository spaceRepository;
    private final SpaceHostRepository spaceHostRepository;
    private final GuestBookCardRepository guestBookCardRepository;
    private final GuestBookReportRepository guestBookReportRepository;

    @Transactional(readOnly = true)
    public ReportHistoryResponse retrieveReportHistory(Host reporter, Pageable pageable) {
        Page<GuestBookReport> reports = guestBookReportRepository.findAllByReporter(
            reporter,
            pageable
        );
        return ReportHistoryResponse.from(reports);
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse retrieveReportDetail(Host reporter, Long reportId) {
        GuestBookReport report = guestBookReportRepository.getByIdOrThrow(reportId);
        if (!report.equalsReporter(reporter)) {
            throw new ForbiddenException("해당 신고 내역에 대한 접근 권한이 없습니다. reportId: " + report.getId());
        }
        return ReportDetailResponse.from(report);
    }

    @Transactional
    public CreateGuestBookReportResponse report(
        Host host,
        String spaceCode,
        Long guestBookCardId,
        CreateGuestBookReportRequest request
    ) {
        Space space = spaceRepository.getByCodeAndDeletedAtIsNullOrThrow(spaceCode);
        validateSpaceHost(host, space);  // 스페이스의 호스트만 신고 가능

        GuestBookCard card = getCardBySpace(guestBookCardId, space);
        validateAlreadyReported(host, card);  // 동일 사용자의 중복 신고 불가능

        GuestBookReport report = guestBookReportRepository.save(
            new GuestBookReport(card, host, host, ReporterType.HOST, request.reason(), request.detail())
        );
        card.hideByAdmin();
        return CreateGuestBookReportResponse.from(report);
    }

    private GuestBookCard getCardBySpace(Long guestBookCardId, Space space) {
        GuestBookCard card = guestBookCardRepository.getByIdAndDeletedAtIsNullOrThrow(guestBookCardId);
        if (!card.equalsSpace(space)) {
            throw new NotFoundException("존재하지 않는 방명록 카드입니다. guestBookCardId: %d".formatted(guestBookCardId));
        }
        return card;
    }

    private void validateAlreadyReported(Host host, GuestBookCard card) {
        if (guestBookReportRepository.existsByGuestBookCardAndReporter(card, host)) {
            throw new ConflictException("이미 신고 접수된 방명록입니다. guestBookCardId: %d, reporterUserId: %d"
                .formatted(card.getId(), host.getId()));
        }
    }

    private void validateSpaceHost(Host host, Space space) {
        if (spaceHostRepository.findBySpaceAndHostAndDeletedAtIsNull(space, host).isPresent()) {
            return;
        }
        throw new ForbiddenException(
            "해당 스페이스에 대한 접근 권한이 없습니다. spaceCode: %s, hostId: %d"
                .formatted(space.getCode(), host.getId())
        );
    }
}
