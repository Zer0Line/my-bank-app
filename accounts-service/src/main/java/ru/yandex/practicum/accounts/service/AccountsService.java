package ru.yandex.practicum.accounts.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.entity.AccountEntity;
import ru.yandex.practicum.accounts.mapper.AccountMapper;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
public class AccountsService {

    private static final Logger log = LoggerFactory.getLogger(AccountsService.class);

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountsService(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }

    public AccountResponse getAccount() {
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);
        log.info("Account found for login='{}': {}", login, account);
        return accountMapper.toResponse(account);
    }

    public AccountResponse updateAccount(UpdateAccountRequest request) {
        String login = extractLogin();
        AccountEntity account = findAccountByLogin(login);

        account.setName(request.name());
        account.setDateOfBirth(LocalDate.parse(request.birthdate()));

        AccountEntity saved = accountRepository.save(account);
        log.info("Account updated for login='{}': {}", login, saved);
        return accountMapper.toResponse(saved);
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