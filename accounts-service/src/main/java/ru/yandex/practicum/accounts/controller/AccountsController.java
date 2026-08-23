package ru.yandex.practicum.accounts.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.service.AccountsService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountsController {

    private final AccountsService accountsService;

    @GetMapping
    public AccountResponse getAccount() {
        log.info("Incoming request: GET /api/accounts");
        return accountsService.getAccount();
    }

    @PutMapping
    public AccountResponse updateAccount(@RequestBody UpdateAccountRequest request) {
        return accountsService.updateAccount(request);
    }
}