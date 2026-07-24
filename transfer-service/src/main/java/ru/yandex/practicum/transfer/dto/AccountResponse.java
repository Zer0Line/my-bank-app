package ru.yandex.practicum.transfer.dto;

import java.util.List;

public record AccountResponse(
        String login,
        String name,
        String birthdate,
        int sum,
        List<AccountDto> accounts
) {
}
