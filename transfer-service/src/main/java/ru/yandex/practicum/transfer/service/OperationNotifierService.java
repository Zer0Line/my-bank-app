package ru.yandex.practicum.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.dto.OperationRequest;
import ru.yandex.practicum.transfer.exception.OperationNotificationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationNotifierService {

    private final KafkaTemplate<String, OperationRequest> kafkaTemplate;

    private static final String TOPIC = "transfer-request";
    private static final long SEND_TIMEOUT_SECONDS = 30;

    public void notifyOperationStarted(String senderLogin, String recipientLogin, BigDecimal value) {
        List<OperationRequest> requests = List.of(
                new OperationRequest(senderLogin, "TRANSFER_SENT",
                        "Transfer to " + recipientLogin + " started", value),
                new OperationRequest(recipientLogin, "TRANSFER_RECEIVED",
                        "Transfer from " + senderLogin + " started", value)
        );

        for (OperationRequest request : requests) {
            try {
                CompletableFuture<SendResult<String, OperationRequest>> future = kafkaTemplate.send(TOPIC, request);
                future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.info("Published operation notification to topic '{}': {}", TOPIC, request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OperationNotificationException("Interrupted while publishing operation notification", e);
            } catch (Exception e) {
                throw new OperationNotificationException("Failed to publish operation notification to topic " + TOPIC, e);
            }
        }
    }
}
