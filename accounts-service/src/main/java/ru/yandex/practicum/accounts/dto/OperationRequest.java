package ru.yandex.practicum.accounts.dto;

public record OperationRequest(
        String login,
        String type,
        String message,
        Integer amount
) {
}
