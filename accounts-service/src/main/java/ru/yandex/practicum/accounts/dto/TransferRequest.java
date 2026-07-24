package ru.yandex.practicum.accounts.dto;

public record TransferRequest(
        String senderLogin,
        String recipientLogin,
        int amount
) {
}
