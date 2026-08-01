package ru.yandex.practicum.mybankfront.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.UpdateAccountRequest;

@FeignClient(
        name = "accounts-client",
        url = "${gateway.base-url}",
        configuration = AccountsClientConfig.class
)
public interface AccountsClient {

    @GetMapping("/api/accounts")
    AccountResponse getAccount();

    @PutMapping("/api/accounts")
    AccountResponse updateAccount(@RequestBody UpdateAccountRequest request);
}
