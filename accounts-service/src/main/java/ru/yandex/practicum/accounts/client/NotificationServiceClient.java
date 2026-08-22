package ru.yandex.practicum.accounts.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.accounts.dto.OperationRequest;

import java.util.List;

@FeignClient(
        name = "notification-service",
        url = "${clients.notification-service.url}",
        configuration = NotificationServiceFeignConfig.class
)
public interface NotificationServiceClient {

    @PostMapping("/api/notifications")
    void saveOperation(@RequestBody OperationRequest request);

    @PostMapping("/api/notifications/batch")
    void saveOperations(@RequestBody List<OperationRequest> requests);
}
