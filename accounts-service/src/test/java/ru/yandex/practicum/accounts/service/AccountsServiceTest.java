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
import ru.yandex.practicum.accounts.client.NotificationServiceClient;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.CashAction;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.entity.AccountEntity;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private NotificationServiceClient notificationClient;

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
        verify(notificationClient).saveOperation(argThat(op ->
                "testuser".equals(op.login()) && "ACCOUNT_UPDATE".equals(op.type())));
    }

    @Test
    void updateAmount_put() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(senderEntity);
        when(accountRepository.findAllByLoginIsNot("testuser")).thenReturn(List.of());

        accountsService.updateAmount(new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), CashAction.PUT));

        assertEquals(0, BigDecimal.valueOf(1300).compareTo(senderEntity.getAmount()));
        verify(accountRepository).save(accountCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(1300).compareTo(accountCaptor.getValue().getAmount()));
        verify(notificationClient).saveOperation(argThat(op ->
                "CASH_PUT".equals(op.type()) && 0 == BigDecimal.valueOf(300).compareTo(op.amount())));
    }

    @Test
    void updateAmount_get() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(senderEntity);
        when(accountRepository.findAllByLoginIsNot("testuser")).thenReturn(List.of());

        accountsService.updateAmount(new UpdateAmountRequest("testuser", BigDecimal.valueOf(400), CashAction.GET));

        assertEquals(0, BigDecimal.valueOf(600).compareTo(senderEntity.getAmount()));
        verify(notificationClient).saveOperation(argThat(op ->
                "CASH_GET".equals(op.type()) && 0 == BigDecimal.valueOf(400).compareTo(op.amount())));
    }

    @Test
    void updateAmount_insufficientFunds() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));

        assertThrows(ResponseStatusException.class, () ->
                accountsService.updateAmount(new UpdateAmountRequest("testuser", BigDecimal.valueOf(2000), CashAction.GET)));
    }

    @Test
    void transfer() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.findByLogin("recipient")).thenReturn(Optional.of(recipientEntity));
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.findAllByLoginIsNot("testuser")).thenReturn(List.of());

        var response = accountsService.transfer(
                new TransferRequest("testuser", "recipient", BigDecimal.valueOf(300)));

        assertEquals(0, BigDecimal.valueOf(700).compareTo(senderEntity.getAmount()));
        assertEquals(0, BigDecimal.valueOf(800).compareTo(recipientEntity.getAmount()));
        verify(notificationClient).saveOperations(argThat(ops ->
                ops.size() == 2 &&
                        "TRANSFER_SENT".equals(ops.get(0).type()) &&
                        "TRANSFER_RECEIVED".equals(ops.get(1).type())));
    }

    @Test
    void transfer_insufficientFunds() {
        when(accountRepository.findByLogin("testuser")).thenReturn(Optional.of(senderEntity));
        when(accountRepository.findByLogin("recipient")).thenReturn(Optional.of(recipientEntity));

        assertThrows(ResponseStatusException.class, () ->
                accountsService.transfer(
                        new TransferRequest("testuser", "recipient", BigDecimal.valueOf(2000))));
    }

    @Test
    void senderAccountNotFound() {
        when(accountRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                accountsService.transfer(
                        new TransferRequest("unknown", "recipient", BigDecimal.valueOf(100))));
    }
}
