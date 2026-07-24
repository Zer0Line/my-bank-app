package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.CashAction;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.entity.AccountEntity;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountsService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountResponse getAccount() {
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);
        log.info("Account found for login='{}': {}", login, account);
        return buildFullResponse(login, account);
    }

    public AccountResponse updateAccount(UpdateAccountRequest request) {
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);

        account.setName(request.name());
        account.setDateOfBirth(LocalDate.parse(request.birthdate()));

        AccountEntity saved = accountRepository.save(account);
        log.info("Account updated for login='{}': {}", login, saved);
        return buildFullResponse(login, saved);
    }

    public AccountResponse updateAmount(UpdateAmountRequest request) {
        AccountEntity account = findAccountByLogin(request.login());

        int newAmount = switch (request.action()) {
            case GET -> account.getAmount() - request.value();
            case PUT -> account.getAmount() + request.value();
        };

        if (newAmount < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        account.setAmount(newAmount);
        AccountEntity saved = accountRepository.save(account);
        log.info("Amount updated for login='{}': new amount={}", request.login(), newAmount);
        return buildFullResponse(request.login(), saved);
    }

    public AccountResponse transfer(TransferRequest request) {
        AccountEntity sender = findAccountByLogin(request.senderLogin());
        AccountEntity recipient = findAccountByLogin(request.recipientLogin());

        int newSenderAmount = sender.getAmount() - request.amount();
        if (newSenderAmount < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Insufficient funds");
        }

        sender.setAmount(newSenderAmount);
        recipient.setAmount(recipient.getAmount() + request.amount());

        accountRepository.save(sender);
        accountRepository.save(recipient);

        log.info("Transfer completed: sender='{}', recipient='{}', amount={}",
                request.senderLogin(), request.recipientLogin(), request.amount());

        return buildFullResponse(request.senderLogin(), sender);
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
            String login = jwt.getClaimAsString("preferred_username");
            log.info("Token claims: preferred_username={}, subject={}, issuer={}",
                    login, jwt.getSubject(), jwt.getIssuer());
            return login;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    private AccountEntity findAccountByLogin(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found: " + login));
    }
}