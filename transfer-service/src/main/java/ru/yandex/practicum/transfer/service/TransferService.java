package ru.yandex.practicum.transfer.service;

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

    private final AccountsServiceClient transferClient;

    public AccountResponse transfer(String senderLogin, TransferActionRequest actionRequest) {
        var request = new TransferRequest(
                senderLogin,
                actionRequest.login(),
                actionRequest.value()
        );

        log.info("Calling accounts-service to transfer from '{}' to '{}', amount={}",
                senderLogin, actionRequest.login(), actionRequest.value());

        return transferClient.transfer(request);
    }
}
