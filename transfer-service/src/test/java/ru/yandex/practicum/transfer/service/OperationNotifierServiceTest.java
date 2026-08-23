package ru.yandex.practicum.transfer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.transfer.dto.OperationRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationNotifierServiceTest {

    @Mock
    private KafkaTemplate<String, OperationRequest> kafkaTemplate;

    @Captor
    private ArgumentCaptor<OperationRequest> requestCaptor;

    @InjectMocks
    private OperationNotifierService operationNotifierService;

    @Test
    void notifyOperationStarted() {
        operationNotifierService.notifyOperationStarted("sender", "recipient", BigDecimal.valueOf(300));

        verify(kafkaTemplate, times(2)).send(eq("transfer-request"), requestCaptor.capture());
        List<OperationRequest> sent = requestCaptor.getAllValues();

        assertEquals(new OperationRequest("sender", "TRANSFER_SENT",
                "Transfer to recipient started", BigDecimal.valueOf(300)), sent.get(0));
        assertEquals(new OperationRequest("recipient", "TRANSFER_RECEIVED",
                "Transfer from sender started", BigDecimal.valueOf(300)), sent.get(1));
    }
}
