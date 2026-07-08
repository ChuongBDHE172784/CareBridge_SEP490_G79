package com.carebridge.backend.identity.repository;

import com.carebridge.backend.identity.entity.TokenBlacklist;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, UUID> {

    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
}
