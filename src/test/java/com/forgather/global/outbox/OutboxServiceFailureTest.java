package com.forgather.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class OutboxServiceFailureTest {

    @Mock private OutboxHandler handler;
    @Mock private OutboxStateService stateService;
    @Mock private OutboxRepository repository;

    private final Logger logger = (Logger)LoggerFactory.getLogger(OutboxService.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private OutboxService service;
    private Outbox first;
    private Outbox second;

    @BeforeEach
    void setUp() {
        logs.start();
        logger.addAppender(logs);
        first = task(1L);
        second = task(2L);
        service = new OutboxService(List.of(handler), stateService, repository, new ObjectMapper());
        when(stateService.findProcessable()).thenReturn(List.of(first, second));
        when(handler.supports(OutboxType.SOCIAL_REVOKE)).thenReturn(true);
        when(handler.getLeaseDurationSeconds()).thenReturn(20L);
        when(stateService.processing(anyLong(), eq(20L))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logs.stop();
    }

    private Outbox task(Long id) {
        Outbox outbox = Outbox.pending(OutboxType.SOCIAL_REVOKE, "payload");
        ReflectionTestUtils.setField(outbox, "id", id);
        return outbox;
    }

    @DisplayName("상태 저장 실패는 오류 로그만 남기고 재시도나 실패 횟수 증가 없이 다음 작업을 처리한다")
    @ParameterizedTest
    @CsvSource({"COMPLETED, 1, 0, 0", "CANCELED, 0, 0, 1", "FAILED, 0, 1, 0"})
    void logsStateFailureWithoutRetry(OutboxTaskResult taskResult, int succeeded, int failed, int canceled) {
        // given
        when(handler.process(any(Outbox.class))).thenReturn(taskResult);
        IllegalStateException failure = new IllegalStateException("상태 저장 실패");
        switch (taskResult) {
            case COMPLETED -> {
                when(stateService.complete(1L)).thenThrow(failure);
                when(stateService.complete(2L)).thenReturn(true);
            }
            case CANCELED -> {
                when(stateService.cancel(1L)).thenThrow(failure);
                when(stateService.cancel(2L)).thenReturn(true);
            }
            case FAILED -> {
                when(stateService.fail(1L)).thenThrow(failure);
                when(stateService.fail(2L)).thenReturn(true);
            }
        }

        // when
        OutboxProcessResult result = service.process();

        // then
        assertAll(
            () -> assertThat(result).isEqualTo(new OutboxProcessResult(succeeded, failed, canceled)),
            () -> verify(stateService, never()).retry(anyLong(), anyInt()),
            () -> verify(handler).process(second),
            () -> assertThat(logs.list).anySatisfy(event -> assertAll(
                () -> assertThat(event.getLevel()).isEqualTo(Level.ERROR),
                () -> assertThat(event.getFormattedMessage()).contains("outboxId: 1")
            ))
        );
    }

    @DisplayName("핸들러 실행 실패는 기존대로 재시도 처리하고 다음 작업을 계속 실행한다")
    @Test
    void retriesHandlerFailure() {
        // given
        when(handler.process(first)).thenThrow(new IllegalStateException("외부 호출 실패"));
        when(handler.process(second)).thenReturn(OutboxTaskResult.COMPLETED);
        when(handler.getMaxFailCount()).thenReturn(5);
        when(stateService.retry(1L, 5)).thenReturn(true);
        when(stateService.complete(2L)).thenReturn(true);

        // when
        OutboxProcessResult result = service.process();

        // then
        assertAll(
            () -> assertThat(result).isEqualTo(new OutboxProcessResult(1, 1, 0)),
            () -> verify(stateService).retry(1L, 5),
            () -> verify(stateService, never()).complete(1L),
            () -> verify(handler).process(second)
        );
    }
}
