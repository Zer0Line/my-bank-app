package ru.yandex.practicum.cash.dto;

import java.math.BigDecimal;

public record CashActionRequest(
        BigDecimal value,
        CashAction action
) {
}
