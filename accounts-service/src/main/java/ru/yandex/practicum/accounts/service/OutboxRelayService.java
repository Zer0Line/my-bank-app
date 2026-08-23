package ru.yandex.practicum.accounts.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.dto.OperationRequest;
import ru.yandex.practicum.accounts.entity.OutboxEventEntity;
import ru.yandex.practicum.accounts.entity.OutboxStatus;
import ru.yandex.practicum.accounts.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, OperationRequest> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "account-operations";

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:5000}")
    @Transactional
    public void publishPending() {
        List<OutboxEventEntity> pending = outboxEventRepository.findPending(
                OutboxStatus.PENDING, Pageable.ofSize(batchSize));

        for (OutboxEventEntity event : pending) {
            try {
                List<OperationRequest> operations = objectMapper.readValue(
                        event.getPayload(), new TypeReference<>() {
                        });
                for (OperationRequest operation : operations) {
                    kafkaTemplate.send(TOPIC, operation);
                }

                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                event.setErrorMessage(null);
                outboxEventRepository.save(event);
                log.info("Published outbox event id={} with {} operation(s) to topic '{}'",
                        event.getId(), operations.size(), TOPIC);
            } catch (Exception e) {
                event.setAttempts(event.getAttempts() + 1);
                event.setErrorMessage(e.getMessage());
                outboxEventRepository.save(event);
                log.warn("Failed to publish outbox event id={}, attempt {}: {}",
                        event.getId(), event.getAttempts(), e.getMessage());
            }
        }
    }
}