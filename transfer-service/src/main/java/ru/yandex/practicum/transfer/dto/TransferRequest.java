package ru.yandex.practicum.transfer.dto;

public record TransferRequest(
        String senderLogin,
        String recipientLogin,
        int amount
) {
}
