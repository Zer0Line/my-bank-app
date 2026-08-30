package ru.yandex.practicum.accounts.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.service.AccountsService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/accounts")
public class InternalAccountsController {

    private final AccountsService accountsService;

    @PatchMapping("/amount")
    public AccountResponse updateAmount(@RequestBody UpdateAmountRequest request,
                                         @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        log.info("Incoming internal request: POST /api/internal/accounts/amount with body: {}, idempotencyKey: {}",
                request, idempotencyKey);
        return accountsService.updateAmount(request, idempotencyKey);
    }

    @PostMapping("/transfer")
    public AccountResponse transfer(@RequestBody TransferRequest request,
                                     @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        log.info("Incoming internal request: POST /api/internal/accounts/transfer with body: {}, idempotencyKey: {}",
                request, idempotencyKey);
        return accountsService.transfer(request, idempotencyKey);
    }
}
