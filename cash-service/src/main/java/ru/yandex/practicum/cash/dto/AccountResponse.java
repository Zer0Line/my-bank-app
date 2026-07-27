package ru.yandex.practicum.cash.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccountResponse(
        String login,
        String name,
        String birthdate,
        BigDecimal sum,
        List<AccountDto> accounts
) {
}
