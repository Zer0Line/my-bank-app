package ru.yandex.practicum.transfer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import ru.yandex.practicum.transfer.dto.AccountResponse;
import ru.yandex.practicum.transfer.dto.TransferRequest;

@FeignClient(
        name = "accounts-service",
        url = "${clients.accounts-service.url}",
        configuration = AccountsServiceFeignConfig.class
)
public interface AccountsServiceClient {

    @PostMapping("/api/internal/accounts/transfer")
    AccountResponse transfer(@RequestBody TransferRequest request,
                             @RequestHeader("Idempotency-Key") String idempotencyKey);
}
