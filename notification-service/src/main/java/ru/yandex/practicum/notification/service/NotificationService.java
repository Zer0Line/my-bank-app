package ru.yandex.practicum.notification.service;

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

    private final OperationRepository operationRepository;

    public void saveOperation(OperationRequest request) {
        OperationEntity entity = new OperationEntity();
        entity.setLogin(request.login());
        entity.setType(request.type());
        entity.setMessage(request.message());
        entity.setAmount(request.amount());
        entity.setCreatedAt(LocalDateTime.now());

        operationRepository.save(entity);
        log.info("Operation saved for login='{}', type='{}', amount={}",
                request.login(), request.type(), request.amount());
    }

    public void saveOperations(List<OperationRequest> requests) {
        List<OperationEntity> entities = requests.stream().map(request -> {
            OperationEntity entity = new OperationEntity();
            entity.setLogin(request.login());
            entity.setType(request.type());
            entity.setMessage(request.message());
            entity.setAmount(request.amount());
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        }).toList();

        operationRepository.saveAll(entities);
        log.info("{} operations saved", entities.size());
    }
}
