package ru.yandex.practicum.transfer.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.dto.OperationRequest;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationNotifierService {

    private static final String TOPIC = "transfer-request";
    private static final String NOTIFICATION_FAILED_METRIC = "operation.notification.failed";

    private final KafkaTemplate<String, OperationRequest> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public void notifyOperationStarted(String senderLogin, String recipientLogin, BigDecimal value) {
        List<OperationRequest> requests = List.of(
                new OperationRequest(senderLogin, "TRANSFER_SENT",
                        "Transfer to " + recipientLogin + " started", value),
                new OperationRequest(recipientLogin, "TRANSFER_RECEIVED",
                        "Transfer from " + senderLogin + " started", value)
        );

        for (OperationRequest request : requests) {
            kafkaTemplate.send(TOPIC, request)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Published operation notification to topic '{}': {}", TOPIC, request);
                        } else {
                            log.error("Failed to publish operation notification to topic '{}': {}", TOPIC, request, ex);
                            meterRegistry.counter(NOTIFICATION_FAILED_METRIC, "topic", TOPIC).increment();
                        }
                    });
        }
    }
}
