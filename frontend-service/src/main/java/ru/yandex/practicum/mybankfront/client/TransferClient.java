package ru.yandex.practicum.mybankfront.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.mybankfront.dto.AccountResponse;
import ru.yandex.practicum.mybankfront.dto.TransferRequest;

@FeignClient(
        name = "transfer-client",
        url = "${gateway.base-url}",
        configuration = AccountsClientConfig.class
)
public interface TransferClient {

    @PostMapping("/api/transfers")
    AccountResponse transfer(@RequestBody TransferRequest request);
}
