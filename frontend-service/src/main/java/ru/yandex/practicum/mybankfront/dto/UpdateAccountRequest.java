package ru.yandex.practicum.mybankfront.dto;

public record UpdateAccountRequest(
        String name,
        String birthdate
) {
}
