package ru.yandex.practicum.cash.dto;

public record UpdateAmountRequest(
        String login,
        int value,
        CashAction action
) {
}
