package ru.yandex.practicum.accounts.dto;

import java.util.List;

public record AccountResponse(
        String login,
        String name,
        String birthdate,
        int sum,
        List<AccountDto> accounts
) {
}
