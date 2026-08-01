package ru.yandex.practicum.cash.dto;

import java.math.BigDecimal;

public record UpdateAmountRequest(
        String login,
        BigDecimal value,
        CashAction action
) {
}
