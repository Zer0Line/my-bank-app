package ru.yandex.practicum.accounts.dto;

public record UpdateAmountRequest(
        String login,
        int value,
        CashAction action
) {
}
