package ru.yandex.practicum.mybankfront.dto;

import java.math.BigDecimal;

public record CashActionRequest(
        BigDecimal value,
        CashAction action
) {
}
