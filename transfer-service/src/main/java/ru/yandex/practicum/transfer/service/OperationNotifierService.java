package ru.yandex.practicum.transfer.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.client.NotificationServiceClient;
import ru.yandex.practicum.transfer.dto.OperationRequest;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationNotifierService {

    private final NotificationServiceClient notificationClient;

    @CircuitBreaker(name = "notification-service")
    @Retry(name = "notification-service")
    public void notifyOperationStarted(String senderLogin, String recipientLogin, BigDecimal value) {
        notificationClient.saveOperations(List.of(
                new OperationRequest(senderLogin, "TRANSFER_SENT",
                        "Transfer to " + recipientLogin + " started", value),
                new OperationRequest(recipientLogin, "TRANSFER_RECEIVED",
                        "Transfer from " + senderLogin + " started", value)
        ));
    }
}