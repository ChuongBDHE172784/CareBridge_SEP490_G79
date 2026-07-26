package com.carebridge.backend.community.repository;

import com.carebridge.backend.community.entity.CommunityProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityProfileRepository extends JpaRepository<CommunityProfile, UUID> {

    @Query("""
            select (count(p) > 0) from CommunityProfile p
            where p.communityProfileId = :userId and p.displayName is not null
            """)
    boolean existsByUserId(@Param("userId") UUID userId);

    @Query("""
            select p from CommunityProfile p
            where p.communityProfileId = :userId and p.displayName is not null
            """)
    Optional<CommunityProfile> findByUserId(@Param("userId") UUID userId);

    @Query("""
            select p from CommunityProfile p
            where p.communityProfileId in :userIds and p.displayName is not null
            """)
    List<CommunityProfile> findAllByUserIdIn(@Param("userIds") Collection<UUID> userIds);

    @Query("select p from CommunityProfile p where p.communityProfileId = :userId")
    Optional<CommunityProfile> findAccountByUserId(@Param("userId") UUID userId);
}
