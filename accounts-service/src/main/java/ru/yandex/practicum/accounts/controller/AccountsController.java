package ru.yandex.practicum.accounts.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.service.AccountsService;

@RestController
@RequestMapping("/api/accounts")
public class AccountsController {

    private static final Logger log = LoggerFactory.getLogger(AccountsController.class);

    @Autowired
    private AccountsService accountsService;

    @GetMapping("/{login}")
    public AccountResponse getAccount(@PathVariable("login") String login) {
        log.info("Incoming request: GET /api/accounts/{}", login);
        return accountsService.getAccount(login);
    }
}
