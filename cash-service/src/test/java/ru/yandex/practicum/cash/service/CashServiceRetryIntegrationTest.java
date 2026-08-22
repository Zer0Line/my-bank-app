package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import ru.yandex.practicum.cash.client.AccountsServiceClient;
import ru.yandex.practicum.cash.dto.AccountResponse;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.UpdateAmountRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = CashServiceRetryIntegrationTest.RetryTestConfig.class,
        properties = {
                "resilience4j.retry.instances.accounts-service.max-attempts=3",
                "resilience4j.retry.instances.accounts-service.wait-duration=10ms",
                "resilience4j.circuitbreaker.instances.accounts-service.minimum-number-of-calls=100"
        }
)
class CashServiceRetryIntegrationTest {

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
        CashService cashService(AccountsServiceClient accountsServiceClient, OperationNotifierService operationNotifierService) {
            return new CashService(accountsServiceClient, operationNotifierService);
        }
    }

    @Autowired
    private CashService cashService;

    @MockBean
    private AccountsServiceClient accountsServiceClient;

    @MockBean
    private OperationNotifierService operationNotifierService;

    @Test
    void retry_transientFailure_retriesThenSucceeds() {
        AccountResponse response = new AccountResponse(
                "testuser", "Test User", "2000-01-01", BigDecimal.valueOf(1500), List.of());
        when(accountsServiceClient.updateAmount(any(UpdateAmountRequest.class), anyString()))
                .thenThrow(new RuntimeException("connection refused"))
                .thenThrow(new RuntimeException("connection refused"))
                .thenReturn(response);

        AccountResponse result = cashService.processCashAction(
                "testuser", BigDecimal.valueOf(500), CashAction.PUT, "test-key");

        assertEquals("testuser", result.login());
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(result.sum()));
        verify(accountsServiceClient, times(3)).updateAmount(any(UpdateAmountRequest.class), anyString());
    }

    @Test
    void retry_persistentFailure_afterExhaustedAttemptsInvokesFallback() {
        when(accountsServiceClient.updateAmount(any(UpdateAmountRequest.class), anyString()))
                .thenThrow(new RuntimeException("connection refused"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                cashService.processCashAction("testuser", BigDecimal.valueOf(500), CashAction.PUT, "test-key"));

        assertEquals("Cash operation failed: accounts service is unavailable", ex.getMessage());
        verify(accountsServiceClient, times(3)).updateAmount(any(UpdateAmountRequest.class), anyString());
    }
}