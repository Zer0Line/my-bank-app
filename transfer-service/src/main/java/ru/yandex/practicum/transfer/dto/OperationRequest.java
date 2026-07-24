package ru.yandex.practicum.transfer.dto;

public record OperationRequest(
        String login,
        String type,
        String message,
        Integer amount
) {
}
