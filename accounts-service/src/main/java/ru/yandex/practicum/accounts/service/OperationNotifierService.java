package ru.yandex.practicum.accounts.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.accounts.dto.CashAction;
import ru.yandex.practicum.accounts.dto.OperationRequest;
import ru.yandex.practicum.accounts.entity.OutboxEventEntity;
import ru.yandex.practicum.accounts.entity.OutboxStatus;
import ru.yandex.practicum.accounts.repository.OutboxEventRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationNotifierService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void notifyAccountUpdate(String login) {
        saveOutboxEvent(List.of(
                new OperationRequest(login, "ACCOUNT_UPDATE", "Account details updated", null)));
    }

    public void notifyCashOperation(String login, CashAction action, BigDecimal value) {
        saveOutboxEvent(List.of(
                new OperationRequest(login, "CASH_" + action.name(), "Cash operation", value)));
    }

    public void notifyTransfer(String senderLogin, String recipientLogin, BigDecimal value) {
        saveOutboxEvent(List.of(
                new OperationRequest(senderLogin, "TRANSFER_SENT", "Transfer to " + recipientLogin, value),
                new OperationRequest(recipientLogin, "TRANSFER_RECEIVED", "Transfer from " + senderLogin, value)
        ));
    }

    private void saveOutboxEvent(List<OperationRequest> operations) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(operations);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize outbox event payload", e);
        }

        OutboxEventEntity event = new OutboxEventEntity();
        event.setPayload(payload);
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setCreatedAt(Instant.now());

        outboxEventRepository.save(event);
        log.info("Outbox event saved for {} operation(s), payload={}", operations.size(), payload);
    }
}