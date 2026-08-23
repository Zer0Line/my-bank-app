package ru.yandex.practicum.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.notification.dto.OperationRequest;
import ru.yandex.practicum.notification.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferRequestListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "transfer-request")
    public void listen(OperationRequest request) {
        log.info("Received operation from topic 'transfer-request': {}", request);
        notificationService.saveOperation(request);
    }
}
