package ru.yandex.practicum.accounts.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.accounts.dto.AccountResponse;
import ru.yandex.practicum.accounts.entity.IdempotencyKeyEntity;
import ru.yandex.practicum.accounts.entity.IdempotencyStatus;
import ru.yandex.practicum.accounts.repository.IdempotencyKeyRepository;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public AccountResponse execute(String idempotencyKey, Object request, Supplier<AccountResponse> operation) {
        String requestDigest = digestOf(request);

        IdempotencyKeyEntity existing = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return storedOutcome(existing, requestDigest);
        }

        int claimed = idempotencyKeyRepository.claim(idempotencyKey, requestDigest, Instant.now());
        if (claimed == 0) {
            IdempotencyKeyEntity concurrent = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT, "Idempotency key is being processed: " + idempotencyKey));
            return storedOutcome(concurrent, requestDigest);
        }

        try {
            AccountResponse response = operation.get();
            idempotencyKeyRepository.complete(idempotencyKey, IdempotencyStatus.COMPLETED, serialize(response));
            return response;
        } catch (RuntimeException e) {
            log.warn("Operation with idempotency key '{}' failed, key will be released on rollback", idempotencyKey, e);
            throw e;
        }
    }

    private AccountResponse storedOutcome(IdempotencyKeyEntity record, String requestDigest) {
        if (!record.getRequestDigest().equals(requestDigest)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency key reused with a different request: " + record.getIdempotencyKey());
        }
        if (record.getStatus() != IdempotencyStatus.COMPLETED || record.getResponseJson() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency key is being processed: " + record.getIdempotencyKey());
        }
        return deserialize(record.getResponseJson());
    }

    private String digestOf(Object request) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize request for idempotency check", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String serialize(AccountResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize stored idempotency response", e);
        }
    }

    private AccountResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, AccountResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot deserialize stored idempotency response", e);
        }
    }
}