package ru.yandex.practicum.cash.dto;

public record OperationRequest(
        String login,
        String type,
        String message,
        Integer amount
) {
}
