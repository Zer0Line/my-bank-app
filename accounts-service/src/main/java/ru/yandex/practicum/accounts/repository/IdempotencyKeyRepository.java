package ru.yandex.practicum.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.accounts.entity.IdempotencyKeyEntity;
import ru.yandex.practicum.accounts.entity.IdempotencyStatus;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {

    Optional<IdempotencyKeyEntity> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = """
            INSERT INTO accounts.idempotency_keys (idempotency_key, request_digest, status, created_at)
            VALUES (:idempotencyKey, :requestDigest, 'IN_PROGRESS', :createdAt)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int claim(@Param("idempotencyKey") String idempotencyKey,
              @Param("requestDigest") String requestDigest,
              @Param("createdAt") Instant createdAt);

    @Modifying
    @Query("""
            UPDATE IdempotencyKeyEntity e
            SET e.status = :status, e.responseJson = :responseJson
            WHERE e.idempotencyKey = :idempotencyKey
            """)
    int complete(@Param("idempotencyKey") String idempotencyKey,
                 @Param("status") IdempotencyStatus status,
                 @Param("responseJson") String responseJson);
}