package ru.yandex.practicum.accounts.dto;

import java.math.BigDecimal;

public record TransferRequest(
        String senderLogin,
        String recipientLogin,
        BigDecimal amount
) {
}
