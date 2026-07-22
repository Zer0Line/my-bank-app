package ru.yandex.practicum.accounts.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountResponse;

import java.util.List;

@Service
public class AccountsService {

    private static final Logger log = LoggerFactory.getLogger(AccountsService.class);

    public AccountResponse getAccount(String login) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            log.info("Token claims: subject={}, issuer={}, authorities={}",
                    jwt.getSubject(), jwt.getIssuer(), jwtAuth.getAuthorities());
        } else {
            log.warn("No JWT authentication found, type={}",
                    authentication != null ? authentication.getClass().getSimpleName() : "null");
        }

        var response = new AccountResponse(
                login,
                "Иванов Иван",
                "2001-01-01",
                100,
                List.of(
                        new AccountDto("petrov", "Петров Петр"),
                        new AccountDto("sidorov", "Сидоров Сидор")
                )
        );

        log.info("Account response for login='{}': {}", login, response);
        return response;
    }
}
