package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.cash.client.AccountsServiceClient;
import ru.yandex.practicum.cash.client.NotificationServiceClient;
import ru.yandex.practicum.cash.dto.AccountDto;
import ru.yandex.practicum.cash.dto.AccountResponse;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.OperationRequest;
import ru.yandex.practicum.cash.dto.UpdateAmountRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountsServiceClient accountsServiceClient;

    @Mock
    private NotificationServiceClient notificationClient;

    @InjectMocks
    private CashService cashService;

    @Test
    void processCashAction() {
        var response = new AccountResponse(
                "testuser", "Test User", "2000-01-01", 1500,
                List.of(new AccountDto("other", "Other User"))
        );
        when(accountsServiceClient.updateAmount(any(UpdateAmountRequest.class)))
                .thenReturn(response);

        AccountResponse result = cashService.processCashAction("testuser", 500, CashAction.PUT);

        assertEquals("testuser", result.login());
        assertEquals(1500, result.sum());
        verify(accountsServiceClient).updateAmount(new UpdateAmountRequest("testuser", 500, CashAction.PUT));
        verify(notificationClient).saveOperation(any(OperationRequest.class));
    }
}
