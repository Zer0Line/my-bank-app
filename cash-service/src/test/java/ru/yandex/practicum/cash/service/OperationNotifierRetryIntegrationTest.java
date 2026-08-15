package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import ru.yandex.practicum.cash.client.NotificationServiceClient;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.OperationRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = OperationNotifierRetryIntegrationTest.RetryTestConfig.class,
        properties = {
                "resilience4j.retry.instances.notification-service.max-attempts=3",
                "resilience4j.retry.instances.notification-service.wait-duration=10ms",
                "resilience4j.circuitbreaker.instances.notification-service.minimum-number-of-calls=100"
        }
)
class OperationNotifierRetryIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
            "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
            "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
            "org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration",
            "org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerAutoConfiguration",
            "org.springframework.cloud.openfeign.FeignAutoConfiguration"
    })
    static class RetryTestConfig {

        @Bean
        OperationNotifierService operationNotifierService(NotificationServiceClient notificationClient) {
            return new OperationNotifierService(notificationClient);
        }
    }

    @Autowired
    private OperationNotifierService operationNotifierService;

    @MockBean
    private NotificationServiceClient notificationClient;

    @Test
    void retry_transientFailure_retriesThenSucceeds() {
        doThrow(new RuntimeException("connection refused"))
                .doThrow(new RuntimeException("connection refused"))
                .doNothing()
                .when(notificationClient).saveOperation(any(OperationRequest.class));

        assertDoesNotThrow(() ->
                operationNotifierService.notifyOperationStarted("testuser", CashAction.PUT, BigDecimal.valueOf(500)));

        verify(notificationClient, times(3)).saveOperation(any(OperationRequest.class));
    }

    @Test
    void retry_persistentFailure_exhaustsAttemptsThenPropagates() {
        doThrow(new RuntimeException("connection refused"))
                .when(notificationClient).saveOperation(any(OperationRequest.class));

        assertThrows(RuntimeException.class, () ->
                operationNotifierService.notifyOperationStarted("testuser", CashAction.PUT, BigDecimal.valueOf(500)));

        verify(notificationClient, times(3)).saveOperation(any(OperationRequest.class));
    }
}