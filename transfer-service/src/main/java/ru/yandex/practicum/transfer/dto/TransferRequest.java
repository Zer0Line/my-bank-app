package ru.yandex.practicum.transfer.dto;

import java.math.BigDecimal;

public record TransferRequest(
        String senderLogin,
        String recipientLogin,
        BigDecimal amount
) {
}
