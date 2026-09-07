package ru.yandex.practicum.accounts.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.entity.AccountEntity;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AccountsService {

    private static final String WITHDRAWAL_FAILED_METRIC = "accounts.withdrawal.failed";

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OperationNotifierService operationNotifierService;
    private final IdempotencyService idempotencyService;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    public AccountResponse getAccount() {
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);
        return buildFullResponse(login, account);
    }

    public AccountResponse updateAccount(UpdateAccountRequest request) {
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);

        String[] parts = request.name().split(" ", 2);
        account.setSurename(parts[0]);
        account.setName(parts.length > 1 ? parts[1] : "");
        account.setDateOfBirth(LocalDate.parse(request.birthdate()));

        AccountEntity saved = accountRepository.save(account);
        operationNotifierService.notifyAccountUpdate(login);
        return buildFullResponse(login, saved);
    }

    public AccountResponse updateAmount(UpdateAmountRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return doUpdateAmount(request);
        }
        return idempotencyService.execute(idempotencyKey, request, () -> doUpdateAmount(request));
    }

    private AccountResponse doUpdateAmount(UpdateAmountRequest request) {
        AccountEntity account = findAccountByLogin(request.login());

        BigDecimal newAmount = switch (request.action()) {
            case GET -> account.getAmount().subtract(request.value());
            case PUT -> account.getAmount().add(request.value());
        };

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            recordFailedWithdrawal(request.login());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        account.setAmount(newAmount);
        AccountEntity saved = accountRepository.save(account);
        operationNotifierService.notifyCashOperation(request.login(), request.action(), request.value());
        return buildFullResponse(request.login(), saved);
    }

    public AccountResponse transfer(TransferRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return doTransfer(request);
        }
        return idempotencyService.execute(idempotencyKey, request, () -> doTransfer(request));
    }

    private AccountResponse doTransfer(TransferRequest request) {
        AccountEntity sender = findAccountByLogin(request.senderLogin());
        AccountEntity recipient = findAccountByLogin(request.recipientLogin());

        BigDecimal newSenderAmount = sender.getAmount().subtract(request.amount());
        if (newSenderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        sender.setAmount(newSenderAmount);
        recipient.setAmount(recipient.getAmount().add(request.amount()));

        accountRepository.save(sender);
        accountRepository.save(recipient);

        operationNotifierService.notifyTransfer(request.senderLogin(), request.recipientLogin(), request.amount());

        return buildFullResponse(request.senderLogin(), sender);
    }

    private void recordFailedWithdrawal(String login) {
        meterRegistry.counter(WITHDRAWAL_FAILED_METRIC, "login", login).increment();
    }

    private AccountResponse buildFullResponse(String login, AccountEntity account) {
        List<AccountEntity> otherAccounts = accountRepository.findAllByLoginIsNot(login);
        List<AccountDto> accountDtos = otherAccounts.stream()
                .map(accountMapper::toDto)
                .toList();
        return new AccountResponse(
                account.getLogin(),
                account.getSurename() + " " + account.getName(),
                account.getDateOfBirth().toString(),
                account.getAmount(),
                accountDtos
        );
    }

    private String extractLogin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("preferred_username");
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    private AccountEntity findAccountByLogin(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + login));
    }
}