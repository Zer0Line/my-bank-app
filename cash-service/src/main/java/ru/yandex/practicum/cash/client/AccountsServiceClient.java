package ru.yandex.practicum.cash.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.cash.dto.AccountResponse;
import ru.yandex.practicum.cash.dto.UpdateAmountRequest;

@FeignClient(
        name = "accounts-service",
        url = "http://accounts-service:9092",
        configuration = AccountsServiceFeignConfig.class
)
public interface AccountsServiceClient {

    @PatchMapping("/api/accounts/amount")
    AccountResponse updateAmount(@RequestBody UpdateAmountRequest request);
}
