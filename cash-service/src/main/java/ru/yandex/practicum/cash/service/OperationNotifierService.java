package ru.yandex.practicum.cash.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.OperationRequest;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationNotifierService {

    private static final String TOPIC = "cash-request";
    private static final String NOTIFICATION_FAILED_METRIC = "operation.notification.failed";

    private final KafkaTemplate<String, OperationRequest> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public void notifyOperationStarted(String login, CashAction action, BigDecimal value) {
        OperationRequest operation = new OperationRequest(login, "CASH_" + action.name(),
                "Cash operation started via cash-service", value);
        kafkaTemplate.send(TOPIC, operation)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published operation notification to topic '{}': {}", TOPIC, operation);
                    } else {
                        log.error("Failed to publish operation notification to topic '{}': {}", TOPIC, operation, ex);
                        meterRegistry.counter(NOTIFICATION_FAILED_METRIC, "topic", TOPIC).increment();
                    }
                });
    }
}
