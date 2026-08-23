package ru.yandex.practicum.transfer.exception;

public class OperationNotificationException extends RuntimeException {

    public OperationNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
