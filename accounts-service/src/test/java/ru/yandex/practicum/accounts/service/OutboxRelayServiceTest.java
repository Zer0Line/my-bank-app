package ru.yandex.practicum.accounts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.practicum.accounts.dto.OperationRequest;
import ru.yandex.practicum.accounts.entity.OutboxEventEntity;
import ru.yandex.practicum.accounts.entity.OutboxStatus;
import ru.yandex.practicum.accounts.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, OperationRequest> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        outboxRelayService = new OutboxRelayService(outboxEventRepository, kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(outboxRelayService, "batchSize", 100);
    }

    @Test
    void publishPending_marksEventProcessedOnSuccess() throws Exception {
        OutboxEventEntity event = pendingEvent(objectMapper.writeValueAsString(List.of(
                new OperationRequest("testuser", "CASH_PUT", "Cash operation", BigDecimal.valueOf(300)))));
        when(outboxEventRepository.findPending(eq(OutboxStatus.PENDING), any(Pageable.class))).thenReturn(List.of(event));

        outboxRelayService.publishPending();

        verify(kafkaTemplate).send(eq("account-operations"), any(OperationRequest.class));
        assertEquals(OutboxStatus.PROCESSED, event.getStatus());
        assertNotNull(event.getProcessedAt());
        verify(outboxEventRepository).save(event);
    }

    @Test
    void publishPending_keepsEventPendingOnFailure() throws Exception {
        OutboxEventEntity event = pendingEvent(objectMapper.writeValueAsString(List.of(
                new OperationRequest("testuser", "CASH_PUT", "Cash operation", BigDecimal.valueOf(300)))));
        when(outboxEventRepository.findPending(eq(OutboxStatus.PENDING), any(Pageable.class))).thenReturn(List.of(event));
        doThrow(new RuntimeException("kafka unavailable"))
                .when(kafkaTemplate).send(any(), any());

        outboxRelayService.publishPending();

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertEquals("kafka unavailable", event.getErrorMessage());
        verify(outboxEventRepository).save(event);
    }

    private OutboxEventEntity pendingEvent(String payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setPayload(payload);
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(Instant.now());
        return event;
    }
}
