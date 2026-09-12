package com.forgather.global.outbox;

import static com.forgather.global.outbox.OutboxStatus.PENDING;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forgather.global.exception.BaseException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Outbox save(OutboxType type, Object payload) {
        Outbox outbox = Outbox.pending(type, serialize(payload));
        return outboxRepository.save(outbox);
    }

    public List<Outbox> findPendingTasks(OutboxType type) {
        return outboxRepository.findByTypeAndStatus(type, PENDING);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BaseException("outbox payload 직렬화에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
