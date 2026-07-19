package com.carebridge.backend.security.repository;

import com.carebridge.backend.security.entity.UserIdentity;
import com.carebridge.backend.security.federation.FederatedProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    Optional<UserIdentity> findByProviderAndProviderSubject(FederatedProvider provider, String providerSubject);

    Optional<UserIdentity> findByUserIdAndProvider(UUID userId, FederatedProvider provider);

    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:identityKey, 0))", nativeQuery = true)
    void lockProviderSubject(@Param("identityKey") String identityKey);

    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:userProviderKey, 0))", nativeQuery = true)
    void lockUserProvider(@Param("userProviderKey") String userProviderKey);
}
