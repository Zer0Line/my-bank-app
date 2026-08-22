package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.cash.client.AccountsServiceClient;
import ru.yandex.practicum.cash.dto.AccountDto;
import ru.yandex.practicum.cash.dto.AccountResponse;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.UpdateAmountRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountsServiceClient accountsServiceClient;

    @Mock
    private OperationNotifierService operationNotifierService;

    @InjectMocks
    private CashService cashService;

    @Test
    void processCashAction() {
        var response = new AccountResponse(
                "testuser", "Test User", "2000-01-01", BigDecimal.valueOf(1500),
                List.of(new AccountDto("other", "Other User"))
        );
        when(accountsServiceClient.updateAmount(any(UpdateAmountRequest.class), anyString()))
                .thenReturn(response);

        AccountResponse result = cashService.processCashAction(
                "testuser", BigDecimal.valueOf(500), CashAction.PUT, "test-key");

        assertEquals("testuser", result.login());
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(result.sum()));
        verify(accountsServiceClient).updateAmount(
                new UpdateAmountRequest("testuser", BigDecimal.valueOf(500), CashAction.PUT), "test-key");
        verify(operationNotifierService).notifyOperationStarted("testuser", CashAction.PUT, BigDecimal.valueOf(500));
    }
}