package ru.yandex.practicum.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.notification.dto.OperationRequest;
import ru.yandex.practicum.notification.entity.OperationEntity;
import ru.yandex.practicum.notification.repository.OperationRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private OperationRepository operationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<OperationEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<List<OperationEntity>> listCaptor;

    @Test
    void saveOperation() {
        var request = new OperationRequest("testuser", "CASH_PUT", "Deposit 500", 500);

        notificationService.saveOperation(request);

        verify(operationRepository).save(entityCaptor.capture());
        OperationEntity saved = entityCaptor.getValue();
        assertEquals("testuser", saved.getLogin());
        assertEquals("CASH_PUT", saved.getType());
        assertEquals("Deposit 500", saved.getMessage());
        assertEquals(500, saved.getAmount());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void saveOperations() {
        var requests = List.of(
                new OperationRequest("user1", "TRANSFER_SENT", "Sent 200", 200),
                new OperationRequest("user2", "TRANSFER_RECEIVED", "Received 200", 200)
        );

        notificationService.saveOperations(requests);

        verify(operationRepository).saveAll(listCaptor.capture());
        List<OperationEntity> entities = listCaptor.getValue();
        assertEquals(2, entities.size());
        assertEquals("user1", entities.get(0).getLogin());
        assertEquals("TRANSFER_SENT", entities.get(0).getType());
        assertEquals("user2", entities.get(1).getLogin());
    }
}
