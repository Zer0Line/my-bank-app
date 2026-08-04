package ru.yandex.practicum.transfer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.transfer.dto.AccountResponse;
import ru.yandex.practicum.transfer.dto.TransferRequest;

@FeignClient(
        name = "accounts-service",
        url = "http://accounts-service:9092",
        configuration = AccountsServiceFeignConfig.class
)
public interface AccountsServiceClient {

    @PostMapping("/api/accounts/transfer")
    AccountResponse transfer(@RequestBody TransferRequest request);
}
