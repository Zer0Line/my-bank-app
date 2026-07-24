package ru.yandex.practicum.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.notification.dto.OperationRequest;
import ru.yandex.practicum.notification.service.NotificationService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public void addOperation(@RequestBody OperationRequest request) {
        log.info("Incoming request: POST /api/notifications with body: {}", request);
        notificationService.saveOperation(request);
    }

    @PostMapping("/batch")
    public void addOperations(@RequestBody List<OperationRequest> requests) {
        log.info("Incoming request: POST /api/notifications/batch with {} operations", requests.size());
        notificationService.saveOperations(requests);
    }
}
