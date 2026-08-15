package ru.yandex.practicum.accounts.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
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
        log.info("Incoming request: PUT /api/accounts with body: {}", request);
        return accountsService.updateAccount(request);
    }

    @PatchMapping("/amount")
    public AccountResponse updateAmount(@RequestBody UpdateAmountRequest request,
                                        @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        log.info("Incoming request: PATCH /api/accounts/amount with body: {}, idempotencyKey: {}", request, idempotencyKey);
        return accountsService.updateAmount(request, idempotencyKey);
    }

    @PostMapping("/transfer")
    public AccountResponse transfer(@RequestBody TransferRequest request,
                                    @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        log.info("Incoming request: POST /api/accounts/transfer with body: {}, idempotencyKey: {}", request, idempotencyKey);
        return accountsService.transfer(request, idempotencyKey);
    }
}