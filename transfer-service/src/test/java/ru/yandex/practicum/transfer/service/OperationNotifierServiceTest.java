package ru.yandex.practicum.transfer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.transfer.client.NotificationServiceClient;
import ru.yandex.practicum.transfer.dto.OperationRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationNotifierServiceTest {

    @Mock
    private NotificationServiceClient notificationClient;

    @InjectMocks
    private OperationNotifierService operationNotifierService;

    @Test
    void notifyOperationStarted() {
        operationNotifierService.notifyOperationStarted("sender", "recipient", BigDecimal.valueOf(300));

        verify(notificationClient).saveOperations(List.of(
                new OperationRequest("sender", "TRANSFER_SENT",
                        "Transfer to recipient started", BigDecimal.valueOf(300)),
                new OperationRequest("recipient", "TRANSFER_RECEIVED",
                        "Transfer from sender started", BigDecimal.valueOf(300))
        ));
    }
}