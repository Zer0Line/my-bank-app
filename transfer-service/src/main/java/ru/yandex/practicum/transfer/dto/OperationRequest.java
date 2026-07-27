package ru.yandex.practicum.transfer.dto;

import java.math.BigDecimal;

public record OperationRequest(
        String login,
        String type,
        String message,
        BigDecimal amount
) {
}
