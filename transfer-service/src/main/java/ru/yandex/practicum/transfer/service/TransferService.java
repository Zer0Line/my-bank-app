package ru.yandex.practicum.transfer.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
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

    private static final String TRANSFER_FAILED_METRIC = "transfer.failed";

    private final AccountsServiceClient accountsServiceClient;
    private final OperationNotifierService operationNotifierService;
    private final MeterRegistry meterRegistry;

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

    AccountResponse fallbackAccountsService(String senderLogin, TransferActionRequest actionRequest, String idempotencyKey, Throwable t) {
        log.error("Transfer from '{}' to '{}' failed, amount={}", senderLogin, actionRequest.login(), actionRequest.value(), t);
        recordFailedTransfer(senderLogin, actionRequest.login());
        throw new RuntimeException("Transfer failed: accounts service is unavailable", t);
    }

    private void recordFailedTransfer(String senderLogin, String recipientLogin) {
        meterRegistry.counter(TRANSFER_FAILED_METRIC,
                "sender_login", senderLogin,
                "recipient_login", recipientLogin).increment();
    }
}