package ru.yandex.practicum.mybankfront.dto;

public record CashActionRequest(
        int value,
        CashAction action
) {
}
