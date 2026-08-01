package ru.yandex.practicum.cash.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.client.AccountsServiceClient;
import ru.yandex.practicum.cash.client.NotificationServiceClient;
import ru.yandex.practicum.cash.dto.AccountResponse;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.OperationRequest;
import ru.yandex.practicum.cash.dto.UpdateAmountRequest;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsServiceClient accountsServiceClient;
    private final NotificationServiceClient notificationClient;

    @CircuitBreaker(name = "accounts-service", fallbackMethod = "fallbackAccountsService")
    @Retry(name = "accounts-service")
    public AccountResponse processCashAction(String login, BigDecimal value, CashAction action) {
        var request = new UpdateAmountRequest(login, value, action);

        log.info("Calling accounts-service to update amount for login='{}', value={}, action={}",
                login, value, action);

        AccountResponse response = accountsServiceClient.updateAmount(request);

        notify(login, action, value);

        return response;
    }

    @CircuitBreaker(name = "notification-service")
    @Retry(name = "notification-service")
    public void notify(String login, CashAction action, BigDecimal value) {
        notificationClient.saveOperation(new OperationRequest(login, "CASH_" + action.name(),
                "Cash operation via cash-service", value));
    }

    private AccountResponse fallbackAccountsService(String login, BigDecimal value, CashAction action, Throwable t) {
        log.error("Accounts-service unavailable for processCashAction: login='{}', value={}, action={}", login, value, action, t);
        throw new RuntimeException("Cash operation failed: accounts service is unavailable", t);
    }
}
