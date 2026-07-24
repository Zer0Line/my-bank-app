package ru.yandex.practicum.cash.dto;

public record CashActionRequest(
        int value,
        CashAction action
) {
}
