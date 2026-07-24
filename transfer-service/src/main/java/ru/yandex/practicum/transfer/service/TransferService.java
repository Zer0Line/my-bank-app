package ru.yandex.practicum.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.client.AccountsServiceClient;
import ru.yandex.practicum.transfer.client.NotificationServiceClient;
import ru.yandex.practicum.transfer.dto.AccountResponse;
import ru.yandex.practicum.transfer.dto.OperationRequest;
import ru.yandex.practicum.transfer.dto.TransferActionRequest;
import ru.yandex.practicum.transfer.dto.TransferRequest;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsServiceClient transferClient;
    private final NotificationServiceClient notificationClient;

    public AccountResponse transfer(String senderLogin, TransferActionRequest actionRequest) {
        var request = new TransferRequest(
                senderLogin,
                actionRequest.login(),
                actionRequest.value()
        );

        log.info("Calling accounts-service to transfer from '{}' to '{}', amount={}",
                senderLogin, actionRequest.login(), actionRequest.value());

        AccountResponse response = transferClient.transfer(request);

        notificationClient.saveOperations(List.of(
                new OperationRequest(senderLogin, "TRANSFER_SENT",
                        "Transfer to " + actionRequest.login(), actionRequest.value()),
                new OperationRequest(actionRequest.login(), "TRANSFER_RECEIVED",
                        "Transfer from " + senderLogin, actionRequest.value())
        ));

        return response;
    }
}
