package com.carebridge.backend.identity.repository;

import com.carebridge.backend.identity.entity.UserSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdAndRevokedAtIsNullOrderByLastActivityAtDesc(UUID userId);

    Page<UserSession> findByUserIdAndRevokedAtIsNull(UUID userId, Pageable pageable);

    @Modifying
    @Query("UPDATE UserSession us SET us.revokedAt = :now, us.status = 'REVOKED' WHERE us.userId = :userId AND us.sessionId != :excludeSessionId AND us.revokedAt IS NULL")
    int revokeAllExceptSession(@Param("userId") UUID userId, @Param("excludeSessionId") UUID excludeSessionId, @Param("now") Instant now);

    Optional<UserSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

    @Modifying
    @Query("UPDATE UserSession us SET us.lastActivityAt = :now WHERE us.refreshTokenHash = :token")
    void updateActivity(@Param("now") Instant now, @Param("ip") String ip, @Param("token") String token);

    @Modifying
    @Query("UPDATE UserSession us SET us.revokedAt = :now, us.status = 'REVOKED' WHERE us.sessionId = :sessionId AND us.userId = :userId")
    int revokeSession(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession us SET us.status = us.status WHERE us.userId = :userId AND us.sessionId != :newSessionId")
    int clearCurrentSessions(@Param("userId") UUID userId, @Param("newSessionId") UUID newSessionId);

    @Modifying
    @Query("UPDATE UserSession us SET us.refreshTokenHash = :newHash, us.expiresAt = :newExpiry, us.lastActivityAt = :now WHERE us.sessionId = :sessionId")
    int updateSessionForRotation(@Param("sessionId") UUID sessionId, @Param("newHash") String newHash, @Param("newExpiry") Instant newExpiry, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession us SET us.revokedAt=:now, us.status='REVOKED' WHERE us.refreshTokenHash=:hash AND us.userId=:userId AND us.revokedAt IS NULL")
    int revokeByHash(@Param("hash") String hash, @Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession us SET us.revokedAt=:now, us.status='REVOKED' WHERE us.userId=:userId AND us.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    default List<UserSession> findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(UUID userId) { return findByUserIdAndRevokedAtIsNullOrderByLastActivityAtDesc(userId); }
    default Page<UserSession> findByUserIdAndRevokedFalse(UUID userId, Pageable pageable) { return findByUserIdAndRevokedAtIsNull(userId, pageable); }
    default Optional<UserSession> findByRefreshTokenHashAndRevokedFalse(String hash) { return findByRefreshTokenHashAndRevokedAtIsNull(hash); }
}
