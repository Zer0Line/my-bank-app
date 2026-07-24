package ru.yandex.practicum.transfer.dto;

public record TransferActionRequest(
        int value,
        String login
) {
}
