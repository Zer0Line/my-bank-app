package ru.yandex.practicum.transfer.dto;

import java.math.BigDecimal;

public record TransferActionRequest(
        BigDecimal value,
        String login
) {
}
