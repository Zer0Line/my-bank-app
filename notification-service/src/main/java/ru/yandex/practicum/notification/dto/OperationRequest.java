package ru.yandex.practicum.notification.dto;

public record OperationRequest(
        String login,
        String type,
        String message,
        Integer amount
) {
}
