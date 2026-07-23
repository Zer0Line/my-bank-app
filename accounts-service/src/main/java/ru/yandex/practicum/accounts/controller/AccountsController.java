package ru.yandex.practicum.accounts.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.service.AccountsService;

@RestController
@RequestMapping("/api/accounts")
public class AccountsController {

    private static final Logger log = LoggerFactory.getLogger(AccountsController.class);

    private final AccountsService accountsService;

    public AccountsController(AccountsService accountsService) {
        this.accountsService = accountsService;
    }

    @GetMapping
    public AccountResponse getAccount() {
        log.info("Incoming request: GET /api/accounts");
        return accountsService.getAccount();
    }

    @PutMapping
    public AccountResponse updateAccount(@RequestBody UpdateAccountRequest request) {
        log.info("Incoming request: PUT /api/accounts with body: {}", request);
        return accountsService.updateAccount(request);
    }
}