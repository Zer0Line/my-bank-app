package ru.yandex.practicum.accounts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.accounts.dto.CashAction;
import ru.yandex.practicum.accounts.entity.OutboxEventEntity;
import ru.yandex.practicum.accounts.entity.OutboxStatus;
import ru.yandex.practicum.accounts.repository.OutboxEventRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationNotifierServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OperationNotifierService operationNotifierService;

    @Test
    void notifyAccountUpdate() {
        operationNotifierService.notifyAccountUpdate("testuser");

        OutboxEventEntity event = captureEvent();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttempts());
        assertTrue(event.getPayload().contains("testuser"));
        assertTrue(event.getPayload().contains("ACCOUNT_UPDATE"));
    }

    @Test
    void notifyCashOperation() {
        operationNotifierService.notifyCashOperation("testuser", CashAction.PUT, BigDecimal.valueOf(300));

        OutboxEventEntity event = captureEvent();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertTrue(event.getPayload().contains("CASH_PUT"));
    }

    @Test
    void notifyTransfer() {
        operationNotifierService.notifyTransfer("sender", "recipient", BigDecimal.valueOf(200));

        OutboxEventEntity event = captureEvent();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertTrue(event.getPayload().contains("TRANSFER_SENT"));
        assertTrue(event.getPayload().contains("TRANSFER_RECEIVED"));
    }

    private OutboxEventEntity captureEvent() {
        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(captor.capture());
        return captor.getValue();
    }
}