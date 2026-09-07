package ru.yandex.practicum.notification.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.notification.dto.OperationRequest;
import ru.yandex.practicum.notification.entity.OperationEntity;
import ru.yandex.practicum.notification.repository.OperationRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private OperationRepository operationRepository;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<OperationEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<List<OperationEntity>> listCaptor;

    @Test
    void saveOperation() {
        var request = new OperationRequest("testuser", "CASH_PUT", "Deposit 500", BigDecimal.valueOf(500));

        notificationService.saveOperation(request);

        verify(operationRepository).save(entityCaptor.capture());
        OperationEntity saved = entityCaptor.getValue();
        assertEquals("testuser", saved.getLogin());
        assertEquals("CASH_PUT", saved.getType());
        assertEquals("Deposit 500", saved.getMessage());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(saved.getAmount()));
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void saveOperations() {
        var requests = List.of(
                new OperationRequest("user1", "TRANSFER_SENT", "Sent 200", BigDecimal.valueOf(200)),
                new OperationRequest("user2", "TRANSFER_RECEIVED", "Received 200", BigDecimal.valueOf(200))
        );

        notificationService.saveOperations(requests);

        verify(operationRepository).saveAll(listCaptor.capture());
        List<OperationEntity> entities = listCaptor.getValue();
        assertEquals(2, entities.size());
        assertEquals("user1", entities.get(0).getLogin());
        assertEquals("TRANSFER_SENT", entities.get(0).getType());
        assertEquals("user2", entities.get(1).getLogin());
    }

    @Test
    void saveOperation_failure_recordsMetricByLogin() {
        var request = new OperationRequest("testuser", "CASH_PUT", "Deposit 500", BigDecimal.valueOf(500));
        when(operationRepository.save(org.mockito.ArgumentMatchers.any(OperationEntity.class)))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class, () -> notificationService.saveOperation(request));

        assertEquals(1.0, saveFailedCounter("testuser"), 0.0001);
    }

    @Test
    void saveOperations_failure_recordsMetricPerLogin() {
        var requests = List.of(
                new OperationRequest("user1", "TRANSFER_SENT", "Sent 200", BigDecimal.valueOf(200)),
                new OperationRequest("user2", "TRANSFER_RECEIVED", "Received 200", BigDecimal.valueOf(200)),
                new OperationRequest("user2", "TRANSFER_RECEIVED", "Received 100", BigDecimal.valueOf(100))
        );
        when(operationRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class, () -> notificationService.saveOperations(requests));

        assertEquals(1.0, saveFailedCounter("user1"), 0.0001);
        assertEquals(2.0, saveFailedCounter("user2"), 0.0001);
    }

    private double saveFailedCounter(String login) {
        return meterRegistry.counter("notification.save.failed", "login", login).count();
    }
}
