package ru.yandex.practicum.cash.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.cash.dto.AccountResponse;
import ru.yandex.practicum.cash.dto.CashActionRequest;
import ru.yandex.practicum.cash.service.CashService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cash")
public class CashController {

    private final CashService cashService;

    @PostMapping
    public AccountResponse editCash(@RequestBody CashActionRequest request) {
        log.info("Incoming request: POST /api/cash with body: {}", request);

        String login = extractLogin();
        return cashService.processCashAction(login, request.value(), request.action());
    }

    private String extractLogin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String login = jwt.getClaimAsString("preferred_username");
            log.info("Extracted login='{}' from JWT", login);
            return login;
        }
        throw new IllegalStateException("Authentication required");
    }
}
