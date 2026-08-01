package ru.yandex.practicum.accounts.dto;

import java.math.BigDecimal;

public record UpdateAmountRequest(
        String login,
        BigDecimal value,
        CashAction action
) {
}
