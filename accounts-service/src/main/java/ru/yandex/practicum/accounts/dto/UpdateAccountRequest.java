package ru.yandex.practicum.accounts.dto;

public record UpdateAccountRequest(
        String name,
        String birthdate
) {
}
