package ru.yandex.practicum.transfer.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.transfer.client.AccountsServiceClient;
import ru.yandex.practicum.transfer.dto.AccountDto;
import ru.yandex.practicum.transfer.dto.AccountResponse;
import ru.yandex.practicum.transfer.dto.TransferActionRequest;
import ru.yandex.practicum.transfer.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountsServiceClient transferClient;

    @Mock
    private OperationNotifierService operationNotifierService;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer() {
        var response = new AccountResponse(
                "sender", "Sender User", "2000-01-01", BigDecimal.valueOf(700),
                List.of(new AccountDto("recipient", "Recipient User"))
        );
        when(transferClient.transfer(any(TransferRequest.class), anyString()))
                .thenReturn(response);

        AccountResponse result = transferService.transfer(
                "sender", new TransferActionRequest(BigDecimal.valueOf(300), "recipient"), "test-key");

        assertEquals("sender", result.login());
        assertEquals(0, BigDecimal.valueOf(700).compareTo(result.sum()));
        verify(transferClient).transfer(new TransferRequest("sender", "recipient", BigDecimal.valueOf(300)), "test-key");
        verify(operationNotifierService).notifyOperationStarted("sender", "recipient", BigDecimal.valueOf(300));
        assertEquals(0.0, failedTransferCounter("sender", "recipient"), 0.0001);
    }

    @Test
    void fallbackAccountsService_recordsFailedTransferByLogins() {
        assertThrows(RuntimeException.class, () ->
                transferService.fallbackAccountsService(
                        "sender", new TransferActionRequest(BigDecimal.valueOf(300), "recipient"), "test-key",
                        new IllegalStateException("accounts down")));

        assertEquals(1.0, failedTransferCounter("sender", "recipient"), 0.0001);
    }

    private double failedTransferCounter(String senderLogin, String recipientLogin) {
        return meterRegistry.counter("transfer.failed",
                "sender_login", senderLogin,
                "recipient_login", recipientLogin).count();
    }
}