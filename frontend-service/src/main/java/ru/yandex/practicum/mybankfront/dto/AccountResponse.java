package ru.yandex.practicum.mybankfront.dto;

import java.util.List;

public record AccountResponse(
        String login,
        String name,
        String birthdate,
        int sum,
        List<AccountDto> accounts
) {
}
