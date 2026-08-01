package ru.yandex.practicum.transfer.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.client.AccountsServiceClient;
import ru.yandex.practicum.transfer.client.NotificationServiceClient;
import ru.yandex.practicum.transfer.dto.AccountResponse;
import ru.yandex.practicum.transfer.dto.OperationRequest;
import ru.yandex.practicum.transfer.dto.TransferActionRequest;
import ru.yandex.practicum.transfer.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsServiceClient accountsServiceClient;
    private final NotificationServiceClient notificationClient;

    @CircuitBreaker(name = "accounts-service", fallbackMethod = "fallbackAccountsService")
    @Retry(name = "accounts-service")
    public AccountResponse transfer(String senderLogin, TransferActionRequest actionRequest) {
        var request = new TransferRequest(
                senderLogin,
                actionRequest.login(),
                actionRequest.value()
        );

        log.info("Calling accounts-service to transfer from '{}' to '{}', amount={}",
                senderLogin, actionRequest.login(), actionRequest.value());

        AccountResponse response = accountsServiceClient.transfer(request);

        notify(senderLogin, actionRequest);

        return response;
    }

    @CircuitBreaker(name = "notification-service")
    @Retry(name = "notification-service")
    public void notify(String senderLogin, TransferActionRequest actionRequest) {
        notificationClient.saveOperations(List.of(
                new OperationRequest(senderLogin, "TRANSFER_SENT",
                        "Transfer to " + actionRequest.login(), actionRequest.value()),
                new OperationRequest(actionRequest.login(), "TRANSFER_RECEIVED",
                        "Transfer from " + senderLogin, actionRequest.value())
        ));
    }

    private AccountResponse fallbackAccountsService(String senderLogin, TransferActionRequest actionRequest, Throwable t) {
        log.error("Accounts-service unavailable for transfer from '{}' to '{}', amount={}", senderLogin, actionRequest.login(), actionRequest.value(), t);
        throw new RuntimeException("Transfer failed: accounts service is unavailable", t);
    }
}
