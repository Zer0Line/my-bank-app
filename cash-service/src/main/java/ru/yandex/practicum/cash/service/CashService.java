package ru.yandex.practicum.cash.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.client.AccountsServiceClient;
import ru.yandex.practicum.cash.dto.AccountResponse;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.UpdateAmountRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsServiceClient accountsServiceClient;

    public AccountResponse processCashAction(String login, int value, CashAction action) {
        var request = new UpdateAmountRequest(login, value, action);

        log.info("Calling accounts-service to update amount for login='{}', value={}, action={}",
                login, value, action);

        return accountsServiceClient.updateAmount(request);
    }
}
