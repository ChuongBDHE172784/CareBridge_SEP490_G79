package com.carebridge.backend.security.repository;

import com.carebridge.backend.security.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash, Instant now);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :usedAt WHERE t.id = :id")
    void markAsUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.attemptCount = t.attemptCount + 1 WHERE t.tokenHash = :tokenHash")
    void incrementAttemptCount(@Param("tokenHash") String tokenHash);
}
