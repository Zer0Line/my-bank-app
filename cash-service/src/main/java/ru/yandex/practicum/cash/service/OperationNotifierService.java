package ru.yandex.practicum.cash.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.client.NotificationServiceClient;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.OperationRequest;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OperationNotifierService {

    private final NotificationServiceClient notificationClient;

    @CircuitBreaker(name = "notification-service")
    @Retry(name = "notification-service")
    public void notifyOperationStarted(String login, CashAction action, BigDecimal value) {
        notificationClient.saveOperation(new OperationRequest(login, "CASH_" + action.name(),
                "Cash operation started via cash-service", value));
    }
}