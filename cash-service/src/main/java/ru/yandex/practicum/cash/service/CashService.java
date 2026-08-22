package ru.yandex.practicum.cash.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.client.AccountsServiceClient;
import ru.yandex.practicum.cash.dto.AccountResponse;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.UpdateAmountRequest;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsServiceClient accountsServiceClient;
    private final OperationNotifierService operationNotifierService;

    @CircuitBreaker(name = "accounts-service", fallbackMethod = "fallbackAccountsService")
    @Retry(name = "accounts-service")
    public AccountResponse processCashAction(String login, BigDecimal value, CashAction action, String idempotencyKey) {
        var request = new UpdateAmountRequest(login, value, action);

        log.info("Calling accounts-service to update amount for login='{}', value={}, action={}",
                login, value, action);

        operationNotifierService.notifyOperationStarted(login, action, value);

        return accountsServiceClient.updateAmount(request, idempotencyKey);
    }

    private AccountResponse fallbackAccountsService(String login, BigDecimal value, CashAction action, String idempotencyKey, Throwable t) {
        log.error("Accounts-service unavailable for processCashAction: login='{}', value={}, action={}", login, value, action, t);
        throw new RuntimeException("Cash operation failed: accounts service is unavailable", t);
    }
}
