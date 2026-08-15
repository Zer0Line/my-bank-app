package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.CashAction;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.entity.AccountEntity;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private OperationNotifierService operationNotifierService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private AccountsService accountsService;

    @Captor
    private ArgumentCaptor<AccountEntity> accountCaptor;

    private AccountEntity senderEntity;
    private AccountEntity recipientEntity;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "testuser")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt));

        senderEntity = new AccountEntity();
        senderEntity.setLogin("testuser");
        senderEntity.setName("User");
        senderEntity.setSurename("Test");
        senderEntity.setDateOfBirth(LocalDate.of(2000, 1, 1));
        senderEntity.setAmount(BigDecimal.valueOf(1000));

        recipientEntity = new AccountEntity();
        recipientEntity.setLogin("recipient");
        recipientEntity.setName("Recipient");
        recipientEntity.setSurename("The");
        recipientEntity.setDateOfBirth(LocalDate.of(1990, 5, 15));
        recipientEntity.setAmount(BigDecimal.valueOf(500));
    }

    @Test
    void getAccount() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.findAllByLoginIsNot("testuser")).thenReturn(List.of(recipientEntity));
        when(accountMapper.toDto(recipientEntity)).thenReturn(new AccountDto("recipient", "The Recipient"));

        var response = accountsService.getAccount();

        assertEquals("testuser", response.login());
        assertEquals("Test User", response.name());
        assertEquals("2000-01-01", response.birthdate());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(response.sum()));
        assertEquals(1, response.accounts().size());
        verify(accountRepository).findByLogin("testuser");
    }

    @Test
    void getAccount_notFound() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> accountsService.getAccount());
    }

    @Test
    void updateAccount() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(senderEntity);
        when(accountRepository.findAllByLoginIsNot("testuser")).thenReturn(List.of());

        var response = accountsService.updateAccount(
                new UpdateAccountRequest("New Name", "2025-12-01"));

        assertEquals("New Name", response.name());
        assertEquals("New", senderEntity.getSurename());
        assertEquals("Name", senderEntity.getName());
        assertEquals(LocalDate.of(2025, 12, 1), senderEntity.getDateOfBirth());
        verify(operationNotifierService).notifyAccountUpdate("testuser");
    }

    @Test
    void updateAmount_put() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(senderEntity);
        when(accountRepository.findAllByLoginIsNot("testuser")).thenReturn(List.of());

        accountsService.updateAmount(new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), CashAction.PUT), null);

        assertEquals(0, BigDecimal.valueOf(1300).compareTo(senderEntity.getAmount()));
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(1300).compareTo(accountCaptor.getValue().getAmount()));
        verify(operationNotifierService).notifyCashOperation("testuser", CashAction.PUT, BigDecimal.valueOf(300));
    }

    @Test
    void updateAmount_get() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(senderEntity);
        when(accountRepository.findAllByLoginIsNot("testuser")).thenReturn(List.of());

        accountsService.updateAmount(new UpdateAmountRequest("testuser", BigDecimal.valueOf(400), CashAction.GET), null);

        assertEquals(0, BigDecimal.valueOf(600).compareTo(senderEntity.getAmount()));
        verify(operationNotifierService).notifyCashOperation("testuser", CashAction.GET, BigDecimal.valueOf(400));
    }

    @Test
    void updateAmount_insufficientFunds() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));

        assertThrows(ResponseStatusException.class, () ->
                accountsService.updateAmount(new UpdateAmountRequest("testuser", BigDecimal.valueOf(2000), CashAction.GET), null));
    }

    @Test
    void transfer() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.findByLogin("recipient")).thenReturn(Optional.of(recipientEntity));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.findAllByLoginIsNot("testuser")).thenReturn(List.of());

        var response = accountsService.transfer(
                new TransferRequest("testuser", "recipient", BigDecimal.valueOf(300)), null);

        assertEquals(0, BigDecimal.valueOf(700).compareTo(senderEntity.getAmount()));
        assertEquals(0, BigDecimal.valueOf(800).compareTo(recipientEntity.getAmount()));
        verify(operationNotifierService).notifyTransfer("testuser", "recipient", BigDecimal.valueOf(300));
    }

    @Test
    void transfer_insufficientFunds() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.findByLogin("recipient")).thenReturn(Optional.of(recipientEntity));

        assertThrows(ResponseStatusException.class, () ->
                accountsService.transfer(
                        new TransferRequest("testuser", "recipient", BigDecimal.valueOf(2000)), null));
    }

    @Test
    void senderAccountNotFound() {
        when(accountRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                accountsService.transfer(
                        new TransferRequest("unknown", "recipient", BigDecimal.valueOf(100)), null));
    }

    @Test
    void updateAmount_withIdempotencyKey_delegatesToIdempotencyService() {
        UpdateAmountRequest request = new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), CashAction.PUT);
        AccountResponse replay = new AccountResponse("testuser", "Test User", "2000-01-01", BigDecimal.valueOf(1300), List.of());
        when(idempotencyService.execute(eq("amount-key"), eq(request), any())).thenReturn(replay);

        AccountResponse result = accountsService.updateAmount(request, "amount-key");

        assertSame(replay, result);
        verify(idempotencyService).execute(eq("amount-key"), eq(request), any());
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }

    @Test
    void transfer_withIdempotencyKey_delegatesToIdempotencyService() {
        TransferRequest request = new TransferRequest("testuser", "recipient", BigDecimal.valueOf(300));
        AccountResponse replay = new AccountResponse("testuser", "Test User", "2000-01-01", BigDecimal.valueOf(700), List.of());
        when(idempotencyService.execute(eq("transfer-key"), eq(request), any())).thenReturn(replay);

        AccountResponse result = accountsService.transfer(request, "transfer-key");

        assertSame(replay, result);
        verify(idempotencyService).execute(eq("transfer-key"), eq(request), any());
        verify(accountRepository, never()).save(any(AccountEntity.class));
    }
}
