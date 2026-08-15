package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.cash.client.NotificationServiceClient;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.OperationRequest;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationNotifierServiceTest {

    @Mock
    private NotificationServiceClient notificationClient;

    @InjectMocks
    private OperationNotifierService operationNotifierService;

    @Test
    void notifyOperationStarted() {
        operationNotifierService.notifyOperationStarted("testuser", CashAction.PUT, BigDecimal.valueOf(500));

        verify(notificationClient).saveOperation(new OperationRequest(
                "testuser", "CASH_PUT", "Cash operation started via cash-service", BigDecimal.valueOf(500)));
    }
}