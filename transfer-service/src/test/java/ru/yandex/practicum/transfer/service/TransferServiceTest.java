package ru.yandex.practicum.transfer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.transfer.client.AccountsServiceClient;
import ru.yandex.practicum.transfer.client.NotificationServiceClient;
import ru.yandex.practicum.transfer.dto.AccountDto;
import ru.yandex.practicum.transfer.dto.AccountResponse;
import ru.yandex.practicum.transfer.dto.TransferActionRequest;
import ru.yandex.practicum.transfer.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountsServiceClient transferClient;

    @Mock
    private NotificationServiceClient notificationClient;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer() {
        var response = new AccountResponse(
                "sender", "Sender User", "2000-01-01", BigDecimal.valueOf(700),
                List.of(new AccountDto("recipient", "Recipient User"))
        );
        when(transferClient.transfer(any(TransferRequest.class)))
                .thenReturn(response);

        AccountResponse result = transferService.transfer(
                "sender", new TransferActionRequest(BigDecimal.valueOf(300), "recipient"));

        assertEquals("sender", result.login());
        assertEquals(0, BigDecimal.valueOf(700).compareTo(result.sum()));
        verify(transferClient).transfer(new TransferRequest("sender", "recipient", BigDecimal.valueOf(300)));
        verify(notificationClient).saveOperations(any(List.class));
    }
}
