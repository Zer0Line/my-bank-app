package ru.yandex.practicum.accounts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.entity.IdempotencyKeyEntity;
import ru.yandex.practicum.accounts.entity.IdempotencyStatus;
import ru.yandex.practicum.accounts.repository.IdempotencyKeyRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private IdempotencyService idempotencyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(idempotencyKeyRepository, objectMapper);
    }

    private AccountResponse response() {
        return new AccountResponse("testuser", "Test User", "2000-01-01", BigDecimal.valueOf(1300), java.util.List.of());
    }

    @Test
    void firstExecution_runsOperationAndCompletesKey() {
        when(idempotencyKeyRepository.findByIdempotencyKey("amount-key")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.claim(eq("amount-key"), anyString(), any(Instant.class))).thenReturn(1);

        AccountResponse result = idempotencyService.execute(
                "amount-key", new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), null), this::response);

        assertEquals(BigDecimal.valueOf(1300), result.sum());
        verify(idempotencyKeyRepository).complete(eq("amount-key"), eq(IdempotencyStatus.COMPLETED), anyString());
    }

    @Test
    void replay_returnsStoredResponseWithoutRunningOperation() throws Exception {
        AccountResponse stored = response();
        IdempotencyKeyEntity existing = new IdempotencyKeyEntity();
        existing.setRequestDigest(idempotencyServiceDigestOf(new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), null)));
        existing.setStatus(IdempotencyStatus.COMPLETED);
        existing.setResponseJson(objectMapper.writeValueAsString(stored));

        when(idempotencyKeyRepository.findByIdempotencyKey("amount-key")).thenReturn(Optional.of(existing));

        AtomicInteger runs = new AtomicInteger();
        AccountResponse result = idempotencyService.execute(
                "amount-key", new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), null),
                () -> {
                    runs.incrementAndGet();
                    return response();
                });

        assertEquals(0, runs.get());
        assertEquals(stored, result);
    }

    @Test
    void replay_withDifferentRequest_returnsConflict() throws Exception {
        IdempotencyKeyEntity existing = new IdempotencyKeyEntity();
        existing.setRequestDigest(idempotencyServiceDigestOf(new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), null)));
        existing.setStatus(IdempotencyStatus.COMPLETED);
        existing.setResponseJson(objectMapper.writeValueAsString(response()));

        when(idempotencyKeyRepository.findByIdempotencyKey("amount-key")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                idempotencyService.execute(
                        "amount-key", new UpdateAmountRequest("other", BigDecimal.valueOf(999), null), this::response));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void concurrentDuplicate_returnsWinnerResponse() throws Exception {
        AccountResponse winner = response();
        IdempotencyKeyEntity existing = new IdempotencyKeyEntity();
        existing.setRequestDigest(idempotencyServiceDigestOf(new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), null)));
        existing.setStatus(IdempotencyStatus.COMPLETED);
        existing.setResponseJson(objectMapper.writeValueAsString(winner));

        when(idempotencyKeyRepository.findByIdempotencyKey("amount-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(idempotencyKeyRepository.claim(eq("amount-key"), anyString(), any(Instant.class))).thenReturn(0);

        AtomicInteger runs = new AtomicInteger();
        AccountResponse result = idempotencyService.execute(
                "amount-key", new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), null),
                () -> {
                    runs.incrementAndGet();
                    return response();
                });

        assertEquals(0, runs.get());
        assertEquals(winner, result);
        verify(idempotencyKeyRepository, never()).complete(anyString(), any(IdempotencyStatus.class), anyString());
    }

    @Test
    void operationFailure_propagatesExceptionAndDoesNotComplete() {
        when(idempotencyKeyRepository.findByIdempotencyKey("amount-key")).thenReturn(Optional.empty());
        when(idempotencyKeyRepository.claim(eq("amount-key"), anyString(), any(Instant.class))).thenReturn(1);

        IllegalStateException expected = new IllegalStateException("operation failed");
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                idempotencyService.execute("amount-key",
                        new UpdateAmountRequest("testuser", BigDecimal.valueOf(300), null),
                        () -> {
                            throw expected;
                        }));

        assertEquals(expected, thrown);
        verify(idempotencyKeyRepository, never()).complete(anyString(), any(IdempotencyStatus.class), anyString());
    }

    private String idempotencyServiceDigestOf(Object request) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(objectMapper.writeValueAsBytes(request)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}