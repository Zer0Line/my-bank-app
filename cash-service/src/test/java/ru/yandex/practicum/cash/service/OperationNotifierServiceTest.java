package ru.yandex.practicum.cash.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.OperationRequest;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationNotifierServiceTest {

    @Mock
    private KafkaTemplate<String, OperationRequest> kafkaTemplate;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Captor
    private ArgumentCaptor<OperationRequest> requestCaptor;

    @InjectMocks
    private OperationNotifierService operationNotifierService;

    @Test
    void notifyOperationStarted() {
        when(kafkaTemplate.send(eq("cash-request"), any(OperationRequest.class)))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        operationNotifierService.notifyOperationStarted("testuser", CashAction.PUT, BigDecimal.valueOf(500));

        verify(kafkaTemplate).send(eq("cash-request"), requestCaptor.capture());
        assertEquals(new OperationRequest("testuser", "CASH_PUT",
                "Cash operation started via cash-service", BigDecimal.valueOf(500)), requestCaptor.getValue());
    }

    @Test
    void notifyOperationStartedIncrementsMetricOnFailure() {
        when(kafkaTemplate.send(eq("cash-request"), any(OperationRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka down")));
        when(meterRegistry.counter("operation.notification.failed", "topic", "cash-request"))
                .thenReturn(counter);

        operationNotifierService.notifyOperationStarted("testuser", CashAction.PUT, BigDecimal.valueOf(500));

        verify(meterRegistry).counter("operation.notification.failed", "topic", "cash-request");
        verify(counter).increment();
    }
}
