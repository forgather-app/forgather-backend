package com.forgather.domain.export.handler;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.domain.export.client.AsyncSpacePdfClient;
import com.forgather.domain.export.dto.SpacePdfExportPayload;
import com.forgather.global.outbox.Outbox;
import com.forgather.global.outbox.OutboxHandler;
import com.forgather.global.outbox.OutboxTaskResult;
import com.forgather.global.outbox.OutboxType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpacePdfExportHandler implements OutboxHandler {

    private final AsyncSpacePdfClient generationClient;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(OutboxType type) {
        return type == OutboxType.SPACE_PDF_EXPORT;
    }

    @Override
    public OutboxTaskResult process(Outbox outbox) {
        SpacePdfExportPayload payload;
        try {
            payload = objectMapper.readValue(outbox.getPayload(), SpacePdfExportPayload.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("PDF 추출 payload 변환 실패. outboxId: {}", outbox.getId());
            return OutboxTaskResult.FAILED;
        }
        if (payload == null || !StringUtils.hasText(payload.jobId())) {
            log.warn("PDF 추출 jobId가 없습니다. outboxId: {}", outbox.getId());
            return OutboxTaskResult.FAILED;
        }
        generationClient.generate(payload.jobId());
        return OutboxTaskResult.COMPLETED;
    }

    @Override
    public long getLeaseDurationSeconds() {
        return 30;
    }

    @Override
    public int getMaxFailCount() {
        return 5;
    }
}
