package com.carebridge.backend.security.repository;

import com.carebridge.backend.identity.entity.UserSession;
import com.carebridge.backend.identity.repository.UserSessionRepository;
import com.carebridge.backend.security.entity.RefreshToken;
import com.carebridge.backend.security.util.TokenUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Compatibility facade backed exclusively by canonical auth_sessions. */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {
    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public RefreshToken save(RefreshToken token) {
        if (token.isRevoked() && token.getTokenHash() != null && token.getUser() != null) {
            sessionRepository.revokeByHash(token.getTokenHash(), token.getUser().getId(), Instant.now());
        }
        return token;
    }

    public Optional<RefreshToken> findByTokenAndRevokedFalse(String rawToken) {
        return findActive(TokenUtils.hashSha256(rawToken), rawToken);
    }

    public Optional<RefreshToken> findByTokenAndRevokedFalseForUpdate(String rawToken) {
        return findByTokenAndRevokedFalse(rawToken);
    }

    public List<RefreshToken> findByUser_IdAndRevokedFalse(UUID userId) {
        return sessionRepository.findByUserIdAndRevokedAtIsNullOrderByLastActivityAtDesc(userId).stream()
                .map(session -> fromSession(session, null)).toList();
    }

    public int revokeByTokenHashAndUserId(String tokenHash, UUID userId) {
        return sessionRepository.revokeByHash(tokenHash, userId, Instant.now());
    }

    public Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash, UUID userId) {
        return sessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(tokenHash)
                .filter(session -> session.getUserId().equals(userId)).map(session -> fromSession(session, null));
    }

    public int revokeAllByUserId(UUID userId) {
        return sessionRepository.revokeAllByUserId(userId, Instant.now());
    }

    public void deleteAll() {
        sessionRepository.deleteAll();
    }

    private Optional<RefreshToken> findActive(String hash, String rawToken) {
        return sessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(hash)
                .map(session -> fromSession(session, rawToken));
    }

    private RefreshToken fromSession(UserSession session, String rawToken) {
        return RefreshToken.builder()
                .id((long) session.getSessionId().hashCode())
                .user(userRepository.findById(session.getUserId()).orElse(null))
                .token(rawToken).tokenHash(session.getRefreshTokenHash())
                .expiresAt(session.getExpiresAt()).revoked(session.isRevoked())
                .createdAt(session.getCreatedAt()).build();
    }
}
