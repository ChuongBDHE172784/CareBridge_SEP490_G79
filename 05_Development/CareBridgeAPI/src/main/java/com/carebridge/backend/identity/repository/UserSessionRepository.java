package com.carebridge.backend.identity.repository;

import com.carebridge.backend.identity.entity.UserSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    List<UserSession> findByUserIdAndRevokedFalseOrderByLastActivityAtDesc(UUID userId);

    Optional<UserSession> findByRefreshTokenHashAndRevokedFalse(String refreshTokenHash);

    @Modifying
    @Query("UPDATE UserSession us SET us.lastActivityAt = :now, us.ipAddress = :ip, us.updatedAt = :now WHERE us.refreshTokenHash = :token")
    void updateActivity(@Param("now") Instant now, @Param("ip") String ip, @Param("token") String token);

    @Modifying
    @Query("UPDATE UserSession us SET us.revoked = true, us.status = 'revoked', us.updatedAt = :now WHERE us.sessionId = :sessionId AND us.userId = :userId")
    int revokeSession(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserSession us SET us.isCurrent = false WHERE us.userId = :userId AND us.isCurrent = true AND us.sessionId != :newSessionId")
    int clearCurrentSessions(@Param("userId") UUID userId, @Param("newSessionId") UUID newSessionId);
}
