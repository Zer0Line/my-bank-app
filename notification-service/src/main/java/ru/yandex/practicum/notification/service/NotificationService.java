package ru.yandex.practicum.notification.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.notification.dto.OperationRequest;
import ru.yandex.practicum.notification.entity.OperationEntity;
import ru.yandex.practicum.notification.repository.OperationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String SAVE_FAILED_METRIC = "notification.save.failed";

    private final OperationRepository operationRepository;
    private final MeterRegistry meterRegistry;

    public void saveOperation(OperationRequest request) {
        OperationEntity entity = buildEntity(request);

        try {
            operationRepository.save(entity);
        } catch (RuntimeException e) {
            recordSaveFailure(request.login());
            throw e;
        }
    }

    public void saveOperations(List<OperationRequest> requests) {
        List<OperationEntity> entities = requests.stream().map(this::buildEntity).toList();

        try {
            operationRepository.saveAll(entities);
            log.info("{} operations saved", entities.size());
        } catch (RuntimeException e) {
            requests.forEach(request -> recordSaveFailure(request.login()));
            throw e;
        }
    }

    private OperationEntity buildEntity(OperationRequest request) {
        OperationEntity entity = new OperationEntity();
        entity.setLogin(request.login());
        entity.setType(request.type());
        entity.setMessage(request.message());
        entity.setAmount(request.amount());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private void recordSaveFailure(String login) {
        meterRegistry.counter(SAVE_FAILED_METRIC, "login", login).increment();
    }
}
