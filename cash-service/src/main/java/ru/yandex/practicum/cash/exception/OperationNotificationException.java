package ru.yandex.practicum.cash.exception;

public class OperationNotificationException extends RuntimeException {

    public OperationNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
