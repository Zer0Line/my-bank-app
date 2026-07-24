package ru.yandex.practicum.mybankfront.dto;

public record TransferRequest(
        int value,
        String login
) {
}
