package ru.yandex.practicum.transfer.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.client.AccountsServiceClient;
import ru.yandex.practicum.transfer.dto.AccountResponse;
import ru.yandex.practicum.transfer.dto.TransferActionRequest;
import ru.yandex.practicum.transfer.dto.TransferRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsServiceClient accountsServiceClient;
    private final OperationNotifierService operationNotifierService;

    @CircuitBreaker(name = "accounts-service", fallbackMethod = "fallbackAccountsService")
    @Retry(name = "accounts-service")
    public AccountResponse transfer(String senderLogin, TransferActionRequest actionRequest, String idempotencyKey) {
        var request = new TransferRequest(
                senderLogin,
                actionRequest.login(),
                actionRequest.value()
        );
        operationNotifierService.notifyOperationStarted(senderLogin, actionRequest.login(), actionRequest.value());

        return accountsServiceClient.transfer(request, idempotencyKey);
    }

    private AccountResponse fallbackAccountsService(String senderLogin, TransferActionRequest actionRequest, String idempotencyKey, Throwable t) {
        log.error("Accounts-service unavailable for transfer from '{}' to '{}', amount={}", senderLogin, actionRequest.login(), actionRequest.value(), t);
        throw new RuntimeException("Transfer failed: accounts service is unavailable", t);
    }
}