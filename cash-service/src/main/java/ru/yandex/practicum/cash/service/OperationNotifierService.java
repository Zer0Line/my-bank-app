package ru.yandex.practicum.cash.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.OperationRequest;
import ru.yandex.practicum.cash.exception.OperationNotificationException;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationNotifierService {

    private final KafkaTemplate<String, OperationRequest> kafkaTemplate;

    private static final String TOPIC = "cash-request";
    private static final long SEND_TIMEOUT_SECONDS = 30;

    public void notifyOperationStarted(String login, CashAction action, BigDecimal value) {
        OperationRequest operation = new OperationRequest(login, "CASH_" + action.name(),
                "Cash operation started via cash-service", value);
        try {
            CompletableFuture<SendResult<String, OperationRequest>> future = kafkaTemplate.send(TOPIC, operation);
            future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("Published operation notification to topic '{}': {}", TOPIC, operation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationNotificationException("Interrupted while publishing operation notification", e);
        } catch (Exception e) {
            throw new OperationNotificationException("Failed to publish operation notification to topic " + TOPIC, e);
        }
    }
}
